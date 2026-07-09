package com.miirphys.bodiala.staticdata.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

/**
 * Property Amenities master record. RezLive CSV columns: {@code HotelCode, HotelAmenities}.
 * Documented as one row per hotel with a list of amenities, so {@code hotelCode} is the key.
 */
@Entity
@Table(name = "property_amenity")
public class PropertyAmenity {

    @Id
    @Column(name = "hotel_code", nullable = false)
    private Long hotelCode;

    @Lob
    @Column(name = "hotel_amenities")
    private String hotelAmenities;

    protected PropertyAmenity() {
    }

    public PropertyAmenity(Long hotelCode, String hotelAmenities) {
        this.hotelCode = hotelCode;
        this.hotelAmenities = hotelAmenities;
    }

    public Long getHotelCode() {
        return hotelCode;
    }

    public void setHotelCode(Long hotelCode) {
        this.hotelCode = hotelCode;
    }

    public String getHotelAmenities() {
        return hotelAmenities;
    }

    public void setHotelAmenities(String hotelAmenities) {
        this.hotelAmenities = hotelAmenities;
    }
}
