package com.miirphys.bodiala.staticdata.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/**
 * City master record. RezLive CSV columns: {@code City, Name, CountryCode}.
 *
 * <p>{@code City} is documented as a String holding a unique 3-5 character numeric code
 * (e.g. "968" = Dubai) and is the geo key passed as {@code <City>} in searches — hence it
 * is kept as a String, not an int.
 */
@Entity
@Table(name = "city", indexes = @Index(name = "idx_city_country", columnList = "country_code"))
public class City {

    @Id
    @Column(name = "city_code", nullable = false)
    private String cityCode;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "country_code", length = 2)
    private String countryCode;

    protected City() {
    }

    public City(String cityCode, String name, String countryCode) {
        this.cityCode = cityCode;
        this.name = name;
        this.countryCode = countryCode;
    }

    public String getCityCode() {
        return cityCode;
    }

    public void setCityCode(String cityCode) {
        this.cityCode = cityCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }
}
