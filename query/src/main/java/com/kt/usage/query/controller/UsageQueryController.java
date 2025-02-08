// query/src/main/java/com/kt/usage/query/controller/UsageQueryController.java
package com.kt.usage.query.controller;

import com.kt.usage.common.dto.ApiResponse;
import com.kt.usage.query.dto.UsageQueryResponse;
import com.kt.usage.query.service.UsageQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/plans/query")
@RequiredArgsConstructor
@Tag(name = "사용 현황 조회 API", description = "사용 현황을 제공합니다.")
public class UsageQueryController {

    private final UsageQueryService usageQueryService;

    @Operation(summary = "사용 현황 조회", description = "사용자의 사용 현황을 조회합니다.")
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UsageQueryResponse>> getUsage(
            @Parameter(description = "사용자 ID", example = "user123")
            @PathVariable String userId) {
        log.debug("Querying usage for user: {}", userId);

        UsageQueryResponse response = usageQueryService.getUsage(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}