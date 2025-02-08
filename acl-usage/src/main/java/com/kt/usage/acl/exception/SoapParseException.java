// acl-usage/src/main/java/com/kt/usage/acl/exception/SoapParseException.java
package com.kt.usage.acl.exception;

public class SoapParseException extends RuntimeException {
    public SoapParseException(String message) {
        super(message);
    }

    public SoapParseException(String message, Throwable cause) {
        super(message, cause);
    }
}