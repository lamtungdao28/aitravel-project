package com.travelhub.config;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.cache.*;

@Slf4j
public class ResilientRedisCache extends RedisCache {
    public ResilientRedisCache(String name,RedisCacheWriter writer,RedisCacheConfiguration config) {
        super(name,writer,config);
    }
    // Deferred cache mutations run after commit, outside Spring's CacheErrorHandler.
    @Override public void put(Object key,Object value) { safely(() -> super.put(key,value)); }
    @Override public void evict(Object key) { safely(() -> super.evict(key)); }
    @Override public void clear() { safely(super::clear); }
    private void safely(Runnable action) {
        try {action.run();} catch(RuntimeException ex) {
            log.warn("Cache mutation failed after commit for {}: {}",getName(),ex.getClass().getSimpleName());
        }
    }
}
