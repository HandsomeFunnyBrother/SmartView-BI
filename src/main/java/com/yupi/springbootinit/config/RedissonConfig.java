package com.yupi.springbootinit.config;

import lombok.Data;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "spring.redis")
public class RedissonConfig {
    private Integer database;
    private String host;
    private Integer port;
    private String password;

    /**
     * 创建RedissonClient对象
     * spring启动时会自动创建RedissonClient对象
     * @return
     */
    @Bean
    public RedissonClient getRedissonClient() {
        //创建配置对象
        Config config = new Config();
        //设置配置
        config.useSingleServer()
                .setDatabase(database)
                .setPassword(password)
                .setAddress("redis://" + host + ":" + port);

        //创建Redisson实例
        RedissonClient redisson = Redisson.create(config);
        return redisson;
    }

}
