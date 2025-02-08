package com.kt.usage.notification.service;

import com.azure.messaging.servicebus.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kt.usage.notification.dto.NotificationRequest;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationEventHandler {

    private final ObjectMapper objectMapper;
    private ServiceBusProcessorClient processorClient;

    @Value("${azure.servicebus.connection-string}")
    private String connectionString;

    @Value("${azure.servicebus.topic-name}")
    private String topicName;

    @Value("${azure.servicebus.subscription-name}")
    private String subscriptionName;

    @PostConstruct
    public void initialize() {
        processorClient = new ServiceBusClientBuilder()
                .connectionString(connectionString)
                .processor()
                .topicName(topicName)
                .subscriptionName(subscriptionName)
                .processMessage(this::processMessage)
                .processError(context -> processError(context.getException()))
                .buildProcessorClient();

        processorClient.start();
    }

    private void processMessage(ServiceBusReceivedMessageContext context) {
        ServiceBusReceivedMessage message = context.getMessage();
        try {
            log.debug("Processing message: {}", message.getBody());

            NotificationRequest notificationRequest = createNotificationRequest(message);
            log.info("Notification would be sent: {}", notificationRequest);

            // 성공적으로 처리된 경우 checkpoint 수행
            context.complete();
            log.debug("Message processed and checkpointed. MessageId: {}", message.getMessageId());

        } catch (Exception e) {
            log.error("Error processing message: {}", e.getMessage(), e);

            // 실패한 경우 abandon 처리하여 재처리 큐로
            context.abandon();
        }
    }

    private NotificationRequest createNotificationRequest(ServiceBusReceivedMessage message) throws Exception {
        String messageBody = message.getBody().toString();
        JsonNode usageData = objectMapper.readTree(messageBody);

        return NotificationRequest.builder()
                .userId(usageData.get("userId").asText())
                .channel("SMS")  // 기본값으로 SMS 설정
                .message(createNotificationMessage(usageData))
                .build();
    }

    private String createNotificationMessage(JsonNode usageData) {
        String serviceType = usageData.get("serviceType").asText();
        int usedAmount = usageData.get("usedAmount").asInt();
        int baseAmount = usageData.get("baseAmount").asInt();

        double usageRate = (baseAmount > 0) ?
                ((double) usedAmount / baseAmount) * 100 : 0;

        String serviceTypeName = switch(serviceType) {
            case "V" -> "음성";
            case "D" -> "데이터";
            case "S" -> "문자";
            default -> "기타";
        };

        return String.format("%s 사용량이 %.1f%%에 도달했습니다.",
                serviceTypeName, usageRate);
    }

    private void processError(Throwable error) {
        log.error("Error occurred while processing message: {}", error.getMessage(), error);
    }

    @PreDestroy
    public void cleanup() {
        if (processorClient != null) {
            processorClient.close();
        }
    }
}