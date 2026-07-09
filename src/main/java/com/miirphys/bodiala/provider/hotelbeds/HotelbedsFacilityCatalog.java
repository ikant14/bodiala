package com.miirphys.bodiala.provider.hotelbeds;

import com.miirphys.bodiala.provider.hotelbeds.dto.FacilityTypesResponse;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Lazily loads and caches the Hotelbeds facilities master ({@code GET /types/facilities}) so a
 * hotel's code-only facilities can be resolved to names. The master is small (~hundreds of entries)
 * and effectively static, so it's fetched once (one Content-API call) and kept in memory.
 *
 * <p>Degrades gracefully: if the master can't be loaded (no credentials / upstream error) it caches
 * an empty map and names simply resolve to {@code null} (amenities come back empty) rather than
 * failing the import or the detail fetch. A restart re-attempts the load.
 */
@Component
public class HotelbedsFacilityCatalog implements FacilityNameResolver {

    private static final Logger log = LoggerFactory.getLogger(HotelbedsFacilityCatalog.class);

    private final HotelbedsJsonClient client;
    private final HotelbedsProperties properties;
    private volatile Map<String, String> namesByGroupAndCode;

    public HotelbedsFacilityCatalog(HotelbedsJsonClient client, HotelbedsProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    @Override
    public String nameFor(Integer facilityGroupCode, Integer facilityCode) {
        if (facilityGroupCode == null || facilityCode == null) {
            return null;
        }
        return catalog().get(key(facilityGroupCode, facilityCode));
    }

    private Map<String, String> catalog() {
        Map<String, String> cached = namesByGroupAndCode;
        if (cached == null) {
            synchronized (this) {
                cached = namesByGroupAndCode;
                if (cached == null) {
                    cached = load();
                    namesByGroupAndCode = cached;
                }
            }
        }
        return cached;
    }

    private Map<String, String> load() {
        if (!properties.hasCredentials()) {
            log.warn("Hotelbeds facility master not loaded — credentials missing; amenities will be empty.");
            return Map.of();
        }
        try {
            String path = "/hotel-content-api/1.0/types/facilities?fields=all&language="
                    + properties.getLanguage() + "&from=1&to=1000";
            FacilityTypesResponse response = client.get(path, FacilityTypesResponse.class);
            Map<String, String> map = new HashMap<>();
            if (response != null && response.facilities() != null) {
                for (FacilityTypesResponse.FacilityType type : response.facilities()) {
                    if (type.code() != null && type.facilityGroupCode() != null
                            && type.description() != null && type.description().content() != null) {
                        map.put(key(type.facilityGroupCode(), type.code()), type.description().content());
                    }
                }
            }
            log.info("Loaded {} Hotelbeds facility types", map.size());
            return Map.copyOf(map);
        } catch (RuntimeException e) {
            log.warn("Failed to load Hotelbeds facility master ({}); amenities will be empty until restart.",
                    e.getMessage());
            return Map.of();
        }
    }

    private static String key(Integer facilityGroupCode, Integer facilityCode) {
        return facilityGroupCode + "#" + facilityCode;
    }
}
