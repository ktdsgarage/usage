// acl-usage/src/main/java/com/kt/usage/acl/dto/UsageDataXml.java
package com.kt.usage.acl.dto;

import jakarta.xml.bind.annotation.*;
import lombok.Data;

@XmlRootElement(name = "usageData")
@XmlAccessorType(XmlAccessType.FIELD)
@Data
public class UsageDataXml {
    @XmlElement(required = true)
    private String userSequence;

    @XmlElement(required = true)
    private String svcTypeCd;

    @XmlElement(required = true)
    private Integer usedQty;

    @XmlElement(required = true)
    private Integer baseQty;

    @XmlElement(required = true)
    private Integer exceedQty;
}