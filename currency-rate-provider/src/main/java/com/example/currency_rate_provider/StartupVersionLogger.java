package com.example.currency_rate_provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class StartupVersionLogger implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StartupVersionLogger.class);

    private final String applicationName;
    private final String version;

    public StartupVersionLogger(
            @Value("${spring.application.name:currency-rate-provider}") String applicationName,
            @Value("${app.version:0.0.1-SNAPSHOT}") String version) {
        this.applicationName = applicationName;
        this.version = version;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("Application started: name={}, version={}", applicationName, version);
    }
}
