package com.example.rate_printer.service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import com.example.rate.grpc.CurrencyRateServiceGrpc;
import com.example.rate.grpc.RateRequest;
import com.example.rate.grpc.RateResponse;
import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.grpc.client.GrpcChannelFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import com.example.rate_printer.discovery.InstanceSelector;

@Service
public class RatePrinterService {
    private static final Logger log = LoggerFactory.getLogger(RatePrinterService.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.of("UTC"));

    // private final CurrencyRateServiceGrpc.CurrencyRateServiceBlockingStub blockingStub;
    private final InstanceSelector instanceSelector;

   @Autowired
    public RatePrinterService(InstanceSelector instanceSelector) {
        this.instanceSelector = instanceSelector;
    }

    @Scheduled(fixedRate = 5000)
    public void printRate() {
        String address = instanceSelector.selectInstance();

        ManagedChannel channel = ManagedChannelBuilder
                .forTarget(address)
                .usePlaintext()
                .build();

        try {
            CurrencyRateServiceGrpc.CurrencyRateServiceBlockingStub blockingStub =
                    CurrencyRateServiceGrpc.newBlockingStub(channel);

            RateRequest request = RateRequest.newBuilder()
                    .setPair("USDRUB")
                    .build();

            log.info("Sending rate request: target={}, pair={}", address, request.getPair());
            RateResponse response = blockingStub.getRate(request);
            String formattedTime = FORMATTER.format(Instant.ofEpochMilli(response.getTimestamp()));
            String rateMessage = String.format("USD/RUB: %.2f (timestamp: %s)", 
                    response.getRate(), formattedTime);
            log.info("Received rate response: target={}, pair={}, rate={}, timestamp={}",
                    address, response.getPair(), response.getRate(), response.getTimestamp());

            System.out.println("Request sent to " + address);
            System.out.println(rateMessage);

        } finally {
            channel.shutdown();
        }
    }
}


