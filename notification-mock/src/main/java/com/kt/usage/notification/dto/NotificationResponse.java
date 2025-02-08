package com.kt.usage.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "알림 발송 응답")
public class NotificationResponse {
    
    @Schema(description = "발송 성공 여부")
    private boolean success;
    
    @Schema(description = "처리 메시지")
    private String message;
    
    @Schema(description = "사용자 ID")
    private String userId;
    
    @Schema(description = "발송 채널")
    private String channel;
}
