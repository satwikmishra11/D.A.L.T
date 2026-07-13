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
@CrossOrigin(origins = "*")
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
