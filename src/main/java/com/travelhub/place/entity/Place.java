package com.travelhub.place.entity;

import com.travelhub.common.entity.BaseEntity;
import com.travelhub.destination.entity.Destination;
import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Table(name = "places")
@Check(constraints = "price_level between 0 and 4 and average_rating between 0 and 5 and review_count >= 0")
@Getter
@Setter
@NoArgsConstructor
public class Place extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "destination_id", nullable = false)
    private Destination destination;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Column(length = 500)
    private String address;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "price_level", nullable = false)
    @ColumnDefault("0")
    private int priceLevel;

    @Column(name = "average_rating", nullable = false, precision = 3, scale = 2)
    @ColumnDefault("0")
    private BigDecimal averageRating = BigDecimal.ZERO;

    @Column(name = "review_count", nullable = false)
    @ColumnDefault("0")
    private int reviewCount;

    @Column(name = "booking_enabled", nullable = false)
    @ColumnDefault("false")
    private boolean bookingEnabled;
}
