package com.miirphys.bodiala.staticdata.repo;

import com.miirphys.bodiala.staticdata.domain.Hotel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HotelRepository extends JpaRepository<Hotel, Long> {

    Page<Hotel> findByCityCode(String cityCode, Pageable pageable);

    Page<Hotel> findByCountryCode(String countryCode, Pageable pageable);
}
