package com.example.currency_rate_provider.service;

import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Service;

@Service
public class CurrencyRateGenerator {

    private static final double USD_RUB_BASE_RATE = 90.0;
    private static final double DELTA = 5.0;

    public double getRate(String pair) throws IllegalArgumentException {
        if ("USDRUB".equalsIgnoreCase(pair)) {
            return generateRandomRate(USD_RUB_BASE_RATE);
        }

        throw new IllegalArgumentException(
                "Unsupported currency pair: " + pair
        );
    }

    private double generateRandomRate(double baseRate) {
        double min = baseRate - DELTA;
        double max = baseRate + DELTA;

        return ThreadLocalRandom.current()
                .nextDouble(min, max);
    }
}