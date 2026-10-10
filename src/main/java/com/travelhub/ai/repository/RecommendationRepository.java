package com.travelhub.ai.repository;
import com.travelhub.place.entity.Place;
import jakarta.persistence.EntityManager;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository @RequiredArgsConstructor
public class RecommendationRepository {
    private final EntityManager em;
    public List<Place> candidates(Long userId,Long destinationId,int limit) {
        return em.createQuery("""
                select p from Place p
                where (:destinationId is null or p.destination.id=:destinationId)
                and not exists(select f from Favorite f where f.user.id=:userId and f.place=p)
                order by
                (case when exists(select t from Trip t where t.user.id=:userId and t.destination=p.destination) then 3 else 0 end
                + case when exists(select pc from PlaceCategory pc where pc.place=p and
                    exists(select liked from PlaceCategory liked where liked.category=pc.category and
                        (exists(select f from Favorite f where f.user.id=:userId and f.place=liked.place)
                        or exists(select r from Review r where r.user.id=:userId and r.rating>=4 and r.place=liked.place)))) then 2 else 0 end) desc,
                p.averageRating desc,p.reviewCount desc,p.id asc
                """,Place.class).setParameter("userId",userId).setParameter("destinationId",destinationId)
                .setMaxResults(limit).getResultList();
    }
}
