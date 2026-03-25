package com.example.servicea.contract;

import com.example.rpc.v1.CurrencyRateServiceGrpc;
import com.example.rpc.v1.RateReply;
import com.example.rpc.v1.RateRequest;
import com.example.servicea.api.grpc.GrpcCurrencyRateService;
import com.example.servicea.application.RateProvider;
import com.example.servicea.application.RateService;
import com.example.servicea.infrastructure.config.RateProperties;
import com.example.servicea.infrastructure.rate.RandomRateProvider;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junit5.MessageTestTarget;
import au.com.dius.pact.provider.PactVerifyProvider;
import au.com.dius.pact.provider.junitsupport.IgnoreNoPactsToVerify;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.loader.PactBroker;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

@Provider("rate-provider")
@PactBroker(url = "${pactbroker.url:http://localhost:9292}")
@IgnoreNoPactsToVerify
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RateProviderPactVerificationTest {
    private Server server;
    private ManagedChannel channel;
    private CurrencyRateServiceGrpc.CurrencyRateServiceBlockingStub stub;

    @BeforeAll
    void startProvider() throws IOException {
        RateProperties properties = new RateProperties();
        properties.setBaseValue(90.0);
        properties.setSpread(1.0);

        RateProvider rateProvider = new RandomRateProvider(properties);
        RateService rateService = new RateService(rateProvider);
        GrpcCurrencyRateService grpcService = new GrpcCurrencyRateService(rateService);

        server = ServerBuilder.forPort(0)
                .addService(grpcService)
                .build()
                .start();

        channel = ManagedChannelBuilder.forAddress("localhost", server.getPort())
                .usePlaintext()
                .build();
        stub = CurrencyRateServiceGrpc.newBlockingStub(channel);
    }

    @AfterAll
    void stopProvider() throws InterruptedException {
        if (channel != null) {
            channel.shutdown();
            if (!channel.awaitTermination(5, TimeUnit.SECONDS)) {
                channel.shutdownNow();
            }
        }

        if (server != null) {
            server.shutdown();
            if (!server.awaitTermination(5, TimeUnit.SECONDS)) {
                server.shutdownNow();
            }
        }
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void verifyPact(PactVerificationContext context) {
        context.verifyInteraction();
    }

    @BeforeEach
    void before(PactVerificationContext context) {
        context.setTarget(new MessageTestTarget());
    }

    @PactVerifyProvider("a current USD/RUB rate message")
    String currentRateMessage() {
        RateReply reply = stub.getRate(
                RateRequest.newBuilder()
                        .setBaseCurrency("USD")
                        .setQuoteCurrency("RUB")
                        .build()
        );
        return String.format(
                Locale.US,
                "{\"pair\":\"%s\",\"rate\":%.4f,\"timestampEpochMillis\":%d}",
                reply.getPair(),
                reply.getRate(),
                reply.getTimestampEpochMillis()
        );
    }
}
