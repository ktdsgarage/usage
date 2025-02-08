// acl-usage/src/main/java/com/kt/usage/acl/service/SoapParserService.java
package com.kt.usage.acl.service;

import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import com.kt.usage.acl.dto.UsageDataXml;
import com.kt.usage.acl.exception.SoapParseException;
import com.kt.usage.acl.config.SoapEnvelope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.io.StringReader;

@Slf4j
@Service
@RequiredArgsConstructor
public class SoapParserService {

    private final Unmarshaller unmarshaller;

    public UsageDataXml parseSoapXml(String soapXml) {
        try {
            log.debug("SOAP XML 파싱 시작: {}", soapXml);

            // SOAP Envelope로 파싱 
            SoapEnvelope envelope = (SoapEnvelope) unmarshaller.unmarshal(new StringReader(soapXml));

            if (envelope == null || envelope.getBody() == null || envelope.getBody().getUsageData() == null) {
                throw new SoapParseException("Invalid SOAP message structure");
            }

            // Body 내의 usageData 반환
            UsageDataXml usageData = envelope.getBody().getUsageData();
            log.debug("SOAP XML 파싱 완료: {}", usageData);
            return usageData;

        } catch (JAXBException e) {
            log.error("SOAP XML 파싱 실패: {}", e.getMessage(), e);
            throw new SoapParseException("SOAP XML 파싱 실패", e);
        }
    }
}