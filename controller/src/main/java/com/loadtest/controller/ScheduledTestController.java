// ========== ScheduledTestController.java ==========
package com.loadtest.controller;

import com.loadtest.model.ScheduledTest;
import com.loadtest.service.SchedulerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/schedules")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ScheduledTestController {
    
    private final SchedulerService schedulerService;
    
    @PostMapping
    public ResponseEntity<ScheduledTest> createSchedule(
            @RequestBody ScheduledTest scheduledTest,
            Authentication authentication) {
        
        String userId = (String) authentication.getPrincipal();
        scheduledTest.setUserId(userId);
        
        ScheduledTest created = schedulerService.createScheduledTest(scheduledTest);
        return ResponseEntity.ok(created);
    }
    
    @GetMapping
    public ResponseEntity<List<ScheduledTest>> getSchedules(Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        List<ScheduledTest> schedules = schedulerService.getUserScheduledTests(userId);
        return ResponseEntity.ok(schedules);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Void> updateSchedule(
            @PathVariable String id,
            @RequestBody ScheduledTest updates) {
        
        schedulerService.updateScheduledTest(id, updates);
        return ResponseEntity.ok().build();
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSchedule(@PathVariable String id) {
        schedulerService.deleteScheduledTest(id);
        return ResponseEntity.ok().build();
    }
}