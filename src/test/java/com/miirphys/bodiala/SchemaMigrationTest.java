package com.miirphys.bodiala;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;

/**
 * OPS-1 / KAN-84 acceptance: the schema is owned by the Flyway migrations, and Hibernate's
 * {@code ddl-auto=validate} accepts what they build — both on a clean database and on a database
 * that {@code ddl-auto=update} built before Flyway existed.
 */
@SpringBootTest
class SchemaMigrationTest {

    @Autowired
    Flyway flyway;

    @Autowired
    DataSource dataSource;

    @Autowired
    JdbcTemplate jdbc;

    @Value("${spring.jpa.properties.hibernate.default_schema}")
    String testSchema;

    @Test
    void aCleanSchemaIsBuiltByTheMigrationsAloneAndPassesHibernateValidation() {
        // This context only started because Flyway migrated a just-cleaned schema and ddl-auto=validate accepted it.
        // (Skip the unversioned "schema created" marker Flyway writes when it creates the test schema.)
        List<MigrationInfo> applied = Arrays.stream(flyway.info().applied()).filter(m -> m.getVersion() != null).toList();
        assertThat(applied).isNotEmpty().allSatisfy(m -> {
            assertThat(m.getState()).isEqualTo(MigrationState.SUCCESS);
            assertThat(m.getType().isBaseline()).isFalse();
        });
        assertThat(applied).extracting(m -> m.getVersion().getVersion()).startsWith("1").contains("2026.11.08.1700");
        assertThat(flyway.info().pending()).isEmpty();

        assertThat(columns(testSchema)).contains(
                        "hotel_booking.supplier_booking_ref character varying(255) NO",
                        "hotel_booking.supplier_booking_code character varying(255) YES")
                .noneMatch(c -> c.startsWith("hotel_booking.rez_"));
        assertThat(constraints(testSchema)).contains("hotel_booking.idx_booking_supplier_ref u");
    }

    @Test
    void aDatabaseBuiltByDdlAutoUpdateIsBaselinedAndMigratedWithoutLosingData() throws Exception {
        String legacy = testSchema + "_legacy";
        assertThat(legacy).startsWith("bodiala_test");
        jdbc.execute("DROP SCHEMA IF EXISTS " + legacy + " CASCADE");
        jdbc.execute("CREATE SCHEMA " + legacy);
        try {
            try (Connection c = dataSource.getConnection()) {
                c.createStatement().execute("SET search_path TO " + legacy);
                try {
                    ScriptUtils.executeSqlScript(c, new ClassPathResource("db/legacy/ddl-auto-update-database.sql"));
                } finally {
                    c.createStatement().execute("RESET search_path");
                }
            }

            // Migrate it the way the app does: with the main profile's Flyway settings.
            Properties app = appProperties();
            Flyway.configure()
                    .configuration(flyway.getConfiguration())
                    .schemas(legacy)
                    .defaultSchema(legacy)
                    .baselineOnMigrate(Boolean.parseBoolean(app.getProperty("spring.flyway.baseline-on-migrate")))
                    .baselineVersion(app.getProperty("spring.flyway.baseline-version"))
                    .outOfOrder(Boolean.parseBoolean(app.getProperty("spring.flyway.out-of-order")))
                    .load()
                    .migrate();

            // The baseline was recorded instead of re-running V1; the later migrations ran.
            assertThat(history(legacy)).startsWith("1 BASELINE true").contains("2026.11.08.1700 SQL true");

            // No data lost: every row is still there, under the new column names.
            assertThat(jdbc.queryForList("SELECT agent_ref_no || '|' || supplier_booking_ref || '|'"
                    + " || coalesce(supplier_booking_code, '-') || '|' || status || '|' || price || '|'"
                    + " || coalesce(cancellation_charges, '-') FROM " + legacy + ".hotel_booking ORDER BY id",
                    String.class)).containsExactly(
                    "LEGACY-REF-1|1-1794141284|-|Confirmed|200.40|-",
                    "LEGACY-REF-2|1-1794141285|LEGACY-CODE|Cancelled|180.00|0.0");
            assertThat(jdbc.queryForObject("SELECT convert_from(lo_get(description), 'UTF8') FROM " + legacy
                    + ".hotel WHERE hotel_code = 1067", String.class)).isEqualTo("A grand hotel in Palma.");
            assertThat(jdbc.queryForObject("SELECT convert_from(lo_get(hotel_amenities), 'UTF8') FROM " + legacy
                    + ".property_amenity WHERE hotel_code = 1067", String.class)).isEqualTo("Outdoor pool|Wi-Fi");
            assertThat(jdbc.queryForObject("SELECT count(*) FROM " + legacy + ".country", Integer.class)
                    + jdbc.queryForObject("SELECT count(*) FROM " + legacy + ".city", Integer.class)
                    + jdbc.queryForObject("SELECT count(*) FROM " + legacy + ".hotel_image", Integer.class))
                    .isEqualTo(3);

            // The identity keeps counting from the existing rows.
            assertThat(jdbc.queryForObject("INSERT INTO " + legacy + ".hotel_booking (supplier_booking_ref)"
                    + " VALUES ('1-new') RETURNING id", Long.class)).isEqualTo(3L);

            // And it ends up with exactly the schema a clean database gets (which Hibernate validated above).
            assertThat(columns(legacy)).containsExactlyElementsOf(columns(testSchema));
            assertThat(constraints(legacy)).containsExactlyElementsOf(constraints(testSchema));
            assertThat(indexes(legacy)).containsExactlyElementsOf(indexes(testSchema));
        } finally {
            // Large objects live outside the schema, so unlink ours before dropping it (if the tables got that far).
            for (String[] lob : List.of(new String[] {"hotel", "description"},
                    new String[] {"property_amenity", "hotel_amenities"})) {
                try {
                    jdbc.query("SELECT lo_unlink(" + lob[1] + ") FROM " + legacy + "." + lob[0]
                            + " WHERE " + lob[1] + " IS NOT NULL", rs -> { });
                } catch (DataAccessException tableNeverCreated) {
                    // nothing to unlink
                }
            }
            jdbc.execute("DROP SCHEMA IF EXISTS " + legacy + " CASCADE");
        }
    }

    @Test
    void anOlderTimestampedMigrationThatLandsAfterANewerOneStillRuns() throws Exception {
        String scratch = testSchema + "_order";
        assertThat(scratch).startsWith("bodiala_test");
        jdbc.execute("DROP SCHEMA IF EXISTS " + scratch + " CASCADE");
        try {
            // A slot database already has a newer branch's migration...
            flyway(scratch, false, "classpath:db/out-of-order/later").migrate();

            // ...when an older-timestamped one lands on develop. Strict ordering would refuse to start:
            assertThatThrownBy(() -> flyway(scratch, false,
                    "classpath:db/out-of-order/later", "classpath:db/out-of-order/earlier").migrate())
                    .isInstanceOf(FlywayException.class).hasMessageContaining("2099.01.01.1600");

            // With the app's setting, the older one is applied late instead.
            boolean outOfOrder = Boolean.parseBoolean(appProperties().getProperty("spring.flyway.out-of-order"));
            flyway(scratch, outOfOrder, "classpath:db/out-of-order/later", "classpath:db/out-of-order/earlier")
                    .migrate();

            assertThat(history(scratch)).containsExactly("2099.01.01.1800 SQL true", "2099.01.01.1600 SQL true");
            assertThat(jdbc.queryForList("SELECT tablename FROM pg_tables WHERE schemaname = ?"
                    + " AND tablename <> 'flyway_schema_history' ORDER BY 1", String.class, scratch))
                    .containsExactly("earlier_branch_table", "later_branch_table");
        } finally {
            jdbc.execute("DROP SCHEMA IF EXISTS " + scratch + " CASCADE");
        }
    }

    private Flyway flyway(String schema, boolean outOfOrder, String... locations) {
        return Flyway.configure().dataSource(dataSource).schemas(schema).defaultSchema(schema)
                .locations(locations).outOfOrder(outOfOrder).load();
    }

    /** The main profile's properties (the test classpath shadows src/main's application.properties). */
    private static Properties appProperties() throws java.io.IOException {
        return PropertiesLoaderUtils.loadProperties(new FileSystemResource("src/main/resources/application.properties"));
    }

    private List<String> history(String schema) {
        return jdbc.queryForList("SELECT version || ' ' || type || ' ' || success FROM " + schema
                + ".flyway_schema_history WHERE version IS NOT NULL ORDER BY installed_rank", String.class);
    }

    private List<String> columns(String schema) {
        return jdbc.queryForList("""
                SELECT table_name || '.' || column_name || ' ' || data_type
                       || coalesce('(' || character_maximum_length || ')', '') || ' ' || is_nullable
                       || CASE WHEN is_identity = 'YES' THEN ' identity' ELSE '' END
                FROM information_schema.columns
                WHERE table_schema = ? AND table_name <> 'flyway_schema_history'
                ORDER BY 1""", String.class, schema);
    }

    private List<String> constraints(String schema) {
        return jdbc.queryForList("""
                SELECT cl.relname || '.' || con.conname || ' ' || con.contype::text
                FROM pg_constraint con
                JOIN pg_class cl ON cl.oid = con.conrelid
                JOIN pg_namespace n ON n.oid = cl.relnamespace
                WHERE n.nspname = ? AND cl.relname <> 'flyway_schema_history'
                ORDER BY 1""", String.class, schema);
    }

    private List<String> indexes(String schema) {
        return jdbc.queryForList("""
                SELECT tablename || '.' || indexname FROM pg_indexes
                WHERE schemaname = ? AND tablename <> 'flyway_schema_history'
                ORDER BY 1""", String.class, schema);
    }
}
