// query/src/main/java/com/kt/usage/query/repository/UsageViewRepository.java
package com.kt.usage.query.repository;

import com.kt.usage.query.model.UsageView;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UsageViewRepository extends MongoRepository<UsageView, String> {
}
