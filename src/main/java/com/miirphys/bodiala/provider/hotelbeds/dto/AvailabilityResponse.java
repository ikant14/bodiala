package com.miirphys.bodiala.provider.hotelbeds.dto;

import java.util.List;

/**
 * The subset of the Hotelbeds {@code POST /hotel-api/1.0/hotels} availability response we map into
 * the neutral search result. Unknown fields are ignored (see
 * {@code spring.jackson.deserialization.fail-on-unknown-properties=false}).
 */
public record AvailabilityResponse(Hotels hotels) {

    public record Hotels(String checkIn, String checkOut, Integer total, List<Hotel> hotels) {
    }

    public record Hotel(
            Integer code,
            String name,
            String categoryCode,
            String categoryName,
            String destinationCode,
            String currency,
            String minRate,
            String maxRate,
            List<Room> rooms) {
    }

    public record Room(String code, String name, List<Rate> rates) {
    }

    public record Rate(
            String rateKey,
            String rateClass,
            String rateType,
            String net,
            String sellingRate,
            String boardCode,
            String boardName,
            Integer rooms,
            Integer adults,
            Integer children,
            String childrenAges) {
    }
}
