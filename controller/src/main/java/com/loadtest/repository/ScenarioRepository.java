package com.loadtest.repository;

import com.loadtest.model.LoadTestScenario;
import com.loadtest.model.ScenarioStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ScenarioRepository extends MongoRepository<LoadTestScenario, String> {
    
    List<LoadTestScenario> findByUserId(String userId);
    
    List<LoadTestScenario> findByStatus(ScenarioStatus status);
    
    @Query("{ 'userId': ?0, 'status': ?1 }")
    List<LoadTestScenario> findByUserIdAndStatus(String userId, ScenarioStatus status);

    // Keeping organizationId methods if future use requires them, 
    // but noting LoadTestScenario currently uses userId.
    // Ideally we should align this. Assuming userId for now based on models.
}