package com.miirphys.bodiala.provider;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Base registry that indexes the loaded implementations of a provider interface by {@link ProviderId}
 * and resolves one per request.
 *
 * <p>Both suppliers' beans are always in the context now (they're no longer gated by
 * {@code hotel.provider}); a request selects a supplier with the {@code ?provider=} query param, and
 * omitting it falls back to the configured default ({@code hotel.provider}, itself defaulting to
 * {@code rezlive}).
 */
public abstract class ProviderRegistry<T> {

    private final Map<ProviderId, T> byId;
    private final ProviderId defaultId;

    protected ProviderRegistry(List<T> providers, Function<T, ProviderId> idFn, ProviderId defaultId) {
        Map<ProviderId, T> map = new EnumMap<>(ProviderId.class);
        for (T provider : providers) {
            ProviderId id = idFn.apply(provider);
            if (map.putIfAbsent(id, provider) != null) {
                throw new IllegalStateException("Two implementations registered for provider " + id);
            }
        }
        this.byId = map;
        this.defaultId = defaultId;
    }

    /** The supplier used when a request doesn't specify one. */
    public ProviderId defaultId() {
        return defaultId;
    }

    /** All loaded implementations, in {@link ProviderId} declaration order. */
    public List<T> all() {
        return List.copyOf(byId.values());
    }

    /** Resolve the implementation for {@code id}, or the default when {@code id} is {@code null}. */
    public T resolve(ProviderId id) {
        ProviderId target = id != null ? id : defaultId;
        T provider = byId.get(target);
        if (provider == null) {
            throw new IllegalArgumentException("No implementation is registered for provider " + target);
        }
        return provider;
    }
}
