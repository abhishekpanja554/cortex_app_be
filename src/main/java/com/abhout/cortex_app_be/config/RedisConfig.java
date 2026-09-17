package com.abhout.cortex_app_be.config;

import org.springframework.boot.cache.autoconfigure.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.jsontype.PolymorphicTypeValidator;

import java.time.Duration;
import java.util.Set;

@Configuration
@EnableCaching
public class RedisConfig {

    @Bean
    RedisCacheConfiguration cacheConfiguration() {
        PolymorphicTypeValidator typeValidator = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("com.abhout.cortex_app_be.note.dtos.")
                .allowIfSubType("com.abhout.cortex_app_be.search.dtos.")
                .allowIfSubType("java.util.")
                .build();
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(GenericJacksonJsonRedisSerializer.builder()
                                .enableDefaultTyping(typeValidator)
                                .build()))
                .disableCachingNullValues();
    }

    @Bean
    RedisCacheManagerBuilderCustomizer cacheManagerBuilderCustomizer() {
        return builder -> builder.initialCacheNames(Set.of("notes")).enableStatistics();
    }
}
