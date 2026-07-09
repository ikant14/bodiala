package com.miirphys.bodiala.staticdata.repo;

import com.miirphys.bodiala.staticdata.domain.HotelImage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HotelImageRepository extends JpaRepository<HotelImage, Long> {

    List<HotelImage> findByHotelCode(Long hotelCode);

    /** Remove one hotel's images before re-inserting them (on-demand single-hotel content refresh). */
    void deleteByHotelCode(Long hotelCode);
}
