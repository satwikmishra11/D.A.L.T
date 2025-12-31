package com.loadtest.repository;

import com.loadtest.model.*;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.List;

@Repository
public interface ScenarioRepository extends MongoRepository<LoadTestScenario, String> {
    List<LoadTestScenario> findByUserId(String userId);
    
    List<LoadTestScenario> findByStatus(ScenarioStatus status);
    
    @Query("{ 'userId': ?0, 'status': ?1 }")
    List<LoadTestScenario> findByUserIdAndStatus(String userId, ScenarioStatus status);
}

@Repository
public interface MetricRepository extends MongoRepository<Metric, String> {
    List<Metric> findByScenarioId(String scenarioId);
    
    @Query("{ 'scenarioId': ?0, 'timestamp': { $gte: ?1, $lte: ?2 } }")
    List<Metric> findByScenarioIdAndTimestampBetween(
        String scenarioId, 
        Instant start, 
        Instant end
    );
    
    long countByScenarioIdAndSuccess(String scenarioId, boolean success);
    
    @Query(value = "{ 'scenarioId': ?0 }", delete = true)
    void deleteByScenarioId(String scenarioId);
}