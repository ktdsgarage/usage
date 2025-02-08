// acl-usage/src/main/java/com/kt/usage/acl/dto/UsageDataJson.java
package com.kt.usage.acl.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UsageDataJson {
    private String userId;
    private String serviceType;
    private Integer usedAmount;
    private Integer baseAmount;
    private Integer excessAmount;

    @JsonFormat(pattern = "yyyyMMddHHmmss")
    private String updateTimestamp;
}