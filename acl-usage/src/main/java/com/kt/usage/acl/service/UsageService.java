// acl-usage/src/main/java/com/kt/usage/acl/service/UsageService.java
package com.kt.usage.acl.service;

import com.azure.messaging.servicebus.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kt.usage.acl.dto.UsageDataJson;
import com.kt.usage.acl.dto.UsageDataXml;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class UsageService {

    private final ObjectMapper objectMapper;
    private final SoapParserService soapParserService;

    @Value("${azure.servicebus.connection-string}")
    private String connectionString;

    @Value("${azure.servicebus.usage-topic}")
    private String usageTopic;

    @Value("${azure.servicebus.notify-topic}")
    private String notifyTopic;

    public void processUsageData(String soapXml) {
        try {
            // SOAP XML 파싱
            UsageDataXml xmlData = soapParserService.parseSoapXml(soapXml);

            // 기존 로직 동일
            UsageDataJson jsonData = convertToJson(xmlData);
            String jsonString = objectMapper.writeValueAsString(jsonData);

            sendToServiceBus(jsonString);

        } catch (Exception e) {
            log.error("Error processing usage data: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process usage data", e);
        }
    }

    private void sendToServiceBus(String jsonString) {
        try (ServiceBusSenderClient usageSender = new ServiceBusClientBuilder()
                .connectionString(connectionString)
                .sender()
                .topicName(usageTopic)
                .buildClient();
             ServiceBusSenderClient notifySender = new ServiceBusClientBuilder()
                     .connectionString(connectionString)
                     .sender()
                     .topicName(notifyTopic)
                     .buildClient()) {

            ServiceBusMessage usageMessage = new ServiceBusMessage(jsonString);
            ServiceBusMessage notifyMessage = new ServiceBusMessage(jsonString);

            usageSender.sendMessage(usageMessage);
            log.debug("Sent usage update message: {}", jsonString);

            notifySender.sendMessage(notifyMessage);
            log.debug("Sent notification message: {}", jsonString);
        }
    }

    private UsageDataJson convertToJson(UsageDataXml xmlData) {
        return UsageDataJson.builder()
                .userId(xmlData.getUserSequence())
                .serviceType(xmlData.getSvcTypeCd())
                .usedAmount(xmlData.getUsedQty())
                .baseAmount(xmlData.getBaseQty())
                .excessAmount(xmlData.getExceedQty())
                .updateTimestamp(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")))
                .build();
    }
}