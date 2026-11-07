package com.miirphys.bodiala.staticdata.repo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.miirphys.bodiala.staticdata.domain.HotelImage;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/** Runs the image queries against real Postgres (isolated {@code bodiala_test} schema; rolled back). */
@SpringBootTest
@Transactional
class HotelImageRepositoryTest {

    @Autowired
    HotelImageRepository images;

    @Test
    void coverImageIsTheFirstStoredImageOfEachRequestedHotel() {
        images.saveAll(List.of(
                new HotelImage(1L, "1-cover.jpg"), new HotelImage(1L, "1-second.jpg"),
                new HotelImage(2L, "2-cover.jpg"),
                new HotelImage(3L, "3-not-requested.jpg")));

        assertThat(images.findCoverImages(List.of(1L, 2L, 404L)))
                .extracting(HotelImage::getHotelCode, HotelImage::getImage)
                .containsExactlyInAnyOrder(tuple(1L, "1-cover.jpg"), tuple(2L, "2-cover.jpg"));
    }

    @Test
    void hotelImagesComeBackInStoredOrder() {
        images.saveAll(List.of(new HotelImage(7L, "a.jpg"), new HotelImage(7L, "b.jpg"), new HotelImage(7L, "c.jpg")));

        assertThat(images.findByHotelCodeOrderByIdAsc(7L)).extracting(HotelImage::getImage)
                .containsExactly("a.jpg", "b.jpg", "c.jpg");
    }
}
