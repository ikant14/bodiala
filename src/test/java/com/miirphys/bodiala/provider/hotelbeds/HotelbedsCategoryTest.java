package com.miirphys.bodiala.provider.hotelbeds;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Unit tests for the Hotelbeds category-code → star-rating extraction. */
class HotelbedsCategoryTest {

    @Test
    void extractsLeadingDigitFromStarCode() {
        assertThat(HotelbedsCategory.stars("4EST")).isEqualTo(4);
        assertThat(HotelbedsCategory.stars("5EST")).isEqualTo(5);
    }

    @Test
    void handlesKeysAndPlainNumbers() {
        assertThat(HotelbedsCategory.stars("3LL")).isEqualTo(3);   // llaves / keys
        assertThat(HotelbedsCategory.stars("4")).isEqualTo(4);     // already numeric
    }

    @Test
    void returnsNullForMissingOrDigitlessCode() {
        assertThat(HotelbedsCategory.stars(null)).isNull();
        assertThat(HotelbedsCategory.stars("")).isNull();
        assertThat(HotelbedsCategory.stars("UNRATED")).isNull();
    }
}
