package com.example.rate_printer.pact;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactBuilder;
import au.com.dius.pact.consumer.junit.MockServerConfig;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.consumer.junit5.ProviderType;
import au.com.dius.pact.consumer.model.MockServerImplementation;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.V4Interaction;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import com.example.rate.grpc.CurrencyRateServiceGrpc;
import com.example.rate.grpc.RateRequest;
import com.example.rate.grpc.RateResponse;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;

import static au.com.dius.pact.consumer.dsl.PactBuilder.filePath;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(
        providerName = "currency-rate-provider",
        providerType = ProviderType.SYNCH_MESSAGE,
        pactVersion = PactSpecVersion.V4
)   
class RatePrinterGrpcConsumerPactTest {

    @Pact(consumer = "rate-printer")
    V4Pact getRatePact(PactBuilder builder) {
        return builder
                .usingPlugin("protobuf")
                .expectsToReceive(
                        "gRPC request for USDRUB rate",
                        "core/interaction/synchronous-message"
                )
                .with(Map.of(
                        "pact:proto", filePath("src/main/proto/rate.proto"),
                        "pact:content-type", "application/grpc",
                        "pact:proto-service", "CurrencyRateService/GetRate",

                        "request", Map.of(
                                "pair", "matching(type, 'USDRUB')"
                        ),

                        "response", Map.of(
                                "pair", "matching(type, 'USDRUB')",
                                "rate", "matching(number, 90.5)",
                                "timestamp", "matching(integer, 1710000000000)"
                        )
                ))
                .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "getRatePact")
    @MockServerConfig(
            implementation = MockServerImplementation.Plugin,
            registryEntry = "protobuf/transport/grpc"
    )
    void shouldReceiveRateByGrpc(
            MockServer mockServer,
            V4Interaction.SynchronousMessages interaction
    ) {
        ManagedChannel channel = ManagedChannelBuilder
                .forTarget("127.0.0.1:" + mockServer.getPort())
                .usePlaintext()
                .build();

        try {
            CurrencyRateServiceGrpc.CurrencyRateServiceBlockingStub stub =
                    CurrencyRateServiceGrpc.newBlockingStub(channel);

            RateRequest request = RateRequest.parseFrom(
                    interaction.getRequest().getContents().getValue()
            );

            RateResponse response = stub.getRate(request);

            assertEquals("USDRUB", response.getPair());
            assertEquals(90.5, response.getRate());
            assertEquals(1710000000000L, response.getTimestamp());
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            channel.shutdownNow();
        }
    }
}