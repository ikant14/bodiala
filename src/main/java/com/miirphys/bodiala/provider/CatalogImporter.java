package com.miirphys.bodiala.provider;

import com.miirphys.bodiala.staticdata.ImportResult;

/**
 * Fills the shared static-data cache (country / city / hotel / images / amenities) from a supplier's
 * catalog (the Hotelbeds Content API). All implementations are loaded and indexed by
 * {@link CatalogImporterRegistry}; {@code POST /api/static-data/import?provider=} delegates to the
 * chosen one (defaulting to {@code hotel.provider}).
 */
public interface CatalogImporter {

    ProviderId id();

    ImportResult importCatalog();
}
