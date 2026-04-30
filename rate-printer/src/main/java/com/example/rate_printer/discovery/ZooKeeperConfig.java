package com.example.rate_printer.discovery;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;

@Configuration
public class ZooKeeperConfig {

    @Bean(destroyMethod = "close")
    public CuratorFramework curatorFramework() {
        CuratorFramework client = CuratorFrameworkFactory.newClient(
                "localhost:2181",
                new ExponentialBackoffRetry(1000, 3)
        );
        client.start();
        return client;
    }
}