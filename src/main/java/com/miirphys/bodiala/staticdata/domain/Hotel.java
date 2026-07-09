package com.miirphys.bodiala.staticdata.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

/**
 * Hotel Details master record — the shared cache row for a hotel: code, name, city/country codes,
 * rating, address, postal code, coordinates and description.
 *
 * <p>{@code hotelCode} is stored as {@code Long} for headroom.
 */
@Entity
@Table(name = "hotel", indexes = {
        @Index(name = "idx_hotel_city", columnList = "city_code"),
        @Index(name = "idx_hotel_country", columnList = "country_code")
})
public class Hotel {

    @Id
    @Column(name = "hotel_code", nullable = false)
    private Long hotelCode;

    @Column(name = "name")
    private String name;

    @Column(name = "city_code")
    private String cityCode;

    @Column(name = "country_code", length = 2)
    private String countryCode;

    @Column(name = "rating")
    private Integer rating;

    @Column(name = "hotel_address", length = 1024)
    private String hotelAddress;

    /** Hotel postal / ZIP code. */
    @Column(name = "hotel_postal_code")
    private String hotelPostalCode;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Lob
    @Column(name = "description")
    private String description;

    public Hotel() {
    }

    public Long getHotelCode() {
        return hotelCode;
    }

    public void setHotelCode(Long hotelCode) {
        this.hotelCode = hotelCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCityCode() {
        return cityCode;
    }

    public void setCityCode(String cityCode) {
        this.cityCode = cityCode;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getHotelAddress() {
        return hotelAddress;
    }

    public void setHotelAddress(String hotelAddress) {
        this.hotelAddress = hotelAddress;
    }

    public String getHotelPostalCode() {
        return hotelPostalCode;
    }

    public void setHotelPostalCode(String hotelPostalCode) {
        this.hotelPostalCode = hotelPostalCode;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
