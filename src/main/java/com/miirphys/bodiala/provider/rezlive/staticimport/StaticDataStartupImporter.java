package com.miirphys.bodiala.provider.rezlive.staticimport;

import com.miirphys.bodiala.staticdata.ImportResult;
import com.miirphys.bodiala.provider.rezlive.RezLiveProperties;
import com.miirphys.bodiala.staticdata.repo.CountryRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Seeds the local static-data cache from the CSV master files on startup, so you don't have to call
 * {@code POST /api/static-data/import} by hand.
 *
 * <p>Guarded to be safe and quiet: it only runs when {@code rezlive.import-on-startup} is enabled,
 * the static-data directory exists, and the cache is still empty (a fresh DB). It never fails startup
 * — a problem is logged and the app continues. Runs after the context is ready, so it doesn't delay
 * the server binding its port.
 */
@Component
@ConditionalOnProperty(name = "hotel.provider", havingValue = "rezlive", matchIfMissing = true)
public class StaticDataStartupImporter implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StaticDataStartupImporter.class);

    private final RezLiveProperties properties;
    private final StaticDataImportService importService;
    private final CountryRepository countryRepository;

    public StaticDataStartupImporter(RezLiveProperties properties,
                                     StaticDataImportService importService,
                                     CountryRepository countryRepository) {
        this.properties = properties;
        this.importService = importService;
        this.countryRepository = countryRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isImportOnStartup()) {
            return;
        }
        Path dir = Path.of(properties.getStaticDataDir());
        if (!Files.isDirectory(dir)) {
            log.info("Static-data auto-import skipped — directory {} not found (drop the CSV master files "
                    + "there, or POST /api/static-data/import).", dir.toAbsolutePath());
            return;
        }
        if (countryRepository.count() > 0) {
            log.info("Static-data auto-import skipped — cache already populated. "
                    + "POST /api/static-data/import to refresh.");
            return;
        }
        try {
            ImportResult result = importService.importAll(dir);
            log.info("Static-data auto-import loaded {} rows from {} ({} file(s) skipped).",
                    result.totalRows(), dir.toAbsolutePath(), result.skipped().size());
        } catch (RuntimeException e) {
            log.warn("Static-data auto-import failed (startup continues): {}", e.getMessage());
        }
    }
}
