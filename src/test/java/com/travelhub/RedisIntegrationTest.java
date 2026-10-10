package com.travelhub;
import com.travelhub.destination.dto.*;
import com.travelhub.destination.service.DestinationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.TestPropertySource;
import static org.assertj.core.api.Assertions.*;
@TestPropertySource(properties="travelhub.cache.enabled=true")
class RedisIntegrationTest extends PostgresIntegrationTest {
    @Autowired DestinationService destinations;
    @Autowired CacheManager caches;
    @Test void roundTripsTypedJsonAndEvictsAfterCommittedMutation() {
        var created=destinations.create(new DestinationCreateRequest("Cached","Vietnam",null,null,null,null,null));
        try {
            assertThat(destinations.get(created.id()).name()).isEqualTo("Cached");
            assertThat(caches.getCache("destinationDetails").get(created.id(),DestinationResponse.class).name()).isEqualTo("Cached");
            destinations.update(created.id(),new DestinationUpdateRequest("Updated","Vietnam",null,null,null,null,null));
            assertThat(caches.getCache("destinationDetails").get(created.id())).isNull();
            assertThat(destinations.get(created.id()).name()).isEqualTo("Updated");
            assertThat(destinations.get(created.id()).name()).isEqualTo("Updated");
        } finally { destinations.delete(created.id()); }
    }
}
