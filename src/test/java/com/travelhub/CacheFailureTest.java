package com.travelhub;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.travelhub.config.ResilientRedisCache;

import org.junit.jupiter.api.Test;
import org.springframework.cache.transaction.TransactionAwareCacheDecorator;
import org.springframework.data.redis.cache.*;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class CacheFailureTest {
    @Test
    void deferredInvalidationFailureDoesNotFailAnAlreadyCommittedWrite() {
        RedisCacheWriter writer = mock(RedisCacheWriter.class);
        doThrow(new IllegalStateException("offline"))
                .when(writer)
                .clean(anyString(), any(byte[].class));
        var cache =
                new TransactionAwareCacheDecorator(
                        new ResilientRedisCache(
                                "test", writer, RedisCacheConfiguration.defaultCacheConfig()));
        TransactionSynchronizationManager.initSynchronization();
        try {
            cache.clear();
            verifyNoInteractions(writer);
            assertThatCode(
                            () ->
                                    TransactionSynchronizationManager.getSynchronizations()
                                            .forEach(s -> s.afterCommit()))
                    .doesNotThrowAnyException();
            verify(writer).clean(eq("test"), any(byte[].class));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }
}
