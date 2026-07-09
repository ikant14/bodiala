package com.miirphys.bodiala.staticdata.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Country master record. RezLive CSV columns: {@code Name, CountryCode}.
 * {@code CountryCode} is the unique 2-letter ISO code (e.g. "AE").
 */
@Entity
@Table(name = "country")
public class Country {

    @Id
    @Column(name = "country_code", length = 2, nullable = false)
    private String countryCode;

    @Column(name = "name", nullable = false)
    private String name;

    protected Country() {
    }

    public Country(String countryCode, String name) {
        this.countryCode = countryCode;
        this.name = name;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
