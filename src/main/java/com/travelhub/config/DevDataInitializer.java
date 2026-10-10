package com.travelhub.config;

import com.travelhub.address.entity.Address;
import com.travelhub.booking.entity.*;
import com.travelhub.category.entity.Category;
import com.travelhub.destination.entity.Destination;
import com.travelhub.favorite.entity.Favorite;
import com.travelhub.place.entity.*;
import com.travelhub.review.entity.Review;
import com.travelhub.trip.entity.*;
import com.travelhub.user.dto.UserCreateRequest;
import com.travelhub.user.entity.*;

import jakarta.persistence.EntityManager;
import jakarta.validation.Validator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Component
@Profile("dev & !prod")
@ConditionalOnProperty(name = "travelhub.seed.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class DevDataInitializer implements ApplicationRunner {
    private final EntityManager em;
    private final PasswordEncoder encoder;
    private final Validator validator;
    private final Clock clock;

    @Value("${DEV_ADMIN_EMAIL:admin@travelhub.test}")
    private String adminEmail;

    @Value("${DEV_ADMIN_PASSWORD:}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(ApplicationArguments arguments) {
        bootstrapAdmin();
        if (em.createQuery("select count(d) from Destination d", Long.class).getSingleResult() > 0)
            return;
        Map<String, Category> categories = new HashMap<>();
        for (String name :
                List.of(
                        "Restaurant",
                        "Hotel",
                        "Cafe",
                        "Museum",
                        "Beach",
                        "Park",
                        "Shopping",
                        "Nightlife",
                        "Attraction",
                        "Nature")) {
            String slug = name.toLowerCase(Locale.ROOT);
            Category c =
                    em.createQuery("select c from Category c where c.slug=:slug", Category.class)
                            .setParameter("slug", slug)
                            .getResultStream()
                            .findFirst()
                            .orElseGet(
                                    () -> {
                                        Category created = new Category();
                                        created.setName(name);
                                        created.setSlug(slug);
                                        em.persist(created);
                                        return created;
                                    });
            categories.put(name, c);
        }
        Destination hanoi =
                destination("Hanoi", "Vietnam", "Asia/Ho_Chi_Minh", "21.0278", "105.8342");
        Destination danang =
                destination("Da Nang", "Vietnam", "Asia/Ho_Chi_Minh", "16.0544", "108.2022");
        Destination dalat =
                destination("Da Lat", "Vietnam", "Asia/Ho_Chi_Minh", "11.9404", "108.4583");
        Destination hcm =
                destination(
                        "Ho Chi Minh City", "Vietnam", "Asia/Ho_Chi_Minh", "10.8231", "106.6297");
        Destination paris = destination("Paris", "France", "Europe/Paris", "48.8566", "2.3522");
        Destination tokyo = destination("Tokyo", "Japan", "Asia/Tokyo", "35.6762", "139.6503");
        List<Place> places =
                List.of(
                        place(hanoi, "Hoan Kiem Lake", categories.get("Nature"), 0, false),
                        place(
                                hanoi,
                                "Temple of Literature",
                                categories.get("Attraction"),
                                1,
                                false),
                        place(danang, "My Khe Beach", categories.get("Beach"), 0, false),
                        place(dalat, "Da Lat Flower Park", categories.get("Park"), 1, false),
                        place(hcm, "Ben Thanh Market", categories.get("Shopping"), 1, false),
                        place(paris, "Louvre Museum", categories.get("Museum"), 2, false),
                        place(tokyo, "Sensoji Temple", categories.get("Attraction"), 0, false),
                        place(hanoi, "Old Quarter Demo Cafe", categories.get("Cafe"), 1, true));
        Role role = role("USER");
        User first = sampleUser("demo.traveler.one@travelhub.test", "Demo Traveler One", role);
        User second = sampleUser("demo.traveler.two@travelhub.test", "Demo Traveler Two", role);
        for (Place place : places) {
            review(
                    first,
                    place,
                    4,
                    "Development sample: an enjoyable visit with convenient access.");
            review(second, place, 5, "Development sample: a memorable stop on our itinerary.");
            place.setAverageRating(new BigDecimal("4.50"));
            place.setReviewCount(2);
        }
        LocalDate start = LocalDate.now(clock).plusDays(7);
        Trip trip = new Trip();
        trip.setUser(first);
        trip.setDestination(hanoi);
        trip.setName("Hanoi Demo Trip");
        trip.setStartDate(start);
        trip.setEndDate(start.plusDays(2));
        trip.setBudget(new BigDecimal("200.00"));
        em.persist(trip);
        for (int i = 0; i < 2; i++) {
            TripPlace visit = new TripPlace();
            visit.setTrip(trip);
            visit.setPlace(places.get(i));
            visit.setVisitDate(start);
            visit.setSortOrder(i + 1);
            em.persist(visit);
        }
        Favorite favorite = new Favorite();
        favorite.setUser(first);
        favorite.setPlace(places.get(2));
        em.persist(favorite);
        booking(first, places.get(7), start, BookingStatus.PENDING);
        booking(second, places.get(7), start.plusDays(1), BookingStatus.CONFIRMED);
        Address address = new Address();
        address.setUser(first);
        address.setRecipientName("Demo Traveler");
        address.setPhone("00000000");
        address.setAddressLine("Development sample address");
        address.setCountry("Vietnam");
        address.setCity("Hanoi");
        address.setDefaultAddress(true);
        em.persist(address);
        em.flush();
        log.info(
                "Created development travel data; sample accounts have randomly generated,"
                    + " undisclosed passwords");
    }

    private void bootstrapAdmin() {
        if (adminPassword.isBlank()) {
            log.info(
                    "Development admin bootstrap is disabled; configure DEV_ADMIN_PASSWORD to"
                        + " enable it");
            return;
        }
        UserCreateRequest request =
                new UserCreateRequest(adminEmail, adminPassword, "Development Administrator", null);
        if (!validator.validate(request).isEmpty()
                || adminPassword.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
            throw new IllegalStateException("Invalid development administrator configuration");
        String email = adminEmail.trim().toLowerCase(Locale.ROOT);
        if (em.createQuery("select count(u) from User u where u.email=:email", Long.class)
                        .setParameter("email", email)
                        .getSingleResult()
                > 0) return;
        User user = new User();
        user.setEmail(email);
        user.setPassword(encoder.encode(adminPassword));
        user.setFullName(request.fullName());
        user.setRole(role("ADMIN"));
        em.persist(user);
        log.info("Created configured development administrator");
    }

    private Role role(String name) {
        return em.createQuery("select r from Role r where r.name=:name", Role.class)
                .setParameter("name", name)
                .getSingleResult();
    }

    private User sampleUser(String email, String name, Role role) {
        return em.createQuery("select u from User u where u.email=:email", User.class)
                .setParameter("email", email)
                .getResultStream()
                .findFirst()
                .orElseGet(
                        () -> {
                            User u = new User();
                            u.setEmail(email);
                            u.setFullName(name);
                            u.setRole(role);
                            u.setPassword(encoder.encode(UUID.randomUUID().toString()));
                            em.persist(u);
                            return u;
                        });
    }

    private Destination destination(
            String name, String country, String timezone, String latitude, String longitude) {
        Destination d = new Destination();
        d.setName(name);
        d.setCountry(country);
        d.setTimezone(timezone);
        d.setLatitude(new BigDecimal(latitude));
        d.setLongitude(new BigDecimal(longitude));
        d.setDescription("Development sample destination: " + name);
        em.persist(d);
        return d;
    }

    private Place place(
            Destination destination, String name, Category category, int price, boolean bookable) {
        Place p = new Place();
        p.setName(name);
        p.setDestination(destination);
        p.setPriceLevel(price);
        p.setBookingEnabled(bookable);
        p.setDescription(
                "Development sample listing. Availability, prices and reviews are illustrative.");
        em.persist(p);
        PlaceCategory link = new PlaceCategory();
        link.setPlace(p);
        link.setCategory(category);
        em.persist(link);
        return p;
    }

    private void review(User user, Place place, int rating, String content) {
        Review r = new Review();
        r.setUser(user);
        r.setPlace(place);
        r.setRating(rating);
        r.setContent(content);
        r.setTitle("Demo review");
        em.persist(r);
    }

    private void booking(User user, Place place, LocalDate date, BookingStatus status) {
        Booking b = new Booking();
        b.setUser(user);
        b.setPlace(place);
        b.setBookingDate(date);
        b.setBookingTime(LocalTime.of(19, 0));
        b.setNumberOfPeople(2);
        b.setStatus(status);
        b.setNote("Development sample booking");
        em.persist(b);
    }
}
