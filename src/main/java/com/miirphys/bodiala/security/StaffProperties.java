package com.miirphys.bodiala.security;

import java.util.Optional;
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
 * hashed with bcrypt at startup, or an already-encoded Spring Security hash such as
 * {@code {bcrypt}$2a$10$...}. A blank password means there is no staff user, so every staff route
 * returns 401 (fail closed).
 *
 * <p>Phase 1 (local) only. Before staff routes reach a public network they should move to an identity
 * provider with MFA, on an internal route (SEC-1 "What").
 */
@ConfigurationProperties(prefix = "bodiala.security.staff")
public record StaffProperties(@DefaultValue("staff") String username, @DefaultValue("") String password) {

    public static final int MIN_PASSWORD_LENGTH = 12;

    public static final String ROLE = "STAFF";

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

    private String encoded(PasswordEncoder encoder) {
        String plain = password;
        if (plain.startsWith("{noop}")) {
            plain = plain.substring("{noop}".length());
        } else if (plain.matches("^\\{[A-Za-z0-9@_.-]+}.+")) {
            return plain; // already an encoded hash, e.g. {bcrypt}$2a$10$...
        }
        if (plain.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalStateException("The staff password (bodiala.security.staff.password / "
                    + "BODIALA_STAFF_PASSWORD) must be at least " + MIN_PASSWORD_LENGTH
                    + " characters, or a {bcrypt} hash.");
        }
        return encoder.encode(plain);
    }

    /** Never print the password (records include every component in {@code toString} by default). */
    @Override
    public String toString() {
        return "StaffProperties[username=" + username + ", password=" + (password.isBlank() ? "<blank>" : "<set>")
                + "]";
    }
}
