package com.example.servicea.api.grpc;

import com.example.servicea.application.RateService;
import com.example.servicea.domain.RateQuote;
import com.example.rpc.v1.CurrencyRateServiceGrpc;
import com.example.rpc.v1.RateReply;
import com.example.rpc.v1.RateRequest;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class GrpcCurrencyRateService extends CurrencyRateServiceGrpc.CurrencyRateServiceImplBase {
    private static final Logger LOGGER = LoggerFactory.getLogger(GrpcCurrencyRateService.class);

    private final RateService rateService;

    public GrpcCurrencyRateService(RateService rateService) {
        this.rateService = rateService;
    }

    @Override
    public void getRate(RateRequest request, StreamObserver<RateReply> responseObserver) {
        LOGGER.info(
                "Received rate request baseCurrency={} quoteCurrency={}",
                request.getBaseCurrency(),
                request.getQuoteCurrency()
        );

        try {
            RateQuote quote = rateService.getRate(request.getBaseCurrency(), request.getQuoteCurrency());
            RateReply reply = RateReply.newBuilder()
                    .setPair(quote.pair())
                    .setRate(quote.rate())
                    .setTimestampEpochMillis(quote.timestamp().toEpochMilli())
                    .build();

            LOGGER.info(
                    "Sending rate response pair={} rate={} timestampEpochMillis={}",
                    reply.getPair(),
                    reply.getRate(),
                    reply.getTimestampEpochMillis()
            );

            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        } catch (Exception ex) {
            LOGGER.error(
                    "Failed to process rate request baseCurrency={} quoteCurrency={}",
                    request.getBaseCurrency(),
                    request.getQuoteCurrency(),
                    ex
            );
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Failed to calculate currency rate")
                            .withCause(ex)
                            .asRuntimeException()
            );
        }
    }
}
