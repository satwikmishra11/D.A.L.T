// ========== ScheduledTestController.java ==========
package com.loadtest.controller;

import com.loadtest.model.ScheduledTest;
import com.loadtest.service.SchedulerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/schedules")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Scheduled Tests", description = "Manage scheduled load tests and cron jobs")
public class ScheduledTestController {
    
    private final SchedulerService schedulerService;
    
    @PostMapping
    @Operation(summary = "Create Schedule", description = "Schedule a new load test execution")
    public ResponseEntity<ScheduledTest> createSchedule(
            @Valid @RequestBody ScheduledTest scheduledTest,
            Authentication authentication) {
        
        String userId = (String) authentication.getPrincipal();
        scheduledTest.setUserId(userId);
        
        ScheduledTest created = schedulerService.createScheduledTest(scheduledTest);
        return ResponseEntity.ok(created);
    }
    
    @GetMapping
    @Operation(summary = "List Schedules", description = "Get all scheduled tests for the authenticated user")
    public ResponseEntity<List<ScheduledTest>> getSchedules(Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        List<ScheduledTest> schedules = schedulerService.getUserScheduledTests(userId);
        return ResponseEntity.ok(schedules);
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "Update Schedule", description = "Modify an existing test schedule")
    public ResponseEntity<Void> updateSchedule(
            @PathVariable String id,
            @Valid @RequestBody ScheduledTest updates) {
        
        schedulerService.updateScheduledTest(id, updates);
        return ResponseEntity.ok().build();
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete Schedule", description = "Remove a test schedule")
    public ResponseEntity<Void> deleteSchedule(@PathVariable String id) {
        schedulerService.deleteScheduledTest(id);
        return ResponseEntity.ok().build();
    }
}