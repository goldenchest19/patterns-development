package com.example.serviceb.infrastructure.grpc;

import com.example.serviceb.application.RateClient;
import com.example.serviceb.domain.RateQuote;
import com.example.serviceb.infrastructure.config.GrpcClientProperties;
import com.example.rpc.v1.CurrencyRateServiceGrpc;
import com.example.rpc.v1.RateReply;
import com.example.rpc.v1.RateRequest;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Component
public class GrpcRateClient implements RateClient {
    private final ManagedChannel channel;
    private final CurrencyRateServiceGrpc.CurrencyRateServiceBlockingStub stub;
    private final GrpcClientProperties properties;

    public GrpcRateClient(GrpcClientProperties properties) {
        this.properties = properties;
        this.channel = ManagedChannelBuilder.forTarget(properties.getTarget())
                .usePlaintext()
                .build();
        this.stub = CurrencyRateServiceGrpc.newBlockingStub(channel);
    }

    @Override
    public RateQuote getRate(String baseCurrency, String quoteCurrency) {
        RateRequest request = RateRequest.newBuilder()
                .setBaseCurrency(baseCurrency)
                .setQuoteCurrency(quoteCurrency)
                .build();
        RateReply reply = stub.withDeadlineAfter(properties.getDeadlineMillis(), TimeUnit.MILLISECONDS)
                .getRate(request);
        return new RateQuote(reply.getPair(), reply.getRate(), Instant.ofEpochMilli(reply.getTimestampEpochMillis()));
    }

    @PreDestroy
    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
}
