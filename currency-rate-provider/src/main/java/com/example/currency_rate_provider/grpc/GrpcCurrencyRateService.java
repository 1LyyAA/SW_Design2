package com.example.currency_rate_provider.grpc;

import io.grpc.stub.StreamObserver;

import io.grpc.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.grpc.server.service.GrpcService;

import com.example.rate.grpc.CurrencyRateServiceGrpc;
import com.example.rate.grpc.RateRequest;
import com.example.rate.grpc.RateResponse;
import com.example.currency_rate_provider.service.CurrencyRateGenerator;

@GrpcService
public class GrpcCurrencyRateService
        extends CurrencyRateServiceGrpc.CurrencyRateServiceImplBase {
    
    private static final Logger log = LoggerFactory.getLogger(GrpcCurrencyRateService.class);
    
    private final CurrencyRateGenerator сurrencyRateGenerator;

    public GrpcCurrencyRateService(CurrencyRateGenerator сurrencyRateGenerator) {
        this.сurrencyRateGenerator = сurrencyRateGenerator;
    }

    @Override
    public void getRate(
            RateRequest request,
            StreamObserver<RateResponse> responseObserver) {

        String pair = request.getPair();
        log.info("Received rate request: pair={}", pair);

        try {
            double rate = сurrencyRateGenerator.getRate(pair);
            long timestamp = System.currentTimeMillis();
            RateResponse response = RateResponse.newBuilder()
                .setPair(pair)
                .setRate(rate)
                .setTimestamp(timestamp)
                .build();

                responseObserver.onNext(response);
                responseObserver.onCompleted();
                log.info("Sent rate response: pair={}, rate={}, timestamp={}",
                        response.getPair(), response.getRate(), response.getTimestamp());

        } catch (IllegalArgumentException e) {
            log.warn("Sent rate error response: pair={}, status={}, message={}",
                    pair, Status.Code.INVALID_ARGUMENT, e.getMessage());
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
            return;
        }
        

        
    }
}
