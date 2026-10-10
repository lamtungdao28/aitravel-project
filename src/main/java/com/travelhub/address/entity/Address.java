package com.travelhub.address.entity;

import com.travelhub.common.entity.BaseEntity;
import com.travelhub.user.entity.User;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.hibernate.annotations.ColumnDefault;

@Entity
@Table(name = "addresses")
@Getter
@Setter
@NoArgsConstructor
public class Address extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "recipient_name", nullable = false, length = 150)
    private String recipientName;

    @Column(nullable = false, length = 40)
    private String phone;

    @Column(name = "address_line", nullable = false, length = 500)
    private String addressLine;

    @Column(length = 120)
    private String city;

    @Column(length = 120)
    private String state;

    @Column(length = 120)
    private String country;

    @Column(name = "postal_code", length = 30)
    private String postalCode;

    @Column(name = "is_default", nullable = false)
    @ColumnDefault("false")
    private boolean defaultAddress;
}
