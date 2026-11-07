package com.miirphys.bodiala.provider.hotelbeds;

import com.miirphys.bodiala.provider.hotelbeds.dto.HotelContentResponse;
import com.miirphys.bodiala.staticdata.domain.Hotel;
import com.miirphys.bodiala.staticdata.domain.PropertyAmenity;
import com.miirphys.bodiala.staticdata.repo.HotelImageRepository;
import com.miirphys.bodiala.staticdata.repo.HotelRepository;
import com.miirphys.bodiala.staticdata.repo.PropertyAmenityRepository;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
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
        HotelContentResponse.Hotel content = fetch(List.of(code)).stream().findFirst().orElse(null);
        if (content == null) {
            throw new NoSuchElementException("Unknown Hotelbeds hotel " + code);
        }
        Hotel hotel = cache(content);
        log.info("Cached Hotelbeds hotel {} ({}) on demand", code, hotel.getName());
        return hotel;
    }

    /**
     * Batch variant for a page of search results: fetch every given hotel in <em>one</em> Content-API
     * call ({@code codes=a,b,c}) and cache each one returned. Codes Hotelbeds doesn't know are skipped.
     */
    @Transactional
    public List<Hotel> fetchAndCacheAll(Collection<Long> codes) {
        if (codes.isEmpty()) {
            return List.of();
        }
        List<Hotel> cached = fetch(codes).stream().map(this::cache).toList();
        log.info("Cached {} of {} requested Hotelbeds hotels on demand", cached.size(), codes.size());
        return cached;
    }

    /** One signed {@code GET /hotels?codes=…} call; only hotels that carry a code come back. */
    private List<HotelContentResponse.Hotel> fetch(Collection<Long> codes) {
        if (!properties.hasCredentials()) {
            throw new IllegalStateException(
                    "Hotelbeds credentials are not configured (hotelbeds.api-key / hotelbeds.secret).");
        }
        String path = "/hotel-content-api/1.0/hotels?fields=all&language=" + properties.getLanguage()
                + "&codes=" + codes.stream().map(String::valueOf).collect(Collectors.joining(","))
                + "&from=1&to=" + codes.size();
        HotelContentResponse response = client.get(path, HotelContentResponse.class);
        if (response == null || response.hotels() == null) {
            return List.of();
        }
        return response.hotels().stream().filter(h -> h.code() != null).toList();
    }

    /** Upsert one hotel plus its images (replaced) and amenities into the shared cache. */
    private Hotel cache(HotelContentResponse.Hotel content) {
        long code = content.code().longValue();
        Hotel hotel = hotelRepository.save(HotelbedsContentMapper.toHotel(content));
        hotelImageRepository.deleteByHotelCode(code);
        hotelImageRepository.saveAll(HotelbedsContentMapper.toImages(content));
        PropertyAmenity amenity = HotelbedsContentMapper.toPropertyAmenity(content, facilityCatalog);
        if (amenity != null) {
            propertyAmenityRepository.save(amenity);
        }
        return hotel;
    }
}
