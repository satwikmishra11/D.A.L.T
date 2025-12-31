// ========== SchedulerService.java ==========
package com.loadtest.service;

import com.loadtest.model.*;
import com.loadtest.repository.ScheduledTestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SchedulerService {
    
    private final ScheduledTestRepository scheduledTestRepository;
    private final LoadTestOrchestrationService orchestrationService;
    
    @Scheduled(fixedRate = 60000) // Every minute
    public void checkScheduledTests() {
        log.debug("Checking for scheduled tests to run");
        
        Instant now = Instant.now();
        List<ScheduledTest> scheduledTests = scheduledTestRepository.findByEnabledTrue();
        
        for (ScheduledTest scheduledTest : scheduledTests) {
            try {
                if (shouldRunNow(scheduledTest, now)) {
                    runScheduledTest(scheduledTest);
                }
            } catch (Exception e) {
                log.error("Failed to run scheduled test: {}", scheduledTest.getId(), e);
            }
        }
    }
    
    private boolean shouldRunNow(ScheduledTest scheduledTest, Instant now) {
        // Check if next run time is now or in the past
        if (scheduledTest.getNextRunAt() != null && 
            scheduledTest.getNextRunAt().isBefore(now)) {
            return true;
        }
        
        // If no next run time set, check cron expression
        if (scheduledTest.getNextRunAt() == null) {
            return true;
        }
        
        return false;
    }
    
    private void runScheduledTest(ScheduledTest scheduledTest) {
        log.info("Running scheduled test: {}", scheduledTest.getName());
        
        try {
            // Start the scenario
            LoadTestScenario scenario = orchestrationService.startScenario(
                scheduledTest.getScenarioId()
            );
            
            // Update scheduled test
            scheduledTest.setLastRunAt(Instant.now());
            scheduledTest.setLastRunStatus("STARTED");
            scheduledTest.setLastRunScenarioId(scenario.getId());
            
            // Calculate next run time
            Instant nextRun = calculateNextRun(scheduledTest.getCronExpression());
            scheduledTest.setNextRunAt(nextRun);
            
            scheduledTestRepository.save(scheduledTest);
            
            log.info("Scheduled test started successfully. Next run: {}", nextRun);
            
        } catch (Exception e) {
            log.error("Failed to run scheduled test", e);
            scheduledTest.setLastRunStatus("FAILED");
            scheduledTestRepository.save(scheduledTest);
        }
    }
    
    private Instant calculateNextRun(String cronExpression) {
        CronExpression cron = CronExpression.parse(cronExpression);
        ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
        ZonedDateTime next = cron.next(now);
        return next != null ? next.toInstant() : null;
    }
    
    public ScheduledTest createScheduledTest(ScheduledTest scheduledTest) {
        // Calculate first run time
        Instant nextRun = calculateNextRun(scheduledTest.getCronExpression());
        scheduledTest.setNextRunAt(nextRun);
        scheduledTest.setCreatedAt(Instant.now());
        
        return scheduledTestRepository.save(scheduledTest);
    }
    
    public void updateScheduledTest(String id, ScheduledTest updates) {
        ScheduledTest existing = scheduledTestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Scheduled test not found"));
        
        if (updates.getName() != null) {
            existing.setName(updates.getName());
        }
        if (updates.getCronExpression() != null) {
            existing.setCronExpression(updates.getCronExpression());
            Instant nextRun = calculateNextRun(updates.getCronExpression());
            existing.setNextRunAt(nextRun);
        }
        if (updates.getEnabled() != null) {
            existing.setEnabled(updates.getEnabled());
        }
        
        scheduledTestRepository.save(existing);
    }
    
    public void deleteScheduledTest(String id) {
        scheduledTestRepository.deleteById(id);
    }
    
    public List<ScheduledTest> getUserScheduledTests(String userId) {
        return scheduledTestRepository.findByUserId(userId);
    }
}