package com.example.rate_printer.discovery;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Component
@RequiredArgsConstructor
public class InstanceSelector {

    private final ProducerDiscoveryService discoveryService;

    public String selectInstance() {
        List<String> instances = discoveryService.getProducerInstances();

        if (instances.isEmpty()) {
            throw new IllegalStateException("No available producer instances");
        }

        int index = ThreadLocalRandom.current().nextInt(instances.size());
        return instances.get(index);
    }
}