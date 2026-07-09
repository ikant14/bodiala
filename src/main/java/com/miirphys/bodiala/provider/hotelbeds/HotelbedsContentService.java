package com.miirphys.bodiala.provider.hotelbeds;

import com.miirphys.bodiala.provider.hotelbeds.dto.HotelContentResponse;
import com.miirphys.bodiala.staticdata.domain.Hotel;
import com.miirphys.bodiala.staticdata.domain.PropertyAmenity;
import com.miirphys.bodiala.staticdata.repo.HotelImageRepository;
import com.miirphys.bodiala.staticdata.repo.HotelRepository;
import com.miirphys.bodiala.staticdata.repo.PropertyAmenityRepository;
import java.util.NoSuchElementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Live per-hotel content from the Hotelbeds Content API, for hotels that aren't in the bounded
 * catalog cache — an availability search returns far more hotels than the (quota-bounded) catalog
 * import pre-loads, so a "details" click on one of those would otherwise 404.
 *
 * <p>Fetches the single hotel by code ({@code GET /hotel-content-api/1.0/hotels?codes=…}) and lazily
 * upserts it (+ images + amenities) into the shared cache, so the detail view works and repeat views
 * are cache hits (no further quota). Costs one Content-API call the first time a given hotel is viewed.
 */
@Service
public class HotelbedsContentService {

    private static final Logger log = LoggerFactory.getLogger(HotelbedsContentService.class);

    private final HotelbedsJsonClient client;
    private final HotelbedsProperties properties;
    private final HotelbedsFacilityCatalog facilityCatalog;
    private final HotelRepository hotelRepository;
    private final HotelImageRepository hotelImageRepository;
    private final PropertyAmenityRepository propertyAmenityRepository;

    public HotelbedsContentService(HotelbedsJsonClient client, HotelbedsProperties properties,
                                   HotelbedsFacilityCatalog facilityCatalog,
                                   HotelRepository hotelRepository, HotelImageRepository hotelImageRepository,
                                   PropertyAmenityRepository propertyAmenityRepository) {
        this.client = client;
        this.properties = properties;
        this.facilityCatalog = facilityCatalog;
        this.hotelRepository = hotelRepository;
        this.hotelImageRepository = hotelImageRepository;
        this.propertyAmenityRepository = propertyAmenityRepository;
    }

    public boolean isConfigured() {
        return properties.hasCredentials();
    }

    /**
     * Fetch one hotel's content by Hotelbeds code, upsert it (+ its images/amenities) into the shared
     * cache, and return the stored {@link Hotel}. Unknown code → {@link NoSuchElementException} (404).
     */
    @Transactional
    public Hotel fetchAndCache(Long code) {
        if (!properties.hasCredentials()) {
            throw new IllegalStateException(
                    "Hotelbeds credentials are not configured (hotelbeds.api-key / hotelbeds.secret).");
        }
        String path = "/hotel-content-api/1.0/hotels?fields=all&language=" + properties.getLanguage()
                + "&codes=" + code + "&from=1&to=1";
        HotelContentResponse response = client.get(path, HotelContentResponse.class);
        HotelContentResponse.Hotel content = (response == null || response.hotels() == null
                || response.hotels().isEmpty()) ? null : response.hotels().get(0);
        if (content == null || content.code() == null) {
            throw new NoSuchElementException("Unknown Hotelbeds hotel " + code);
        }

        Hotel hotel = hotelRepository.save(HotelbedsContentMapper.toHotel(content));
        hotelImageRepository.deleteByHotelCode(code);
        hotelImageRepository.saveAll(HotelbedsContentMapper.toImages(content));
        PropertyAmenity amenity = HotelbedsContentMapper.toPropertyAmenity(content, facilityCatalog);
        if (amenity != null) {
            propertyAmenityRepository.save(amenity);
        }
        log.info("Cached Hotelbeds hotel {} ({}) on demand", code, hotel.getName());
        return hotel;
    }
}
