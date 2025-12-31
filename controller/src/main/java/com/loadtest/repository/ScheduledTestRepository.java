// ========== ScheduledTestRepository.java ==========
package com.loadtest.repository;

import com.loadtest.model.ScheduledTest;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScheduledTestRepository extends MongoRepository<ScheduledTest, String> {
    List<ScheduledTest> findByUserId(String userId);
    List<ScheduledTest> findByEnabledTrue();
}
