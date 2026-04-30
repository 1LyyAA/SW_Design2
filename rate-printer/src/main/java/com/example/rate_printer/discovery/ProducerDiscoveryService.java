package com.example.rate_printer.discovery;

import lombok.RequiredArgsConstructor;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.data.Stat;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProducerDiscoveryService {

    private final CuratorFramework client;

    private static final String SERVICE_PATH = "/services";

    public List<String> getProducerInstances() {
        try {
            List<String> instances = new ArrayList<>();

            // получаем список нод
            List<String> children = client.getChildren().forPath(SERVICE_PATH);

            for (String child : children) {
                String fullPath = SERVICE_PATH + "/" + child;

                // читаем данные (host:port)
                byte[] data = client.getData().forPath(fullPath);

                String address = new String(data, StandardCharsets.UTF_8);
                instances.add(address);
            }

            return instances;

        } catch (Exception e) {
            throw new RuntimeException("Failed to discover producers", e);
        }
    }
}