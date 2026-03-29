package com.example.servicea.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "grpc.server")
public class GrpcServerProperties {
    private int port = 50051;

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
