package com.kt.usage.acl.config;

import com.kt.usage.acl.dto.UsageDataXml;
import jakarta.xml.bind.annotation.*;
import lombok.Data;

@XmlRootElement(name = "Envelope", namespace = "http://schemas.xmlsoap.org/soap/envelope/")
@XmlAccessorType(XmlAccessType.FIELD)
@Data
public class SoapEnvelope {

    @XmlElement(name = "Body", namespace = "http://schemas.xmlsoap.org/soap/envelope/")
    private SoapBody body;

    @XmlAccessorType(XmlAccessType.FIELD)
    @Data
    public static class SoapBody {

        @XmlElement(name = "usageData")
        private UsageDataXml usageData;
    }
}
