// query/src/main/java/com/kt/usage/query/dto/UsageQueryResponse.java
package com.kt.usage.query.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "사용 현황 조회 응답")
public class UsageQueryResponse {

    @Schema(description = "음성 통화 현재 사용량 (초)")
    private Integer callUsage;

    @Schema(description = "음성 통화 기본 제공량 (초)")
    private Integer callMinutes;

    @Schema(description = "데이터 현재 사용량 (MB)")
    private Integer dataUsage;

    @Schema(description = "데이터 기본 제공량 (MB)")
    private Integer dataAllowance;

    @Schema(description = "문자 현재 사용량 (건)")
    private Integer messageUsage;

    @Schema(description = "문자 기본 제공량 (건)")
    private Integer messageCount;

    @Schema(description = "마지막 업데이트 시간")
    private LocalDateTime lastUpdated;
}
