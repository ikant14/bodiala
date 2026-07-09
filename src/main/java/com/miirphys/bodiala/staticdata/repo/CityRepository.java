package com.miirphys.bodiala.staticdata.repo;

import com.miirphys.bodiala.staticdata.domain.City;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CityRepository extends JpaRepository<City, String> {

    List<City> findByCountryCode(String countryCode);
}
