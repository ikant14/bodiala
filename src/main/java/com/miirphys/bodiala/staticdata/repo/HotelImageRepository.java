package com.miirphys.bodiala.staticdata.repo;

import com.miirphys.bodiala.staticdata.domain.HotelImage;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HotelImageRepository extends JpaRepository<HotelImage, Long> {

    /** One hotel's images in stored order — the importer saves them display-order, cover first. */
    List<HotelImage> findByHotelCodeOrderByIdAsc(Long hotelCode);

    /**
     * The cover image (the first one stored) of each given hotel, in one query. Hotels with no cached
     * images are simply absent from the result.
     */
    @Query("select i from HotelImage i where i.id in "
            + "(select min(j.id) from HotelImage j where j.hotelCode in :codes group by j.hotelCode)")
    List<HotelImage> findCoverImages(@Param("codes") Collection<Long> codes);

    /** Remove one hotel's images before re-inserting them (on-demand single-hotel content refresh). */
    void deleteByHotelCode(Long hotelCode);
}
