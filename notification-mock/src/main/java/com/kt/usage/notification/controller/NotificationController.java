package com.kt.usage.notification.controller;

import com.kt.usage.common.dto.ApiResponse;
import com.kt.usage.notification.dto.NotificationRequest;
import com.kt.usage.notification.dto.NotificationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/notifications")
@Tag(name = "알림 발송 Mock API", description = "알림 발송을 시뮬레이션하는 Mock API")
public class NotificationController {

    @Operation(summary = "알림 발송", description = "각 채널별 알림 발송을 시뮬레이션")
    @PostMapping("/send")
    public ResponseEntity<ApiResponse<NotificationResponse>> sendNotification(
            @RequestBody NotificationRequest request) {
        log.info("Notification request received: {}", request);
        
        NotificationResponse response = NotificationResponse.builder()
                .success(true)
                .message("알림이 발송되었습니다.")
                .userId(request.getUserId())
                .channel(request.getChannel())
                .build();
                
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
