package com.example.servicea.infrastructure.grpc;

import io.grpc.ForwardingServerCall;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

@Component
public class GrpcServerMetricsInterceptor implements ServerInterceptor {
    public static final Metadata.Key<String> CLIENT_NAME_HEADER =
            Metadata.Key.of("x-client-name", Metadata.ASCII_STRING_MARSHALLER);

    private final MeterRegistry meterRegistry;

    public GrpcServerMetricsInterceptor(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next
    ) {
        String clientName = resolveClientName(headers);
        String methodName = call.getMethodDescriptor().getFullMethodName();

        Counter.builder("grpc.server.requests")
                .description("Total number of gRPC requests handled by the server")
                .tags("client", clientName, "method", methodName)
                .register(meterRegistry)
                .increment();

        Timer.Sample sample = Timer.start(meterRegistry);

        ServerCall<ReqT, RespT> monitoringCall = new ForwardingServerCall.SimpleForwardingServerCall<>(call) {
            @Override
            public void close(Status status, Metadata trailers) {
                sample.stop(
                        Timer.builder("grpc.server.request.duration")
                                .description("Duration of gRPC requests handled by the server")
                                .tags(
                                        "client", clientName,
                                        "method", methodName,
                                        "status", status.getCode().name()
                                )
                                .publishPercentileHistogram()
                                .publishPercentiles(0.5, 0.95, 0.99)
                                .register(meterRegistry)
                );

                if (status.getCode() == Status.Code.INTERNAL) {
                    Counter.builder("grpc.server.errors")
                            .description("Number of INTERNAL gRPC errors returned by the server")
                            .tags("client", clientName, "method", methodName)
                            .register(meterRegistry)
                            .increment();
                }

                super.close(status, trailers);
            }
        };

        return next.startCall(monitoringCall, headers);
    }

    private String resolveClientName(Metadata headers) {
        String clientName = headers.get(CLIENT_NAME_HEADER);
        if (clientName == null || clientName.isBlank()) {
            return "unknown";
        }
        return clientName;
    }
}
