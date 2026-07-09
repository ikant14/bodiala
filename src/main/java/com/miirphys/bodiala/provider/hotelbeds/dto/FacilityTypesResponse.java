package com.miirphys.bodiala.provider.hotelbeds.dto;

import java.util.List;

/**
 * Subset of the Hotelbeds {@code GET /hotel-content-api/1.0/types/facilities} master. Each entry
 * gives the human {@code description} for a {@code (facilityGroupCode, code)} pair — the names that
 * a hotel's code-only {@link HotelContentResponse.Facility} list references.
 */
public record FacilityTypesResponse(List<FacilityType> facilities, Integer total) {

    public record FacilityType(Integer code, Integer facilityGroupCode, HotelContentResponse.Content description) {
    }
}
