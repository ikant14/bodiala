package com.miirphys.bodiala.provider.hotelbeds;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for the Hotelbeds APItude API (the second supplier). Bound from {@code hotelbeds.*};
 * the {@code apiKey} + {@code secret} produce the per-request {@code X-Signature}
 * (see {@link HotelbedsSignature}), and {@code apiKey} is also sent as the {@code Api-key} header.
 *
 * <p>Active only when {@code hotel.provider=hotelbeds}. Set the secrets via a git-ignored {@code .env}
 * (copy {@code .env.example}) or real env vars.
 */
@ConfigurationProperties(prefix = "hotelbeds")
public class HotelbedsProperties {

    /** API base URL. Test: {@code https://api.test.hotelbeds.com}; prod: {@code https://api.hotelbeds.com}. */
    private String baseUrl = "https://api.test.hotelbeds.com";

    /** The Hotelbeds API key (also the {@code Api-key} header). */
    private String apiKey = "";

    /** The Hotelbeds shared secret (used only to compute the signature; never sent directly). */
    private String secret = "";

    /** Response language code (e.g. {@code ENG}). */
    private String language = "ENG";

    /** Optional source market (e.g. {@code ES}). */
    private String sourceMarket = "";

    /** Content-catalog page size (rows per {@code /hotels} request; max 1000). */
    private int catalogPageSize = 100;

    /** Bounded catalog sync — stop after this many hotels (default keeps the test quota safe). */
    private int catalogMaxHotels = 100;

    /**
     * Auto-import the catalog on startup (only when {@code hotel.provider=hotelbeds}, the cache is
     * empty, and credentials are set). Off by default so a real run never spends quota unprompted;
     * the {@code hotelbeds-stub} profile turns it ON, so the local stub seeds the cache with no
     * manual {@code POST /api/static-data/import}.
     */
    private boolean importOnStartup = false;

    /** True only when the credentials needed for live calls are present. */
    public boolean hasCredentials() {
        return notBlank(apiKey) && notBlank(secret) && notBlank(baseUrl);
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getSourceMarket() {
        return sourceMarket;
    }

    public void setSourceMarket(String sourceMarket) {
        this.sourceMarket = sourceMarket;
    }

    public int getCatalogPageSize() {
        return catalogPageSize;
    }

    public void setCatalogPageSize(int catalogPageSize) {
        this.catalogPageSize = catalogPageSize;
    }

    public int getCatalogMaxHotels() {
        return catalogMaxHotels;
    }

    public void setCatalogMaxHotels(int catalogMaxHotels) {
        this.catalogMaxHotels = catalogMaxHotels;
    }

    public boolean isImportOnStartup() {
        return importOnStartup;
    }

    public void setImportOnStartup(boolean importOnStartup) {
        this.importOnStartup = importOnStartup;
    }
}
