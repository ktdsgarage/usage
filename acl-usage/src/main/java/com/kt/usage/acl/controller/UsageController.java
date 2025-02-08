package com.kt.usage.acl.controller;

import com.kt.usage.acl.service.UsageService;
import com.kt.usage.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/usages")
@RequiredArgsConstructor
@Tag(name = "사용량 데이터 수신 API", description = "Legacy KOS 시스템으로부터 사용량 데이터를 수신합니다.")
public class UsageController {

    private final UsageService usageService;

    @Operation(summary = "사용량 데이터 수신", description = "Legacy 시스템의 사용량 데이터를 XML 형식으로 수신합니다.")
    @PostMapping(consumes = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<ApiResponse<String>> receiveUsageData(@RequestBody String usageData) {
        log.info("Received usage data: {}", usageData);
        usageService.processUsageData(usageData);
        return ResponseEntity.ok(ApiResponse.success("데이터가 성공적으로 처리되었습니다."));
    }
}
