package com.travelhub.destination;

import com.travelhub.PostgresIntegrationTest;
import com.travelhub.destination.entity.Destination;
import com.travelhub.destination.repository.DestinationRepository;
import com.travelhub.destination.repository.DestinationSpecification;
import com.travelhub.place.entity.Place;
import com.travelhub.trip.entity.Trip;
import com.travelhub.user.entity.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class DestinationRepositoryTest extends PostgresIntegrationTest {
    @Autowired DestinationRepository repository;
    @Autowired EntityManager entityManager;

    @Test
    void specificationCombinesFiltersAndPaginates() {
        createDestination("Da Nang", "Vietnam");
        createDestination("Da Lat", "Vietnam");
        createDestination("Dallas", "United States");
        var result = repository.findAll(DestinationSpecification.search("DA", "VIETNAM"),
                PageRequest.of(0, 1, Sort.by("name")));
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getTotalPages()).isEqualTo(2);
        assertThat(result.getContent()).extracting(Destination::getName).containsExactly("Da Lat");
    }

    @Test
    void specificationTreatsWildcardsAsLiteralCharacters() {
        createDestination("Hanoi", "Vietnam");
        createDestination("100% Amazing", "Vietnam");
        createDestination("Under_score", "Vietnam");
        assertThat(repository.findAll(DestinationSpecification.search("%", null)))
                .extracting(Destination::getName).containsExactly("100% Amazing");
        assertThat(repository.findAll(DestinationSpecification.search("_", null)))
                .extracting(Destination::getName).containsExactly("Under_score");
    }

    @Test
    void deletionGuardQueriesCountOnlyReferencingRows() {
        Destination destination = createDestination("Hanoi", "Vietnam");
        Destination otherDestination = createDestination("Paris", "France");
        Place place = new Place();
        place.setName("Lake");
        place.setDestination(destination);
        entityManager.persist(place);
        User user = new User();
        user.setEmail("destination-repository@example.test");
        user.setPassword("test-value");
        user.setFullName("Test User");
        entityManager.persist(user);
        Trip trip = new Trip();
        trip.setName("Holiday");
        trip.setUser(user);
        trip.setDestination(destination);
        entityManager.persist(trip);
        entityManager.flush();
        assertThat(repository.countPlaces(destination.getId())).isEqualTo(1);
        assertThat(repository.countTrips(destination.getId())).isEqualTo(1);
        assertThat(repository.countPlaces(otherDestination.getId())).isZero();
        assertThat(repository.countTrips(otherDestination.getId())).isZero();
    }

    private Destination createDestination(String name, String country) {
        Destination destination = new Destination();
        destination.setName(name);
        destination.setCountry(country);
        return repository.saveAndFlush(destination);
    }
}
