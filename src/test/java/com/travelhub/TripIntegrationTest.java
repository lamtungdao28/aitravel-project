package com.travelhub;
import com.travelhub.common.exception.*;
import com.travelhub.destination.entity.Destination;
import com.travelhub.place.entity.Place;
import com.travelhub.trip.dto.*;
import com.travelhub.trip.entity.TripPlace;
import com.travelhub.trip.entity.TripPlaceId;
import com.travelhub.trip.service.TripService;
import com.travelhub.user.entity.User;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.*;

@Transactional
class TripIntegrationTest extends PostgresIntegrationTest {
    @Autowired TripService trips;
    @Autowired EntityManager em;
    @Test void itineraryDatesOrderingAndDeletionAreConsistent() {
        User user=user();
        Place a=place("Alpha"), b=place("Beta");
        LocalDate start=LocalDate.now().plusDays(5);
        var trip=trips.create(new TripCreateRequest(user.getId(),null,"Trip",start,start.plusDays(2),BigDecimal.TEN,null));
        trips.addPlace(trip.id(),new TripPlaceRequest(a.getId(),start.plusDays(1),null,0));
        trips.addPlace(trip.id(),new TripPlaceRequest(b.getId(),start,null,1));
        assertThat(trips.listPlaces(trip.id(),PageRequest.of(0,20)).content().getFirst().placeId()).isEqualTo(b.getId());
        assertThatThrownBy(() -> trips.addPlace(trip.id(),new TripPlaceRequest(a.getId(),start,null,0)))
                .isInstanceOf(DuplicateResourceException.class);
        assertThatThrownBy(() -> trips.update(trip.id(),new TripUpdateRequest(null,"Trip",start,start,BigDecimal.TEN,null)))
                .isInstanceOf(BusinessException.class);
        trips.updatePlace(trip.id(),a.getId(),new TripPlaceRequest(a.getId(),start,"Updated",2));
        trips.update(trip.id(),new TripUpdateRequest(null,"Trip",start,start,BigDecimal.TEN,null));
        trips.removePlace(trip.id(),b.getId());
        trips.delete(trip.id()); em.flush(); em.clear();
        assertThat(em.find(TripPlace.class,new TripPlaceId(trip.id(),a.getId()))).isNull();
        assertThat(em.find(Place.class,a.getId())).isNotNull();
    }
    @Test void rejectsInvalidTripAndVisitDates() {
        User user=user(); Place p=place("Lake"); LocalDate today=LocalDate.now();
        assertThatThrownBy(() -> trips.create(new TripCreateRequest(user.getId(),null,"Trip",today,today.minusDays(1),null,null)))
                .isInstanceOf(BusinessException.class);
        var t=trips.create(new TripCreateRequest(user.getId(),null,"Trip",today,today.plusDays(1),null,null));
        assertThatThrownBy(() -> trips.addPlace(t.id(),new TripPlaceRequest(p.getId(),today.plusDays(2),null,0)))
                .isInstanceOf(BusinessException.class);
        assertThat(trips.list(user.getId(),PageRequest.of(0,20)).totalElements()).isEqualTo(1);
    }
    private User user() {
        User u=new User();u.setEmail("trip@example.test");u.setFullName("Trip User");u.setPassword("test-only");em.persist(u);return u;
    }
    private Place place(String name) {
        Destination d=new Destination();d.setName("Hanoi");d.setCountry("Vietnam");em.persist(d);
        Place p=new Place();p.setName(name);p.setDestination(d);em.persist(p);return p;
    }
}
