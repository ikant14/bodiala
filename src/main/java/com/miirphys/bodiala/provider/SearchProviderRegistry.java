package com.miirphys.bodiala.provider;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Registry of the loaded {@link SearchProvider}s. A search request selects its supplier with the
 * {@code ?provider=} query param; omitting it uses the {@code hotel.provider} default.
 */
@Component
public class SearchProviderRegistry extends ProviderRegistry<SearchProvider> {

    public SearchProviderRegistry(List<SearchProvider> providers,
                                  @Value("${hotel.provider:hotelbeds}") String defaultProvider) {
        super(providers, SearchProvider::id, ProviderId.fromOrDefault(defaultProvider, ProviderId.HOTELBEDS));
    }
}
