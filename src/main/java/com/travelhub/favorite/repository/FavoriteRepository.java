package com.travelhub.favorite.repository;

import com.travelhub.favorite.entity.Favorite;
import com.travelhub.favorite.entity.FavoriteId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FavoriteRepository extends JpaRepository<Favorite, FavoriteId> {
    @EntityGraph(attributePaths = "place")
    Page<Favorite> findByUserId(Long userId, Pageable pageable);
}
