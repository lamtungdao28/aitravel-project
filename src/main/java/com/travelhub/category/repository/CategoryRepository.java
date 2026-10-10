package com.travelhub.category.repository;

import com.travelhub.category.entity.Category;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    boolean existsBySlugAndIdNot(String slug, Long id);

    @Query("select count(pc) from PlaceCategory pc where pc.category.id = :id")
    long countAssignments(Long id);
}
