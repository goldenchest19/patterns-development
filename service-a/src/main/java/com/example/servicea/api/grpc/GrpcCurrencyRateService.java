package com.example.servicea.api.grpc;

import com.example.servicea.application.RateService;
import com.example.servicea.domain.RateQuote;
import com.example.rpc.v1.CurrencyRateServiceGrpc;
import com.example.rpc.v1.RateReply;
import com.example.rpc.v1.RateRequest;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Component;

@Component
public class GrpcCurrencyRateService extends CurrencyRateServiceGrpc.CurrencyRateServiceImplBase {
    private final RateService rateService;

    public GrpcCurrencyRateService(RateService rateService) {
        this.rateService = rateService;
    }

    @Override
    public void getRate(RateRequest request, StreamObserver<RateReply> responseObserver) {
        RateQuote quote = rateService.getRate(request.getBaseCurrency(), request.getQuoteCurrency());
        RateReply reply = RateReply.newBuilder()
                .setPair(quote.pair())
                .setRate(quote.rate())
                .setTimestampEpochMillis(quote.timestamp().toEpochMilli())
                .build();
        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }
}
