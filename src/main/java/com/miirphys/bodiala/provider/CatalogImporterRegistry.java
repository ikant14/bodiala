package com.miirphys.bodiala.provider;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Registry of the loaded {@link CatalogImporter}s. {@code POST /api/static-data/import} selects the
 * supplier to import from with the {@code ?provider=} query param; omitting it uses the
 * {@code hotel.provider} default.
 */
@Component
public class CatalogImporterRegistry extends ProviderRegistry<CatalogImporter> {

    public CatalogImporterRegistry(List<CatalogImporter> importers,
                                   @Value("${hotel.provider:hotelbeds}") String defaultProvider) {
        super(importers, CatalogImporter::id, ProviderId.from(defaultProvider));
    }
}
