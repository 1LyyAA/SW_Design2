package com.example.currency_rate_provider.zookeeper;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ZooKeeperConfig {

    @Bean(destroyMethod = "close")
    public CuratorFramework curatorFramework(
            @Value("${zookeeper.connect-string}") String connectString
    ) {
        CuratorFramework client = CuratorFrameworkFactory.newClient(
                connectString,
                new ExponentialBackoffRetry(1000, 3)
        );

        client.start();
        return client;
    }
}