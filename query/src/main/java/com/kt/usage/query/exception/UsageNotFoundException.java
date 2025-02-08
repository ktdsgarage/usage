// query/src/main/java/com/kt/usage/query/exception/UsageNotFoundException.java
package com.kt.usage.query.exception;

public class UsageNotFoundException extends RuntimeException {
    public UsageNotFoundException(String userId) {
        super("Usage data not found for user: " + userId);
    }
}