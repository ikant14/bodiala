package com.miirphys.bodiala.security;

import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * The one staff login, bound from {@code bodiala.security.staff.*} (env {@code BODIALA_STAFF_USERNAME} /
 * {@code BODIALA_STAFF_PASSWORD}, e.g. from {@code .env}). Staff call the staff routes in
 * {@link RouteMatrix} with HTTP Basic.
 *
 * <p>The password is either a plain password of at least {@value #MIN_PASSWORD_LENGTH} characters,
 * hashed with bcrypt at startup, or an already-encoded Spring Security hash with a supported id
 * ({@link #HASH_IDS}), such as {@code {bcrypt}$2a$10$...}. Any other {@code {id}} prefix fails startup;
 * a plain password that really starts with a brace goes after a {@code {noop}} prefix. A blank
 * password means there is no staff user, so every staff route returns 401 (fail closed).
 *
 * <p>Phase 1 (local) only. Before staff routes reach a public network they should move to an identity
 * provider with MFA, on an internal route (SEC-1 "What").
 */
@ConfigurationProperties(prefix = "bodiala.security.staff")
public record StaffProperties(@DefaultValue("staff") String username, @DefaultValue("") String password) {

    public static final int MIN_PASSWORD_LENGTH = 12;

    public static final String ROLE = "STAFF";

    /**
     * Hash ids accepted for an already-encoded password: the strong ones that
     * {@code PasswordEncoderFactories.createDelegatingPasswordEncoder()} registers. Its legacy ids
     * (MD4, MD5, SHA-1, SHA-256, sha256, ldap) are refused.
     */
    static final Set<String> HASH_IDS = Set.of("bcrypt", "argon2", "argon2@SpringSecurity_v5_8", "pbkdf2",
            "pbkdf2@SpringSecurity_v5_8", "scrypt", "scrypt@SpringSecurity_v5_8");

    /** A leading {@code {id}}, as the {@code DelegatingPasswordEncoder} reads it. */
    private static final Pattern ID_PREFIX = Pattern.compile("^\\{([^{}]*)}");

    /** The shape {@code BCryptPasswordEncoder} accepts; anything else would just never match. */
    private static final Pattern BCRYPT_HASH = Pattern.compile("\\$2[aby]?\\$\\d\\d\\$[./0-9A-Za-z]{53}");

    /** The configured staff user, or empty when no password is set. */
    public Optional<UserDetails> toUser(PasswordEncoder encoder) {
        if (password == null || password.isBlank()) {
            return Optional.empty();
        }
        if (username == null || username.isBlank()) {
            throw new IllegalStateException("bodiala.security.staff.username must not be blank when a staff "
                    + "password is set.");
        }
        return Optional.of(User.withUsername(username)
                .password(encoded(encoder))
                .roles(ROLE)
                .build());
    }

    /**
     * Turns the configured value into what the {@code DelegatingPasswordEncoder} stores, or fails at
     * startup. Anything that would only fail at the first login is rejected here instead.
     */
    private String encoded(PasswordEncoder encoder) {
        Matcher prefix = ID_PREFIX.matcher(password);
        String id = prefix.find() ? prefix.group(1) : null;
        if (id == null || id.equals("noop")) {
            String plain = id == null ? password : password.substring(prefix.end());
            if (plain.length() < MIN_PASSWORD_LENGTH) {
                throw invalid("must be at least " + MIN_PASSWORD_LENGTH + " characters, or a {bcrypt} hash");
            }
            return encoder.encode(plain);
        }
        // Don't echo the id: if this was meant as a plain password, it is part of the secret.
        if (!HASH_IDS.contains(id)) {
            throw invalid("starts with a {...} prefix that is not a supported hash id. Use a {bcrypt}, {argon2}, "
                    + "{pbkdf2} or {scrypt} hash, or put {noop} in front of a plain password that starts with '{'");
        }
        if (id.equals("bcrypt") && !BCRYPT_HASH.matcher(password.substring(prefix.end())).matches()) {
            throw invalid("is not a well-formed {bcrypt} hash");
        }
        try {
            encoder.matches("", password); // e.g. {argon2}/{scrypt} need BouncyCastle on the classpath
        } catch (RuntimeException | LinkageError e) {
            throw invalid("is a {" + id + "} hash this app cannot check (" + e + ")");
        }
        return password;
    }

    private static IllegalStateException invalid(String problem) {
        return new IllegalStateException("The staff password (bodiala.security.staff.password / "
                + "BODIALA_STAFF_PASSWORD) " + problem + ".");
    }

    /** Never print the password (records include every component in {@code toString} by default). */
    @Override
    public String toString() {
        return "StaffProperties[username=" + username + ", password=" + (password.isBlank() ? "<blank>" : "<set>")
                + "]";
    }
}
