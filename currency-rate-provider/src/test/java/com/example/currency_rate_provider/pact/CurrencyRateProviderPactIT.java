package com.example.currency_rate_provider.pact;

import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junit5.PluginTestTarget;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.loader.PactBroker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;

@Provider("currency-rate-provider")
@PactBroker(url = "http://localhost:9292")
class CurrencyRateProviderPactIT {

    @BeforeEach
    void before(PactVerificationContext context) {
        context.setTarget(new PluginTestTarget(Map.of(
                "host", "127.0.0.1",
                "port", 9090,
                "transport", "grpc"
        )));
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void verifyPact(PactVerificationContext context) {
        context.verifyInteraction();
    }
}