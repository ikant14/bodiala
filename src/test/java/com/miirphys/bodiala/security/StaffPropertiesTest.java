package com.miirphys.bodiala.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

class StaffPropertiesTest {

    private final PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();

    @Test
    void aBlankPasswordMeansNoStaffUser() {
        assertThat(new StaffProperties("staff", "").toUser(encoder)).isEmpty();
        assertThat(new StaffProperties("staff", "   ").toUser(encoder)).isEmpty();
    }

    @Test
    void aPlainPasswordIsHashedAndCarriesTheStaffRole() {
        UserDetails user = new StaffProperties("ops", "correct-horse-battery").toUser(encoder).orElseThrow();

        assertThat(user.getUsername()).isEqualTo("ops");
        assertThat(user.getPassword()).startsWith("{bcrypt}").doesNotContain("correct-horse-battery");
        assertThat(encoder.matches("correct-horse-battery", user.getPassword())).isTrue();
        assertThat(user.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_STAFF");
    }

    @Test
    void anEncodedHashIsUsedAsIs() {
        String hash = encoder.encode("correct-horse-battery");

        UserDetails user = new StaffProperties("staff", hash).toUser(encoder).orElseThrow();

        assertThat(user.getPassword()).isEqualTo(hash);
    }

    @Test
    void aShortPasswordFailsStartup() {
        assertThatThrownBy(() -> new StaffProperties("staff", "short").toUser(encoder))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 12 characters");
        // {noop} does not get around the length rule.
        assertThatThrownBy(() -> new StaffProperties("staff", "{noop}short").toUser(encoder))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void aPasswordWithoutAUsernameFailsStartup() {
        assertThatThrownBy(() -> new StaffProperties(" ", "correct-horse-battery").toUser(encoder))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void toStringNeverShowsThePassword() {
        assertThat(new StaffProperties("staff", "correct-horse-battery").toString())
                .doesNotContain("correct-horse-battery");
    }
}
