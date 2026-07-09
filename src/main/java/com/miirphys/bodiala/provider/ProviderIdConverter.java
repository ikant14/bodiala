package com.miirphys.bodiala.provider;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * Binds the {@code ?provider=} query param to {@link ProviderId} case-insensitively, so
 * {@code rezlive}, {@code RezLive} and {@code REZLIVE} all resolve. A blank value binds to
 * {@code null} (the controller then falls back to the default provider); an unknown value fails
 * conversion → HTTP 400. Spring Boot auto-registers {@link Converter} beans into the MVC
 * conversion service.
 */
@Component
public class ProviderIdConverter implements Converter<String, ProviderId> {

    @Override
    public ProviderId convert(String source) {
        return source.isBlank() ? null : ProviderId.from(source);
    }
}
