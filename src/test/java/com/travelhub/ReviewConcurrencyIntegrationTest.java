package com.travelhub;

import com.travelhub.destination.entity.Destination;
import com.travelhub.place.entity.Place;
import com.travelhub.review.dto.ReviewCreateRequest;
import com.travelhub.review.entity.Review;
import com.travelhub.review.service.ReviewService;
import com.travelhub.user.entity.User;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class ReviewConcurrencyIntegrationTest extends PostgresIntegrationTest {
    @Autowired ReviewService reviews;
    @Autowired EntityManager em;
    @Autowired PlatformTransactionManager transactionManager;

    @Test void concurrentReviewsKeepTheAggregateConsistent() throws Exception {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        Long[] ids = tx.execute(status -> {
            User user = new User(); user.setEmail(UUID.randomUUID() + "@example.test");
            user.setPassword("concurrency-test-only"); user.setFullName("Concurrency test"); em.persist(user);
            Destination destination = new Destination(); destination.setName("Concurrent"); destination.setCountry("Vietnam"); em.persist(destination);
            Place place = new Place(); place.setName("Concurrent Place"); place.setDestination(destination); em.persist(place);
            return new Long[]{user.getId(), place.getId(), destination.getId()};
        });
        try {
            CountDownLatch start = new CountDownLatch(1);
            try (var delegate = Executors.newFixedThreadPool(4)) {
                var executor = new org.springframework.security.concurrent.DelegatingSecurityContextExecutorService(delegate);
                List<Callable<Void>> calls = List.of(1, 2, 4, 5).stream().<Callable<Void>>map(rating -> () -> {
                    start.await(10, TimeUnit.SECONDS);
                    reviews.create(ids[1], new ReviewCreateRequest(ids[0], rating, "Concurrent", "Concurrent review", List.of()));
                    return null;
                }).toList();
                var tasks = calls.stream().map(executor::submit).toList();
                start.countDown();
                for (var task : tasks) task.get(20, TimeUnit.SECONDS);
            }
            tx.executeWithoutResult(status -> {
                Place place = em.find(Place.class, ids[1]);
                assertThat(place.getReviewCount()).isEqualTo(4);
                assertThat(place.getAverageRating()).isEqualByComparingTo("3.00");
            });
        } finally {
            tx.executeWithoutResult(status -> {
                em.createQuery("select r from Review r where r.place.id = :id", Review.class)
                        .setParameter("id", ids[1]).getResultList().forEach(em::remove);
                em.flush();
                em.remove(em.find(Place.class, ids[1])); em.flush();
                em.remove(em.find(Destination.class, ids[2]));
                em.remove(em.find(User.class, ids[0]));
            });
        }
    }
}
