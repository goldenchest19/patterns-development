package com.example.servicea.infrastructure.grpc;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.zookeeper.discovery.ZookeeperDiscoveryProperties;
import org.springframework.cloud.zookeeper.discovery.ZookeeperInstance;
import org.springframework.cloud.zookeeper.serviceregistry.ServiceInstanceRegistration;
import org.springframework.cloud.zookeeper.serviceregistry.ZookeeperRegistration;
import org.springframework.cloud.zookeeper.serviceregistry.ZookeeperServiceRegistry;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.example.servicea.api.grpc.GrpcCurrencyRateService;
import com.example.servicea.infrastructure.config.GrpcServerProperties;

@Component
public class GrpcServerLifecycle implements SmartLifecycle {
    private static final Logger LOGGER = LoggerFactory.getLogger(GrpcServerLifecycle.class);

    private final Server server;
    private final GrpcServerProperties properties;
    private final ZookeeperServiceRegistry serviceRegistry;
    private final ZookeeperDiscoveryProperties discoveryProperties;
    private final Environment environment;
    private Thread awaitThread;
    private ZookeeperRegistration registration;
    private volatile boolean running;

    public GrpcServerLifecycle(
            GrpcServerProperties properties,
            GrpcCurrencyRateService service,
            ZookeeperServiceRegistry serviceRegistry,
            ZookeeperDiscoveryProperties discoveryProperties,
            Environment environment
    ) {
        this.properties = properties;
        this.serviceRegistry = serviceRegistry;
        this.discoveryProperties = discoveryProperties;
        this.environment = environment;
        this.server = ServerBuilder.forPort(properties.getPort())
                .addService(service)
                .build();
    }

    @Override
    public void start() {
        try {
            server.start();
            registration = buildRegistration();
            serviceRegistry.register(registration);
            running = true;
            LOGGER.info("gRPC server started on port {}", server.getPort());
            awaitThread = new Thread(this::awaitTermination, "grpc-server-await");
            awaitThread.setDaemon(false);
            awaitThread.start();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to start gRPC server", ex);
        }
    }

    @Override
    public void stop() {
        if (registration != null) {
            serviceRegistry.deregister(registration);
        }
        server.shutdown();
        try {
            if (!server.awaitTermination(5, TimeUnit.SECONDS)) {
                server.shutdownNow();
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            server.shutdownNow();
        }
        running = false;
    }

    private void awaitTermination() {
        try {
            server.awaitTermination();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private ZookeeperRegistration buildRegistration() {
        String serviceName = environment.getProperty("spring.application.name", "application");
        String host = discoveryProperties.getInstanceHost();
        if (host == null || host.isBlank()) {
            host = "localhost";
        }

        String instanceId = discoveryProperties.getInstanceId();
        if (instanceId == null || instanceId.isBlank()) {
            instanceId = serviceName + "-" + properties.getPort() + "-" + UUID.randomUUID();
        }

        Map<String, String> metadata = new HashMap<>(discoveryProperties.getMetadata());
        metadata.put("grpc.port", Integer.toString(properties.getPort()));

        ZookeeperInstance payload = new ZookeeperInstance(instanceId, serviceName, metadata);
        return ServiceInstanceRegistration.builder()
                .name(serviceName)
                .address(host)
                .port(properties.getPort())
                .id(instanceId)
                .payload(payload)
                .uriSpec(discoveryProperties.getUriSpec())
                .build();
    }

    @Override
    public void stop(Runnable callback) {
        stop();
        callback.run();
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE;
    }
}
