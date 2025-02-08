// query/src/main/java/com/kt/usage/query/service/UsageQueryService.java
package com.kt.usage.query.service;

import com.kt.usage.query.dto.UsageQueryResponse;
import com.kt.usage.query.exception.UsageNotFoundException;
import com.kt.usage.query.model.UsageView;
import com.kt.usage.query.repository.UsageViewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UsageQueryService {

    private final UsageViewRepository usageViewRepository;

    public UsageQueryResponse getUsage(String userId) {
        log.debug("Retrieving usage data for user: {}", userId);

        UsageView view = usageViewRepository.findById(userId)
                .orElseThrow(() -> new UsageNotFoundException(userId));

        return UsageQueryResponse.builder()
                .callUsage(view.getCallUsage())
                .callMinutes(view.getCallMinutes())
                .dataUsage(view.getDataUsage())
                .dataAllowance(view.getDataAllowance())
                .messageUsage(view.getMessageUsage())
                .messageCount(view.getMessageCount())
                .lastUpdated(view.getLastUpdated())
                .build();
    }
}
