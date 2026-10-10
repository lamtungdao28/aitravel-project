package com.travelhub.place.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class PlaceCategoryId implements Serializable {
    private static final long serialVersionUID = 1L;

    @Column(name = "place_id")
    private Long placeId;

    @Column(name = "category_id")
    private Long categoryId;
}
