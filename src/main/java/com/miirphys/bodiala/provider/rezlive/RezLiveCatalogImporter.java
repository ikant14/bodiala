package com.miirphys.bodiala.provider.rezlive;

import com.miirphys.bodiala.provider.CatalogImporter;
import com.miirphys.bodiala.provider.ProviderId;
import com.miirphys.bodiala.provider.rezlive.staticimport.StaticDataImportService;
import com.miirphys.bodiala.staticdata.ImportResult;
import org.springframework.stereotype.Component;

/**
 * RezLive {@link CatalogImporter}: imports the CSV master files from the configured
 * {@code rezlive.static-data-dir}. Always loaded and registered under {@link ProviderId#REZLIVE};
 * {@code POST /api/static-data/import?provider=rezlive} runs it (the default when none is given).
 */
@Component
public class RezLiveCatalogImporter implements CatalogImporter {

    private final StaticDataImportService service;

    public RezLiveCatalogImporter(StaticDataImportService service) {
        this.service = service;
    }

    @Override
    public ProviderId id() {
        return ProviderId.REZLIVE;
    }

    @Override
    public ImportResult importCatalog() {
        return service.importAll();
    }
}
