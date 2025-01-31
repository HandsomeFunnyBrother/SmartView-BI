package com.yupi.springbootinit.config;

import com.volcengine.ark.runtime.service.ArkService;
import lombok.Data;
import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.concurrent.TimeUnit;

@Configuration
@ConfigurationProperties(prefix = "ark")
@Data
public class ArkConfig {
    private String apiKey;
    private String baseUrl;

    @Bean
    public ConnectionPool connectionPool() {
        return new ConnectionPool(5, 1, TimeUnit.SECONDS);
    }

    @Bean
    public Dispatcher dispatcher() {
        return new Dispatcher();
    }

    @Bean
    public ArkService arkService(ConnectionPool connectionPool, Dispatcher dispatcher) {
        return ArkService.builder()
               .dispatcher(dispatcher)
               .connectionPool(connectionPool)
               .baseUrl(baseUrl)
               .apiKey(apiKey)
               .build();
    }
}