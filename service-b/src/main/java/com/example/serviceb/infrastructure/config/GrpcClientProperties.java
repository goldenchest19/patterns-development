package com.example.serviceb.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "grpc.client")
public class GrpcClientProperties {
    private long deadlineMillis = 3000;

    public long getDeadlineMillis() {
        return deadlineMillis;
    }

    public void setDeadlineMillis(long deadlineMillis) {
        this.deadlineMillis = deadlineMillis;
    }
}
