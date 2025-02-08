package com.kt.usage.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "알림 발송 요청")
public class NotificationRequest {
    
    @Schema(description = "사용자 ID", example = "user123")
    private String userId;
    
    @Schema(description = "발송 채널(SMS/PUSH/EMAIL)", example = "SMS")
    private String channel;
    
    @Schema(description = "발송 메시지", example = "데이터 사용량이 90%를 초과하였습니다.")
    private String message;
}
