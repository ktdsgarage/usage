package com.kt.usage.sync.repository;

import com.kt.usage.sync.model.UsageView;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UsageViewRepository extends MongoRepository<UsageView, String> {
}
