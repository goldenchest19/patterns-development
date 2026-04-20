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
import io.grpc.Metadata;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.MetadataUtils;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class GrpcRateClient implements RateClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(GrpcRateClient.class);
    private static final Metadata.Key<String> CLIENT_NAME_HEADER =
            Metadata.Key.of("x-client-name", Metadata.ASCII_STRING_MARSHALLER);

    private final DiscoveryClient discoveryClient;
    private final RateProviderDiscoveryProperties discoveryProperties;
    private final GrpcClientProperties properties;
    private final String clientName;
    private final AtomicInteger nextIndex = new AtomicInteger(0);
    private final Map<String, ManagedChannel> channels = new ConcurrentHashMap<>();

    public GrpcRateClient(
            DiscoveryClient discoveryClient,
            RateProviderDiscoveryProperties discoveryProperties,
            GrpcClientProperties properties,
            Environment environment
    ) {
        this.discoveryClient = discoveryClient;
        this.discoveryProperties = discoveryProperties;
        this.properties = properties;
        this.clientName = environment.getProperty("spring.application.name", "unknown-client");
    }

    @Override
    public RateQuote getRate(String baseCurrency, String quoteCurrency) {
        ServiceInstance instance = chooseInstance();
        RateRequest request = RateRequest.newBuilder()
                .setBaseCurrency(baseCurrency)
                .setQuoteCurrency(quoteCurrency)
                .build();

        LOGGER.info(
                "Sending rate request to {}:{} baseCurrency={} quoteCurrency={}",
                instance.getHost(),
                instance.getPort(),
                baseCurrency,
                quoteCurrency
        );

        Metadata headers = new Metadata();
        headers.put(CLIENT_NAME_HEADER, clientName);

        CurrencyRateServiceGrpc.CurrencyRateServiceBlockingStub stub = CurrencyRateServiceGrpc.newBlockingStub(
                channelFor(instance)
        ).withInterceptors(MetadataUtils.newAttachHeadersInterceptor(headers));

        try {
            RateReply reply = stub.withDeadlineAfter(properties.getDeadlineMillis(), TimeUnit.MILLISECONDS)
                    .getRate(request);

            LOGGER.info(
                    "Received rate response from {}:{} pair={} rate={} timestampEpochMillis={}",
                    instance.getHost(),
                    instance.getPort(),
                    reply.getPair(),
                    reply.getRate(),
                    reply.getTimestampEpochMillis()
            );

            return new RateQuote(
                    reply.getPair(),
                    reply.getRate(),
                    Instant.ofEpochMilli(reply.getTimestampEpochMillis())
            );
        } catch (StatusRuntimeException ex) {
            LOGGER.warn(
                    "Rate request failed for {}:{} status={}",
                    instance.getHost(),
                    instance.getPort(),
                    ex.getStatus().getCode(),
                    ex
            );
            throw ex;
        }
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
        LOGGER.info("Shutting down {} gRPC channel(s), timeout={}", channels.size(), properties.getShutdownGracePeriod());
        for (ManagedChannel channel : channels.values()) {
            channel.shutdown();
        }
        for (ManagedChannel channel : channels.values()) {
            if (!channel.awaitTermination(properties.getShutdownGracePeriod().toMillis(), TimeUnit.MILLISECONDS)) {
                channel.shutdownNow();
            }
        }
        LOGGER.info("gRPC channels stopped");
    }
}
