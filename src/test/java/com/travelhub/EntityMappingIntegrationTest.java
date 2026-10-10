package com.travelhub;

import com.travelhub.user.entity.*;
import com.travelhub.destination.entity.Destination;
import com.travelhub.place.entity.*;
import com.travelhub.category.entity.Category;
import com.travelhub.favorite.entity.*;
import com.travelhub.trip.entity.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class EntityMappingIntegrationTest extends PostgresIntegrationTest {
    @Autowired EntityManager em;

    @Test void explicitJoinEntitiesPersistWithCompositeKeysAndAuditing() {
        Role role = new Role(); role.setName("MAPPING_TEST"); em.persist(role);
        Permission permission = new Permission(); permission.setName("MAPPING_READ"); em.persist(permission);
        RolePermission rp = new RolePermission();
        rp.setId(new RolePermissionId(role.getId(), permission.getId()));
        rp.setRole(role); rp.setPermission(permission); em.persist(rp);

        User user = new User(); user.setEmail("mapping@example.test"); user.setPassword("phase-two-test-only");
        user.setFullName("Mapping Test"); user.setRole(role); em.persist(user);
        Destination destination = new Destination(); destination.setName("Hanoi"); destination.setCountry("Vietnam"); em.persist(destination);
        Place place = new Place(); place.setName("Lake"); place.setDestination(destination); em.persist(place);
        Category category = new Category(); category.setName("Nature"); category.setSlug("nature-mapping"); em.persist(category);
        PlaceCategory pc = new PlaceCategory(); pc.setId(new PlaceCategoryId(place.getId(), category.getId()));
        pc.setPlace(place); pc.setCategory(category); em.persist(pc);
        Favorite favorite = new Favorite(); favorite.setId(new FavoriteId(user.getId(), place.getId()));
        favorite.setUser(user); favorite.setPlace(place); em.persist(favorite);
        Trip trip = new Trip(); trip.setName("Hanoi Trip"); trip.setUser(user); trip.setDestination(destination); em.persist(trip);
        TripPlace tp = new TripPlace(); tp.setId(new TripPlaceId(trip.getId(), place.getId()));
        tp.setTrip(trip); tp.setPlace(place); tp.setSortOrder(1); em.persist(tp);
        em.flush(); em.clear();

        assertThat(em.find(PlaceCategory.class, pc.getId()).getCategory().getName()).isEqualTo("Nature");
        assertThat(em.find(Favorite.class, favorite.getId()).getCreatedAt()).isNotNull();
        assertThat(em.find(TripPlace.class, tp.getId()).getTrip().getName()).isEqualTo("Hanoi Trip");
        assertThat(em.find(RolePermission.class, rp.getId()).getPermission().getName()).isEqualTo("MAPPING_READ");
        assertThat(em.find(User.class, user.getId()).getCreatedAt()).isNotNull();
    }
}
