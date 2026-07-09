package com.miirphys.bodiala.provider.hotelbeds;

import com.miirphys.bodiala.staticdata.ImportResult;
import com.miirphys.bodiala.staticdata.repo.HotelRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Seeds the static-data cache from the Hotelbeds Content API on startup, so you don't have to call
 * {@code POST /api/static-data/import?provider=hotelbeds} by hand.
 *
 * <p>Guarded to be safe and quiet: only when {@code hotel.provider=hotelbeds}, credentials are set,
 * {@code hotelbeds.import-on-startup} is enabled (ON only in the {@code hotelbeds-stub} profile, so a
 * real run never spends quota unprompted), and the cache is still empty (a fresh DB). It never fails
 * startup — a problem is logged and the app continues. Runs after the context is ready.
 */
@Component
@ConditionalOnProperty(name = "hotel.provider", havingValue = "hotelbeds")
public class HotelbedsStartupImporter implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(HotelbedsStartupImporter.class);

    private final HotelbedsProperties properties;
    private final HotelbedsCatalogImporter importer;
    private final HotelRepository hotelRepository;

    public HotelbedsStartupImporter(HotelbedsProperties properties, HotelbedsCatalogImporter importer,
                                    HotelRepository hotelRepository) {
        this.properties = properties;
        this.importer = importer;
        this.hotelRepository = hotelRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isImportOnStartup()) {
            return;
        }
        if (!properties.hasCredentials()) {
            log.info("Hotelbeds auto-import skipped — credentials not set.");
            return;
        }
        if (hotelRepository.count() > 0) {
            log.info("Hotelbeds auto-import skipped — cache already populated. "
                    + "POST /api/static-data/import?provider=hotelbeds to refresh.");
            return;
        }
        try {
            ImportResult result = importer.importCatalog();
            log.info("Hotelbeds auto-import loaded {} hotels into the cache.",
                    result.imported().getOrDefault("HOTEL", 0));
        } catch (RuntimeException e) {
            log.warn("Hotelbeds auto-import failed (startup continues): {}. "
                    + "Is the stub up? POST /api/static-data/import?provider=hotelbeds to seed manually.",
                    e.getMessage());
        }
    }
}
