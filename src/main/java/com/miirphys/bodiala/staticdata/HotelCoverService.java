package com.miirphys.bodiala.staticdata;

import com.miirphys.bodiala.provider.ProviderId;
import com.miirphys.bodiala.provider.hotelbeds.HotelbedsContentService;
import com.miirphys.bodiala.staticdata.domain.Hotel;
import com.miirphys.bodiala.staticdata.domain.HotelImage;
import com.miirphys.bodiala.staticdata.repo.HotelImageRepository;
import com.miirphys.bodiala.staticdata.repo.HotelRepository;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * One cover photo per hotel, for the search-result cards. Supplier availability carries no photos and
 * the catalog import is bounded, so most results of a single-city search aren't cached yet: those are
 * fetched live in ONE batched Content-API call (one quota request per call that has misses) and cached,
 * so a hotel costs a request at most once — later searches and its detail view are cache hits.
 *
 * <p>Photos are cosmetic: a failed live fetch is logged and the cached covers are returned anyway.
 */
@Service
public class HotelCoverService {

    private static final Logger log = LoggerFactory.getLogger(HotelCoverService.class);

    /** Generous for one results page (12 cards) while keeping a single Content-API call small. */
    static final int MAX_CODES = 100;

    private final HotelImageRepository images;
    private final HotelRepository hotels;
    private final HotelbedsContentService hotelbedsContent;

    public HotelCoverService(HotelImageRepository images, HotelRepository hotels,
                             HotelbedsContentService hotelbedsContent) {
        this.images = images;
        this.hotels = hotels;
        this.hotelbedsContent = hotelbedsContent;
    }

    /**
     * Cover URL per hotel code; codes without a cover are absent. With {@code provider=HOTELBEDS},
     * codes the cache has never seen are fetched live first (hotels cached without photos are not
     * re-fetched).
     */
    public Map<Long, String> covers(Collection<Long> codes, ProviderId provider) {
        Set<Long> wanted = codes.stream().filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));
        if (wanted.size() > MAX_CODES) {
            throw new IllegalArgumentException("at most " + MAX_CODES + " hotel codes per request");
        }
        Map<Long, String> covers = cachedCovers(wanted);

        if (provider == ProviderId.HOTELBEDS && hotelbedsContent.isConfigured()) {
            Set<Long> missing = wanted.stream().filter(c -> !covers.containsKey(c))
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            Set<Long> known = hotels.findAllById(missing).stream().map(Hotel::getHotelCode).collect(Collectors.toSet());
            List<Long> uncached = missing.stream().filter(c -> !known.contains(c)).toList();
            if (!uncached.isEmpty()) {
                try {
                    hotelbedsContent.fetchAndCacheAll(uncached);
                    covers.putAll(cachedCovers(uncached));
                } catch (RuntimeException e) {
                    log.warn("Couldn't fetch content for {} uncached hotels; their cards keep the placeholder: {}",
                            uncached.size(), e.getMessage());
                }
            }
        }
        return covers;
    }

    /** Mutable, so the live-fetched covers can be added to it. */
    private Map<Long, String> cachedCovers(Collection<Long> codes) {
        if (codes.isEmpty()) {
            return new HashMap<>();
        }
        return images.findCoverImages(codes).stream()
                .collect(Collectors.toMap(HotelImage::getHotelCode, HotelImage::getImage, (a, b) -> a, HashMap::new));
    }
}
