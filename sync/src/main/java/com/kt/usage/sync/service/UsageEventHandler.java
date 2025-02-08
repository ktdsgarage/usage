package com.kt.usage.sync.service;

import com.azure.messaging.servicebus.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kt.usage.sync.model.UsageView;
import com.kt.usage.sync.repository.UsageViewRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class UsageEventHandler {

    private final ObjectMapper objectMapper;
    private final MongoTemplate mongoTemplate;
    private final UsageViewRepository usageViewRepository;
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
            String messageBody = message.getBody().toString();
            var usageData = objectMapper.readTree(messageBody);
            
            String userId = usageData.get("userId").asText();
            String serviceType = usageData.get("serviceType").asText();
            int usedAmount = usageData.get("usedAmount").asInt();
            int baseAmount = usageData.get("baseAmount").asInt();

            Query query = new Query(Criteria.where("userId").is(userId));
            Update update = new Update().set("lastUpdated", LocalDateTime.now());

            switch (serviceType) {
                case "V" -> {
                    update.set("callUsage", usedAmount);
                    update.set("callMinutes", baseAmount);
                }
                case "D" -> {
                    update.set("dataUsage", usedAmount);
                    update.set("dataAllowance", baseAmount);
                }
                case "S" -> {
                    update.set("messageUsage", usedAmount);
                    update.set("messageCount", baseAmount);
                }
                default -> {
                    log.warn("Unknown service type: {}", serviceType);
                    return;
                }
            }

            mongoTemplate.upsert(query, update, UsageView.class);
            log.debug("Updated usage view for user {} - Service: {}, Used: {}, Base: {}", 
                userId, serviceType, usedAmount, baseAmount);

            // 성공적으로 처리된 경우 checkpoint 수행
            context.complete();
            log.debug("Message processed and checkpointed. MessageId: {}", message.getMessageId());

        } catch (Exception e) {
            log.error("Error processing message: {}", e.getMessage(), e);

            // 실패한 경우 abandon 처리하여 재처리 큐로
            context.abandon();
        }
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
