package com.miirphys.bodiala.staticdata.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/**
 * Hotels Images master record. RezLive CSV columns: {@code HotelCode, Image}.
 * One row per image, so a surrogate id is used and rows are joined to a hotel on
 * {@code hotelCode}.
 */
@Entity
@Table(name = "hotel_image", indexes = @Index(name = "idx_hotel_image_hotel", columnList = "hotel_code"))
public class HotelImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "hotel_code", nullable = false)
    private Long hotelCode;

    @Column(name = "image", length = 2048)
    private String image;

    protected HotelImage() {
    }

    public HotelImage(Long hotelCode, String image) {
        this.hotelCode = hotelCode;
        this.image = image;
    }

    public Long getId() {
        return id;
    }

    public Long getHotelCode() {
        return hotelCode;
    }

    public void setHotelCode(Long hotelCode) {
        this.hotelCode = hotelCode;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }
}
