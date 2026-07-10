package com.miirphys.bodiala.staticdata.repo;

import com.miirphys.bodiala.staticdata.domain.Hotel;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface HotelRepository extends JpaRepository<Hotel, Long> {

    Page<Hotel> findByCityCode(String cityCode, Pageable pageable);

    Page<Hotel> findByCountryCode(String countryCode, Pageable pageable);

    /** Cached hotel codes for the given destination codes — used to fan a multi-city search into one
     * availability call by hotel ids. */
    @Query("select h.hotelCode from Hotel h where h.cityCode in :cityCodes")
    List<Long> findHotelCodesByCityCodeIn(Collection<String> cityCodes);
}
