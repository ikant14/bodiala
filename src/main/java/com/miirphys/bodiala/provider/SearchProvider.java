package com.miirphys.bodiala.provider;

import com.miirphys.bodiala.provider.model.SearchResult;
import com.miirphys.bodiala.search.DestinationSearchRequest;
import com.miirphys.bodiala.search.HotelIdsSearchRequest;

/**
 * Live hotel-availability search, abstracted over the supplier. All implementations are loaded and
 * indexed by {@link SearchProviderRegistry}; a request picks one via the {@code ?provider=} query
 * param (defaulting to {@code hotel.provider}). Each supplier maps its native response into the
 * neutral {@link SearchResult}.
 */
public interface SearchProvider {

    /** Which supplier this implementation talks to. */
    ProviderId id();

    /** Whether this provider has the credentials it needs to make live calls. */
    boolean isConfigured();

    SearchResult searchByDestination(DestinationSearchRequest request);

    SearchResult searchByHotelIds(HotelIdsSearchRequest request);
}
