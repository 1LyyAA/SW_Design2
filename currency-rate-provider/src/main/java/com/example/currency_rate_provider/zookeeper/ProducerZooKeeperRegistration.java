package com.example.currency_rate_provider.zookeeper;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.CreateMode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.net.InetAddress;

@Component
@RequiredArgsConstructor
public class ProducerZooKeeperRegistration {

    private final CuratorFramework client;

    @Value("${zookeeper.service-path}")
    private String servicePath;

    @Value("${spring.grpc.server.port}")
    private int grpcPort;

    @EventListener(ApplicationReadyEvent.class)
    public void register() throws Exception {
        String host = InetAddress.getLocalHost().getHostAddress();
        String address = host + ":" + grpcPort;

        String nodePath = servicePath + "/" + address;

        // if (client.checkExists().forPath(nodePath) != null) {
        //     client.delete().forPath(nodePath);
        // }

        client.create()
                .creatingParentsIfNeeded()
                .withMode(CreateMode.EPHEMERAL)
                .forPath(nodePath, address.getBytes(StandardCharsets.UTF_8));

        System.out.println("Registered producer in ZooKeeper: " + nodePath);
    }
}