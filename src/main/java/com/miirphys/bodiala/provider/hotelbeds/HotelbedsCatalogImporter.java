package com.miirphys.bodiala.provider.hotelbeds;

import com.miirphys.bodiala.provider.CatalogImporter;
import com.miirphys.bodiala.provider.ProviderId;
import com.miirphys.bodiala.provider.hotelbeds.dto.HotelContentResponse;
import com.miirphys.bodiala.staticdata.ImportResult;
import com.miirphys.bodiala.staticdata.domain.City;
import com.miirphys.bodiala.staticdata.domain.Country;
import com.miirphys.bodiala.staticdata.domain.Hotel;
import com.miirphys.bodiala.staticdata.domain.HotelImage;
import com.miirphys.bodiala.staticdata.domain.PropertyAmenity;
import com.miirphys.bodiala.staticdata.repo.CityRepository;
import com.miirphys.bodiala.staticdata.repo.CountryRepository;
import com.miirphys.bodiala.staticdata.repo.HotelImageRepository;
import com.miirphys.bodiala.staticdata.repo.HotelRepository;
import com.miirphys.bodiala.staticdata.repo.PropertyAmenityRepository;
import com.miirphys.bodiala.staticdata.repo.RoomAmenityRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Hotelbeds {@link CatalogImporter}: pages the Content API {@code GET /hotel-content-api/1.0/hotels}
 * and fills the shared cache (full-snapshot delete-all + insert, like the RezLive importer). Country
 * and City are derived from the hotels; room amenities aren't a separate Content concept, so that
 * table is cleared.
 *
 * <p><b>Bounded on purpose:</b> the full catalog is ~250k hotels — far beyond the test key's daily
 * quota — so the sync stops at {@code hotelbeds.catalog-max-hotels} (default 100). Raise it (and use
 * a production key) for a full sync. Always loaded and registered under {@link ProviderId#HOTELBEDS};
 * {@code POST /api/static-data/import?provider=hotelbeds} runs it.
 */
@Component
public class HotelbedsCatalogImporter implements CatalogImporter {

    private static final Logger log = LoggerFactory.getLogger(HotelbedsCatalogImporter.class);

    private final HotelbedsJsonClient client;
    private final HotelbedsProperties properties;
    private final HotelbedsFacilityCatalog facilityCatalog;
    private final CountryRepository countryRepository;
    private final CityRepository cityRepository;
    private final HotelRepository hotelRepository;
    private final HotelImageRepository hotelImageRepository;
    private final PropertyAmenityRepository propertyAmenityRepository;
    private final RoomAmenityRepository roomAmenityRepository;

    public HotelbedsCatalogImporter(HotelbedsJsonClient client, HotelbedsProperties properties,
                                    HotelbedsFacilityCatalog facilityCatalog,
                                    CountryRepository countryRepository, CityRepository cityRepository,
                                    HotelRepository hotelRepository, HotelImageRepository hotelImageRepository,
                                    PropertyAmenityRepository propertyAmenityRepository,
                                    RoomAmenityRepository roomAmenityRepository) {
        this.client = client;
        this.properties = properties;
        this.facilityCatalog = facilityCatalog;
        this.countryRepository = countryRepository;
        this.cityRepository = cityRepository;
        this.hotelRepository = hotelRepository;
        this.hotelImageRepository = hotelImageRepository;
        this.propertyAmenityRepository = propertyAmenityRepository;
        this.roomAmenityRepository = roomAmenityRepository;
    }

    @Override
    public ProviderId id() {
        return ProviderId.HOTELBEDS;
    }

    @Override
    @Transactional
    public ImportResult importCatalog() {
        if (!properties.hasCredentials()) {
            throw new IllegalStateException(
                    "Hotelbeds credentials are not configured (hotelbeds.api-key / hotelbeds.secret).");
        }
        List<HotelContentResponse.Hotel> hotels = fetchHotels();

        List<Hotel> hotelEntities = new ArrayList<>();
        List<HotelImage> imageEntities = new ArrayList<>();
        List<PropertyAmenity> amenityEntities = new ArrayList<>();
        Map<String, Country> countries = new LinkedHashMap<>();
        Map<String, City> cities = new LinkedHashMap<>();

        for (HotelContentResponse.Hotel h : hotels) {
            if (h.code() == null) {
                continue;
            }
            hotelEntities.add(HotelbedsContentMapper.toHotel(h));
            imageEntities.addAll(HotelbedsContentMapper.toImages(h));
            PropertyAmenity amenity = HotelbedsContentMapper.toPropertyAmenity(h, facilityCatalog);
            if (amenity != null) {
                amenityEntities.add(amenity);
            }
            if (h.countryCode() != null) {
                countries.putIfAbsent(h.countryCode(), HotelbedsContentMapper.toCountry(h));
            }
            if (h.destinationCode() != null) {
                cities.putIfAbsent(h.destinationCode(), HotelbedsContentMapper.toCity(h));
            }
        }

        // Full-snapshot replace of each table.
        replace(countryRepository::deleteAllInBatch, () -> countryRepository.saveAll(countries.values()));
        replace(cityRepository::deleteAllInBatch, () -> cityRepository.saveAll(cities.values()));
        replace(hotelRepository::deleteAllInBatch, () -> hotelRepository.saveAll(hotelEntities));
        replace(hotelImageRepository::deleteAllInBatch, () -> hotelImageRepository.saveAll(imageEntities));
        replace(propertyAmenityRepository::deleteAllInBatch, () -> propertyAmenityRepository.saveAll(amenityEntities));
        roomAmenityRepository.deleteAllInBatch(); // no room-amenity concept in Hotelbeds content

        ImportResult result = ImportResult.empty();
        result.imported().put("COUNTRY", countries.size());
        result.imported().put("CITY", cities.size());
        result.imported().put("HOTEL", hotelEntities.size());
        result.imported().put("HOTEL_IMAGES", imageEntities.size());
        result.imported().put("PROPERTY_AMENITIES", amenityEntities.size());
        log.info("Hotelbeds catalog import complete: {} hotels across {} cities / {} countries "
                        + "({} images, {} amenities)",
                hotelEntities.size(), cities.size(), countries.size(), imageEntities.size(), amenityEntities.size());
        return result;
    }

    private List<HotelContentResponse.Hotel> fetchHotels() {
        int pageSize = clamp(properties.getCatalogPageSize(), 1, 1000);
        int maxHotels = Math.max(1, properties.getCatalogMaxHotels());
        List<HotelContentResponse.Hotel> collected = new ArrayList<>();
        int from = 1;
        while (collected.size() < maxHotels) {
            int to = from + Math.min(pageSize, maxHotels - collected.size()) - 1;
            String path = "/hotel-content-api/1.0/hotels?fields=all&language=" + properties.getLanguage()
                    + "&from=" + from + "&to=" + to;
            HotelContentResponse page = client.get(path, HotelContentResponse.class);
            List<HotelContentResponse.Hotel> hotels = (page == null || page.hotels() == null)
                    ? List.of() : page.hotels();
            if (hotels.isEmpty()) {
                break;
            }
            collected.addAll(hotels);
            Integer total = page == null ? null : page.total();
            if (total != null && to >= total) {
                break;
            }
            from = to + 1;
        }
        return collected;
    }

    private static void replace(Runnable delete, Runnable insert) {
        delete.run();
        insert.run();
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }
}
