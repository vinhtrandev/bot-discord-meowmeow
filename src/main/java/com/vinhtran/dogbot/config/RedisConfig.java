package com.vinhtran.dogbot.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.net.URI;

@Configuration
public class RedisConfig {

    @Bean
    public JedisPool jedisPool(@Value("${redis.url}") String redisUrl) {
        return new JedisPool(new JedisPoolConfig(), URI.create(redisUrl));
    }
}