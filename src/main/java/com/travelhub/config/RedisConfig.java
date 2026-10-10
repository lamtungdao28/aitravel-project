package com.travelhub.config;

import com.fasterxml.jackson.databind.*;
import com.travelhub.common.response.PageResponse;
import com.travelhub.destination.dto.DestinationResponse;
import com.travelhub.place.dto.*;
import java.time.Duration;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.*;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.*;
import org.springframework.data.redis.cache.*;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.*;

@Configuration @EnableCaching @RequiredArgsConstructor @Slf4j
@ConditionalOnProperty(name="travelhub.cache.enabled",havingValue="true",matchIfMissing=true)
public class RedisConfig implements CachingConfigurer {
    private final ObjectMapper mapper;
    @Bean
    RedisCacheManager cacheManager(RedisConnectionFactory connection,
            @Value("${travelhub.cache.destination-ttl:30m}") Duration destinationTtl,
            @Value("${travelhub.cache.place-ttl:10m}") Duration placeTtl,
            @Value("${travelhub.cache.ranking-ttl:5m}") Duration rankingTtl,
            @Value("${travelhub.cache.summary-ttl:30m}") Duration summaryTtl) {
        Map<String,RedisCacheConfiguration> configs=new HashMap<>();
        configs.put("destinationDetails",config(mapper.constructType(DestinationResponse.class),destinationTtl));
        configs.put("destinationLists",config(mapper.getTypeFactory().constructParametricType(PageResponse.class,DestinationResponse.class),destinationTtl));
        configs.put("placeDetails",config(mapper.constructType(PlaceDetailResponse.class),placeTtl));
        JavaType ranking=mapper.getTypeFactory().constructParametricType(PageResponse.class,PlaceSummaryResponse.class);
        configs.put("popularPlaces",config(ranking,rankingTtl));
        configs.put("topRatedPlaces",config(ranking,rankingTtl));
        configs.put("reviewSummaries",config(mapper.constructType(com.travelhub.ai.dto.ReviewSummaryResponse.class),summaryTtl));
        RedisCacheWriter writer=RedisCacheWriter.nonLockingRedisCacheWriter(connection);
        RedisCacheManager manager=new RedisCacheManager(writer,RedisCacheConfiguration.defaultCacheConfig(),false,configs) {
            @Override protected RedisCache createRedisCache(String name,RedisCacheConfiguration config) {
                return new ResilientRedisCache(name,writer,config);
            }
        };
        manager.setTransactionAware(true);
        return manager;
    }
    public RedisCacheConfiguration config(JavaType type,Duration ttl) {
        if(ttl.isZero() || ttl.isNegative()) throw new IllegalArgumentException("Cache TTL must be positive");
        return RedisCacheConfiguration.defaultCacheConfig().prefixCacheNameWith("travelhub:")
                .entryTtl(ttl).disableCachingNullValues().serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new Jackson2JsonRedisSerializer<>(mapper,type)));
    }
    @Override public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            private void failure(Cache cache,RuntimeException ex) {log.warn("Cache {} failed: {}",cache.getName(),ex.getClass().getSimpleName());}
            public void handleCacheGetError(RuntimeException ex,Cache c,Object key){failure(c,ex);}
            public void handleCachePutError(RuntimeException ex,Cache c,Object key,Object value){failure(c,ex);}
            public void handleCacheEvictError(RuntimeException ex,Cache c,Object key){failure(c,ex);}
            public void handleCacheClearError(RuntimeException ex,Cache c){failure(c,ex);}
        };
    }
}
