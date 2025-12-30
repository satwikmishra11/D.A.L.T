package com.loadtest.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loadtest.model.*;
import com.loadtest.repository.MetricRepository;
import com.loadtest.repository.ScenarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExportService {
    
    private final ScenarioRepository scenarioRepository;
    private final MetricRepository metricRepository;
    private final MetricsAggregationService metricsService;
    private final ObjectMapper objectMapper;
    
    private static final DateTimeFormatter FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());
    
    public byte[] exportToJson(String scenarioId) throws IOException {
        LoadTestScenario scenario = scenarioRepository.findById(scenarioId)
            .orElseThrow(() -> new RuntimeException("Scenario not found"));
        
        ScenarioStats stats = metricsService.getAggregatedStats(scenarioId);
        List<Metric> metrics = metricRepository.findByScenarioId(scenarioId);
        
        Map<String, Object> export = new LinkedHashMap<>();
        export.put("scenario", scenario);
        export.put("stats", stats);
        export.put("metrics", metrics);
        export.put("exportedAt", Instant.now());
        
        return objectMapper.writerWithDefaultPrettyPrinter()
            .writeValueAsBytes(export);
    }
    
    public byte[] exportToCsv(String scenarioId) throws IOException {
        List<Metric> metrics = metricRepository.findByScenarioId(scenarioId);
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out));
        
        CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.builder()
            .setHeader(
                "Timestamp",
                "Worker ID",
                "Latency (ms)",
                "Status Code",
                "Success",
                "Error Message",
                "Request Count"
            )
            .build());
        
        for (Metric metric : metrics) {
            csvPrinter.printRecord(
                FORMATTER.format(metric.getTimestamp()),
                metric.getWorkerId(),
                metric.getLatencyMs(),
                metric.getStatusCode(),
                metric.isSuccess(),
                metric.getErrorMessage() != null ? metric.getErrorMessage() : "",
                metric.getRequestCount()
            );
        }
        
        csvPrinter.flush();
        writer.flush();
        
        return out.toByteArray();
    }
    
    public byte[] exportToHtml(String scenarioId) throws IOException {
        LoadTestScenario scenario = scenarioRepository.findById(scenarioId)
            .orElseThrow(() -> new RuntimeException("Scenario not found"));
        
        ScenarioStats stats = metricsService.getAggregatedStats(scenarioId);
        
        String html = generateHtmlReport(scenario, stats);
        return html.getBytes();
    }
    
    public TestReport generateReport(String scenarioId) {
        LoadTestScenario scenario = scenarioRepository.findById(scenarioId)
            .orElseThrow(() -> new RuntimeException("Scenario not found"));
        
        ScenarioStats stats = metricsService.getAggregatedStats(scenarioId);
        
        List<String> insights = generateInsights(stats);
        List<String> recommendations = generateRecommendations(stats);
        
        String summary = generateSummary(scenario, stats);
        
        return TestReport.builder()
            .scenarioId(scenarioId)
            .userId(scenario.getUserId())
            .title(scenario.getName() + " - Test Report")
            .summary(summary)
            .stats(stats)
            .insights(insights)
            .recommendations(recommendations)
            .generatedAt(Instant.now())
            .build();
    }
    
    private List<String> generateInsights(ScenarioStats stats) {
        List<String> insights = new ArrayList<>();
        
        // Success rate insight
        if (stats.getSuccessRate() > 99.5) {
            insights.add("Excellent success rate - system handling load well");
        } else if (stats.getSuccessRate() > 95) {
            insights.add("Good success rate with some errors observed");
        } else {
            insights.add("Low success rate indicates system struggling under load");
        }
        
        // Latency insight
        if (stats.getP99LatencyMs() < 100) {
            insights.add("Excellent latency performance across all percentiles");
        } else if (stats.getP99LatencyMs() < 500) {
            insights.add("Good latency but some outliers in P99");
        } else {
            insights.add("High P99 latency suggests performance bottlenecks");
        }
        
        // Latency consistency
        double p99ToP50Ratio = stats.getP99LatencyMs() / stats.getP50LatencyMs();
        if (p99ToP50Ratio > 5) {
            insights.add("Large variance between P50 and P99 indicates inconsistent performance");
        }
        
        // Error analysis
        if (!stats.getErrorTypeDistribution().isEmpty()) {
            String topError = stats.getErrorTypeDistribution().entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("Unknown");
            
            insights.add("Most common error type: " + topError);
        }
        
        // Throughput
        if (stats.getAvgRps() > 1000) {
            insights.add("High throughput achieved - " + String.format("%.0f", stats.getAvgRps()) + " RPS average");
        }
        
        return insights;
    }
    
    private List<String> generateRecommendations(ScenarioStats stats) {
        List<String> recommendations = new ArrayList<>();
        
        if (stats.getSuccessRate() < 99) {
            recommendations.add("Investigate error logs to identify root cause of failures");
            recommendations.add("Consider adding retry logic for transient failures");
        }
        
        if (stats.getP99LatencyMs() > 1000) {
            recommendations.add("Profile application to identify slow database queries");
            recommendations.add("Consider implementing caching for frequently accessed data");
            recommendations.add("Review connection pool settings and timeout configurations");
        }
        
        if (stats.getP99LatencyMs() / stats.getP50LatencyMs() > 5) {
            recommendations.add("Investigate outliers causing latency spikes");
            recommendations.add("Consider implementing circuit breakers for failing dependencies");
        }
        
        Map<Integer, Long> statusCodes = stats.getStatusCodeDistribution();
        if (statusCodes.getOrDefault(429, 0L) > 0) {
            recommendations.add("Rate limiting detected - consider implementing backoff strategy");
        }
        
        if (statusCodes.getOrDefault(500, 0L) > stats.getTotalRequests() * 0.01) {
            recommendations.add("High rate of 500 errors - check application logs and error monitoring");
        }
        
        if (statusCodes.getOrDefault(503, 0L) > 0) {
            recommendations.add("Service unavailable errors detected - check resource capacity");
        }
        
        if (recommendations.isEmpty()) {
            recommendations.add("Performance looks good! Consider testing with higher load");
        }
        
        return recommendations;
    }
    
    private String generateSummary(LoadTestScenario scenario, ScenarioStats stats) {
        return String.format("""
            Load test '%s' completed with the following results:
            
            • Total Requests: %,d
            • Success Rate: %.2f%%
            • Average Latency: %.0fms
            • P95 Latency: %.0fms
            • P99 Latency: %.0fms
            • Average Throughput: %.0f RPS
            • Peak Throughput: %.0f RPS
            
            Duration: %d seconds
            Workers: %d
            Target: %s
            """,
            scenario.getName(),
            stats.getTotalRequests(),
            stats.getSuccessRate(),
            stats.getAvgLatencyMs(),
            stats.getP95LatencyMs(),
            stats.getP99LatencyMs(),
            stats.getAvgRps(),
            stats.getPeakRps(),
            scenario.getDurationSeconds(),
            scenario.getNumWorkers(),
            scenario.getTargetUrl()
        );
    }
    
    private String generateHtmlReport(LoadTestScenario scenario, ScenarioStats stats) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <title>Load Test Report - %s</title>
                <style>
                    body {
                        font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                        max-width: 1200px;
                        margin: 0 auto;
                        padding: 20px;
                        background: #f5f5f5;
                    }
                    .header {
                        background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                        color: white;
                        padding: 30px;
                        border-radius: 10px;
                        margin-bottom: 30px;
                    }
                    .metric-card {
                        background: white;
                        padding: 20px;
                        border-radius: 8px;
                        box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                        margin-bottom: 20px;
                    }
                    .metric-grid {
                        display: grid;
                        grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
                        gap: 20px;
                        margin-bottom: 30px;
                    }
                    .metric-value {
                        font-size: 32px;
                        font-weight: bold;
                        color: #667eea;
                    }
                    .metric-label {
                        font-size: 14px;
                        color: #666;
                        margin-top: 5px;
                    }
                    .success { color: #10b981; }
                    .warning { color: #f59e0b; }
                    .error { color: #ef4444; }
                    table {
                        width: 100%%;
                        border-collapse: collapse;
                        background: white;
                    }
                    th, td {
                        padding: 12px;
                        text-align: left;
                        border-bottom: 1px solid #ddd;
                    }
                    th {
                        background: #f8f9fa;
                        font-weight: 600;
                    }
                </style>
            </head>
            <body>
                <div class="header">
                    <h1>Load Test Report</h1>
                    <h2>%s</h2>
                    <p>Generated: %s</p>
                </div>
                
                <div class="metric-grid">
                    <div class="metric-card">
                        <div class="metric-value">%,d</div>
                        <div class="metric-label">Total Requests</div>
                    </div>
                    <div class="metric-card">
                        <div class="metric-value %s">%.2f%%</div>
                        <div class="metric-label">Success Rate</div>
                    </div>
                    <div class="metric-card">
                        <div class="metric-value">%.0fms</div>
                        <div class="metric-label">Avg Latency</div>
                    </div>
                    <div class="metric-card">
                        <div class="metric-value">%.0f</div>
                        <div class="metric-label">Avg RPS</div>
                    </div>
                </div>
                
                <div class="metric-card">
                    <h3>Latency Distribution</h3>
                    <table>
                        <tr>
                            <th>Percentile</th>
                            <th>Latency</th>
                        </tr>
                        <tr>
                            <td>P50 (Median)</td>
                            <td>%.0fms</td>
                        </tr>
                        <tr>
                            <td>P75</td>
                            <td>%.0fms</td>
                        </tr>
                        <tr>
                            <td>P90</td>
                            <td>%.0fms</td>
                        </tr>
                        <tr>
                            <td>P95</td>
                            <td>%.0fms</td>
                        </tr>
                        <tr>
                            <td>P99</td>
                            <td>%.0fms</td>
                        </tr>
                    </table>
                </div>
                
                <div class="metric-card">
                    <h3>Test Configuration</h3>
                    <table>
                        <tr><td><strong>Target URL</strong></td><td>%s</td></tr>
                        <tr><td><strong>Method</strong></td><td>%s</td></tr>
                        <tr><td><strong>Duration</strong></td><td>%d seconds</td></tr>
                        <tr><td><strong>Workers</strong></td><td>%d</td></tr>
                        <tr><td><strong>Load Profile</strong></td><td>%s</td></tr>
                    </table>
                </div>
            </body>
            </html>
            """,
            scenario.getName(),
            scenario.getName(),
            FORMATTER.format(Instant.now()),
            stats.getTotalRequests(),
            stats.getSuccessRate() > 99 ? "success" : stats.getSuccessRate() > 95 ? "warning" : "error",
            stats.getSuccessRate(),
            stats.getAvgLatencyMs(),
            stats.getAvgRps(),
            stats.getP50LatencyMs(),
            stats.getP75LatencyMs(),
            stats.getP90LatencyMs(),
            stats.getP95LatencyMs(),
            stats.getP99LatencyMs(),
            scenario.getTargetUrl(),
            scenario.getMethod(),
            scenario.getDurationSeconds(),
            scenario.getNumWorkers(),
            scenario.getLoadProfile().getType()
        );
    }
}

// ========== Export Controller ==========
package com.loadtest.controller;

import com.loadtest.model.TestReport;
import com.loadtest.service.ExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/export")
@RequiredArgsConstructor
public class ExportController {
    
    private final ExportService exportService;
    
    @GetMapping("/{scenarioId}/json")
    public ResponseEntity<byte[]> exportJson(@PathVariable String scenarioId) {
        try {
            byte[] data = exportService.exportToJson(scenarioId);
            
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                    "attachment; filename=loadtest-" + scenarioId + ".json")
                .contentType(MediaType.APPLICATION_JSON)
                .body(data);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/{scenarioId}/csv")
    public ResponseEntity<byte[]> exportCsv(@PathVariable String scenarioId) {
        try {
            byte[] data = exportService.exportToCsv(scenarioId);
            
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                    "attachment; filename=loadtest-" + scenarioId + ".csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(data);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/{scenarioId}/html")
    public ResponseEntity<byte[]> exportHtml(@PathVariable String scenarioId) {
        try {
            byte[] data = exportService.exportToHtml(scenarioId);
            
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                    "attachment; filename=loadtest-" + scenarioId + ".html")
                .contentType(MediaType.TEXT_HTML)
                .body(data);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/{scenarioId}/report")
    public ResponseEntity<TestReport> getReport(@PathVariable String scenarioId) {
        TestReport report = exportService.generateReport(scenarioId);
        return ResponseEntity.ok(report);
    }
}