package com.miirphys.bodiala.provider.hotelbeds;

import com.miirphys.bodiala.provider.hotelbeds.dto.HotelContentResponse;
import com.miirphys.bodiala.staticdata.domain.City;
import com.miirphys.bodiala.staticdata.domain.Country;
import com.miirphys.bodiala.staticdata.domain.Hotel;
import com.miirphys.bodiala.staticdata.domain.HotelImage;
import com.miirphys.bodiala.staticdata.domain.PropertyAmenity;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Maps Hotelbeds Content API hotels into the shared cache entities. Pure / HTTP-free
 * (fixture-testable). Country/City are derived from the hotels themselves (each carries
 * {@code countryCode} + {@code destinationCode} + {@code city}), so the {@code cityCode} stored is
 * the Hotelbeds destination code — exactly what {@code HotelbedsSearchProvider} sends as
 * {@code destination.code}.
 */
public final class HotelbedsContentMapper {

    private static final String IMAGE_BASE = "https://photos.hotelbeds.com/giata/bigger/";

    /** Facility groups that aren't guest amenities: 20 = accommodation type, 30 = payment methods. */
    private static final Set<Integer> NON_AMENITY_GROUPS = Set.of(20, 30);

    private HotelbedsContentMapper() {
    }

    public static Hotel toHotel(HotelContentResponse.Hotel h) {
        Hotel hotel = new Hotel();
        hotel.setHotelCode(h.code() == null ? null : h.code().longValue());
        hotel.setName(content(h.name()));
        hotel.setCityCode(h.destinationCode());
        hotel.setCountryCode(h.countryCode());
        hotel.setRating(HotelbedsCategory.stars(h.categoryCode()));
        hotel.setHotelAddress(content(h.address()));
        hotel.setHotelPostalCode(h.postalCode());
        if (h.coordinates() != null) {
            hotel.setLatitude(h.coordinates().latitude());
            hotel.setLongitude(h.coordinates().longitude());
        }
        hotel.setDescription(content(h.description()));
        return hotel;
    }

    public static List<HotelImage> toImages(HotelContentResponse.Hotel h) {
        if (h.images() == null || h.code() == null) {
            return List.of();
        }
        long code = h.code().longValue();
        return h.images().stream()
                .filter(i -> i.path() != null && !i.path().isBlank())
                .map(i -> new HotelImage(code, IMAGE_BASE + i.path()))
                .toList();
    }

    /**
     * Joins the hotel's guest-amenity facility names into the shared {@code PropertyAmenity} text
     * column (null if none). The hotel carries only facility codes, so {@code resolver} (the
     * {@code /types/facilities} master) supplies names; non-amenity facilities — payment methods,
     * accommodation type, numeric metadata (renovation year, room counts), POI distances, operational
     * hours, and fees/deposits — are filtered out (see {@link #isAmenity}).
     */
    public static PropertyAmenity toPropertyAmenity(HotelContentResponse.Hotel h, FacilityNameResolver resolver) {
        if (h.facilities() == null || h.facilities().isEmpty() || h.code() == null) {
            return null;
        }
        String names = h.facilities().stream()
                .filter(HotelbedsContentMapper::isAmenity)
                .map(f -> resolver.nameFor(f.facilityGroupCode(), f.facilityCode()))
                .filter(n -> n != null && !n.isBlank())
                .distinct()
                .collect(Collectors.joining(","));
        return names.isBlank() ? null : new PropertyAmenity(h.code().longValue(), names);
    }

    /**
     * A guest amenity is a facility that's actually present ({@code indYesOrNo != false}), in an
     * amenity group, and carries none of the "this is metadata, not an amenity" markers — a
     * {@code number} (counts/years), a {@code distance} (POI), {@code timeFrom/To} (hours), or
     * {@code amount/currency/applicationType} (fees/deposits).
     */
    private static boolean isAmenity(HotelContentResponse.Facility f) {
        if (f.facilityGroupCode() != null && NON_AMENITY_GROUPS.contains(f.facilityGroupCode())) {
            return false;
        }
        if (Boolean.FALSE.equals(f.indYesOrNo())) {
            return false;
        }
        return f.number() == null && f.distance() == null && f.amount() == null
                && f.currency() == null && f.timeFrom() == null && f.timeTo() == null
                && f.applicationType() == null;
    }

    public static Country toCountry(HotelContentResponse.Hotel h) {
        return h.countryCode() == null ? null : new Country(h.countryCode(), h.countryCode());
    }

    public static City toCity(HotelContentResponse.Hotel h) {
        if (h.destinationCode() == null) {
            return null;
        }
        String name = content(h.city()) != null ? content(h.city()) : h.destinationCode();
        return new City(h.destinationCode(), name, h.countryCode());
    }

    private static String content(HotelContentResponse.Content c) {
        return c == null ? null : c.content();
    }
}
