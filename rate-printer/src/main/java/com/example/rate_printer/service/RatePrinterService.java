package com.example.rate_printer.service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import com.example.rate.grpc.CurrencyRateServiceGrpc;
import com.example.rate.grpc.RateRequest;
import com.example.rate.grpc.RateResponse;
import io.grpc.Channel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.grpc.client.GrpcChannelFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class RatePrinterService {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.of("UTC"));

    private final CurrencyRateServiceGrpc.CurrencyRateServiceBlockingStub blockingStub;

    @Autowired
    public RatePrinterService(GrpcChannelFactory channelFactory) {
        Channel channel = channelFactory.createChannel("currency-rate-provider");
        this.blockingStub = CurrencyRateServiceGrpc.newBlockingStub(channel);
    }

    @Scheduled(fixedRate = 5000)
    public void printRate() {
        try {
            String pair = "USDRUB";
            RateRequest request = RateRequest.newBuilder().setPair(pair).build();
            RateResponse response = blockingStub.getRate(request);
            // String timestamp = FORMATTER.format(response.getTimestamp().toInstant());
            String formattedTime = FORMATTER.format(Instant.ofEpochMilli(response.getTimestamp()));
            String rateMessage = String.format("USD/RUB: %.2f (timestamp: %s)", 
                    response.getRate(), formattedTime);

            System.out.println(rateMessage);
        } catch (Exception e) {
            System.err.println("Error fetching rate: " + e.getMessage());
        }
    }


}
