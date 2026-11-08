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
    void anUnknownHashIdFailsStartupInsteadOfEveryLogin() {
        // Was treated as an encoded hash: no length check, then "no PasswordEncoder mapped" on each login.
        assertThatThrownBy(() -> new StaffProperties("staff", "{office}short").toUser(encoder))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not a supported hash id")
                .hasMessageNotContaining("office");
        assertThatThrownBy(() -> new StaffProperties("staff", "{office}long-enough-password").toUser(encoder))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new StaffProperties("staff", "{}long-enough-password").toUser(encoder))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void legacyHashIdsAreRefused() {
        for (String id : new String[] {"MD5", "MD4", "SHA-1", "SHA-256", "sha256", "ldap"}) {
            assertThatThrownBy(() -> new StaffProperties("staff", "{" + id + "}5f4dcc3b5aa765d61d8327deb882cf99")
                    .toUser(encoder))
                    .as(id).isInstanceOf(IllegalStateException.class);
        }
    }

    @Test
    void aMalformedBcryptHashFailsStartup() {
        assertThatThrownBy(() -> new StaffProperties("staff", "{bcrypt}not-a-real-hash").toUser(encoder))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("well-formed {bcrypt}");
    }

    @Test
    void aHashThisAppCannotCheckFailsStartup() {
        // E.g. {argon2} without BouncyCastle: the encoder throws instead of answering.
        PasswordEncoder cannotCheck = new PasswordEncoder() {
            @Override
            public String encode(CharSequence raw) {
                return encoder.encode(raw);
            }

            @Override
            public boolean matches(CharSequence raw, String encoded) {
                throw new NoClassDefFoundError("org/bouncycastle/crypto/generators/Argon2BytesGenerator");
            }
        };

        assertThatThrownBy(() -> new StaffProperties("staff", "{argon2}$argon2id$v=19$m=16384,t=2,p=1$c2FsdA$aGFzaA")
                .toUser(cannotCheck))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cannot check");
    }

    @Test
    void noopMarksAPlainPasswordThatStartsWithABrace() {
        UserDetails user = new StaffProperties("staff", "{noop}{office}long-enough").toUser(encoder).orElseThrow();

        assertThat(encoder.matches("{office}long-enough", user.getPassword())).isTrue();
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
