package com.miirphys.bodiala.provider.hotelbeds.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Subset of the Hotelbeds {@code GET /hotel-content-api/1.0/hotels} (paged) response. Text fields
 * are {@code {content, languageCode}} objects, not plain strings.
 */
public record HotelContentResponse(List<Hotel> hotels, Integer from, Integer to, Integer total) {

    public record Hotel(
            Integer code,
            Content name,
            Content description,
            String countryCode,
            String destinationCode,
            Content city,
            String categoryCode,
            Content address,
            String postalCode,
            Coordinates coordinates,
            List<Image> images,
            List<Facility> facilities) {
    }

    public record Content(String content, String languageCode) {
    }

    public record Coordinates(Double longitude, Double latitude) {
    }

    public record Image(String path, String imageTypeCode, Integer order) {
    }

    /**
     * A facility on a hotel. The real API sends only the codes — the human name comes from the
     * {@code /types/facilities} master (see {@link com.miirphys.bodiala.provider.hotelbeds.HotelbedsFacilityCatalog}).
     * The remaining fields are what distinguishes a real amenity from metadata: a {@code number}
     * (renovation year, room count), a {@code distance} (POI distance), operational {@code timeFrom/To},
     * or {@code amount/currency/applicationType} (fees/deposits) all mark a NON-amenity;
     * {@code indYesOrNo=false} means "not present".
     */
    public record Facility(
            Integer facilityCode,
            Integer facilityGroupCode,
            Boolean indYesOrNo,
            BigDecimal number,
            BigDecimal distance,
            BigDecimal amount,
            String currency,
            String timeFrom,
            String timeTo,
            String applicationType) {
    }
}
