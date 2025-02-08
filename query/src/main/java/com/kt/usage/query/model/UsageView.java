// query/src/main/java/com/kt/usage/query/model/UsageView.java
package com.kt.usage.query.model;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Data
@Builder
@Document(collection = "usage_views")
public class UsageView {
    @Id
    private String userId;

    private Integer callUsage;
    private Integer callMinutes;

    private Integer dataUsage;
    private Integer dataAllowance;

    private Integer messageUsage;
    private Integer messageCount;

    private LocalDateTime lastUpdated;
}