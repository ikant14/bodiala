package com.miirphys.bodiala;

import java.util.Arrays;
import java.util.regex.Pattern;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Test-only: each Spring test context wipes the test schema and then migrates it from scratch, so
 * every context starts on an empty schema built by the real migrations (what {@code create-drop}
 * gave before Flyway). Clean is enabled only in the test properties.
 *
 * <p>This is a plain {@code @Configuration} in the application's package, not a
 * {@code @TestConfiguration}, so component scanning hands it to every {@code @SpringBootTest}.
 *
 * <p>Clean drops everything in the schemas it is given. It refuses unless every configured schema
 * is a {@code bodiala_test*} one, so a misconfigured test can never wipe {@code public} (dev data).
 */
@Configuration(proxyBeanMethods = false)
class CleanTestSchemaBeforeMigrating {

    private static final Pattern TEST_SCHEMA = Pattern.compile("bodiala_test\\w*");

    @Bean
    FlywayMigrationStrategy cleanThenMigrate() {
        return flyway -> {
            String[] schemas = flyway.getConfiguration().getSchemas();
            if (schemas.length == 0 || !Arrays.stream(schemas).allMatch(s -> TEST_SCHEMA.matcher(s).matches())) {
                throw new IllegalStateException("Refusing to clean schemas " + Arrays.toString(schemas)
                        + ": tests may only clean bodiala_test* schemas (set spring.flyway.schemas)");
            }
            flyway.clean();
            flyway.migrate();
        };
    }
}
