package com.miirphys.bodiala.staticdata.repo;

import com.miirphys.bodiala.staticdata.domain.Country;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CountryRepository extends JpaRepository<Country, String> {
}
