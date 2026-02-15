package com.example.serviceb.infrastructure.grpc;

import com.example.rpc.v1.CurrencyRateServiceGrpc;
import com.example.rpc.v1.RateReply;
import com.example.rpc.v1.RateRequest;
import com.example.serviceb.application.RateClient;
import com.example.serviceb.domain.RateQuote;
import com.example.serviceb.infrastructure.config.GrpcClientProperties;
import com.example.serviceb.infrastructure.config.RateProviderDiscoveryProperties;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import jakarta.annotation.PreDestroy;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class GrpcRateClient implements RateClient {
    private final DiscoveryClient discoveryClient;
    private final RateProviderDiscoveryProperties discoveryProperties;
    private final GrpcClientProperties properties;
    private final AtomicInteger nextIndex = new AtomicInteger(0);
    private final Map<String, ManagedChannel> channels = new ConcurrentHashMap<>();

    public GrpcRateClient(
            DiscoveryClient discoveryClient,
            RateProviderDiscoveryProperties discoveryProperties,
            GrpcClientProperties properties
    ) {
        this.discoveryClient = discoveryClient;
        this.discoveryProperties = discoveryProperties;
        this.properties = properties;
    }

    @Override
    public RateQuote getRate(String baseCurrency, String quoteCurrency) {
        ServiceInstance instance = chooseInstance();
        RateRequest request = RateRequest.newBuilder()
                .setBaseCurrency(baseCurrency)
                .setQuoteCurrency(quoteCurrency)
                .build();

        CurrencyRateServiceGrpc.CurrencyRateServiceBlockingStub stub = CurrencyRateServiceGrpc.newBlockingStub(
                channelFor(instance)
        );
        RateReply reply = stub.withDeadlineAfter(properties.getDeadlineMillis(), TimeUnit.MILLISECONDS)
                .getRate(request);
        return new RateQuote(reply.getPair(), reply.getRate(), Instant.ofEpochMilli(reply.getTimestampEpochMillis()));
    }

    private ServiceInstance chooseInstance() {
        List<ServiceInstance> instances = discoveryClient.getInstances(discoveryProperties.getServiceName());
        if (instances.isEmpty()) {
            throw new IllegalStateException(
                    "No instances discovered for service: " + discoveryProperties.getServiceName()
            );
        }

        int index = Math.floorMod(nextIndex.getAndIncrement(), instances.size());
        return instances.get(index);
    }

    private ManagedChannel channelFor(ServiceInstance instance) {
        String key = instance.getHost() + ":" + instance.getPort();
        return channels.computeIfAbsent(
                key,
                ignored -> ManagedChannelBuilder.forAddress(instance.getHost(), instance.getPort())
                        .usePlaintext()
                        .build()
        );
    }

    @PreDestroy
    public void shutdown() throws InterruptedException {
        for (ManagedChannel channel : channels.values()) {
            channel.shutdown();
        }
        for (ManagedChannel channel : channels.values()) {
            if (!channel.awaitTermination(5, TimeUnit.SECONDS)) {
                channel.shutdownNow();
            }
        }
    }
}
