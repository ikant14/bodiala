package com.miirphys.bodiala.staticdata.web;

import com.miirphys.bodiala.staticdata.domain.Hotel;
import java.util.List;

/**
 * Assembled view of a hotel from the cached static data: the core hotel record plus its
 * images and amenity lists (each stored in a separate master file, joined on hotel code).
 */
public record HotelView(
        Hotel hotel,
        List<String> images,
        String propertyAmenities,
        String roomAmenities) {
}
