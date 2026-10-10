package com.travelhub.admin.repository;

import jakarta.persistence.EntityManager;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.*;

@Repository
@RequiredArgsConstructor
public class AdminStatisticsRepository {
    private final EntityManager em;

    public long newUsersSince(Instant since) {
        return em.createQuery("select count(u) from User u where u.createdAt >= :since", Long.class)
                .setParameter("since", since)
                .getSingleResult();
    }

    public Map<String, Long> usersByRole() {
        Map<String, Long> result = new LinkedHashMap<>();
        em.createQuery(
                        "select coalesce(r.name,'UNASSIGNED'),count(u) from User u left join u.role"
                            + " r group by r.name",
                        Object[].class)
                .getResultList()
                .forEach(row -> result.put((String) row[0], (Long) row[1]));
        return result;
    }

    public long bookablePlaces() {
        return em.createQuery(
                        "select count(p) from Place p where p.bookingEnabled=true", Long.class)
                .getSingleResult();
    }

    public Double averageRating() {
        return em.createQuery("select avg(r.rating) from Review r", Double.class).getSingleResult();
    }
}
