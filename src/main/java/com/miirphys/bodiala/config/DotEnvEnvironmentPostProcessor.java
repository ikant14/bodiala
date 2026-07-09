package com.miirphys.bodiala.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Loads a project-root {@code .env} file (simple {@code KEY=VALUE} lines) into the Spring
 * {@link ConfigurableEnvironment} before the context starts, so the {@code ${REZLIVE_*}}
 * placeholders in {@code application.properties} resolve from it. Spring Boot does not read
 * {@code .env} natively — this is the bridge.
 *
 * <p>The file is optional (absent → no-op) and holds secrets, so it is git-ignored (see
 * {@code .env.example} for the committed template). The source is added with <em>lower</em>
 * precedence than the real OS/process environment, so an actual environment variable always
 * overrides the file — which lets CI or the NAS override without editing it. Point at a
 * different file with {@code -Ddotenv.path=/path/to/.env}.
 *
 * <p>Only key <em>names</em> are logged, never values.
 */
public class DotEnvEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String PROPERTY_SOURCE_NAME = "dotenv";
    private static final String PATH_PROPERTY = "dotenv.path";
    private static final String DEFAULT_PATH = ".env";

    private final Log log;

    public DotEnvEnvironmentPostProcessor(DeferredLogFactory logFactory) {
        this.log = logFactory.getLog(DotEnvEnvironmentPostProcessor.class);
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Path envFile = Path.of(environment.getProperty(PATH_PROPERTY, DEFAULT_PATH));
        if (!Files.isRegularFile(envFile)) {
            log.debug("No .env file at " + envFile.toAbsolutePath() + " — skipping.");
            return;
        }
        Map<String, Object> values = parse(envFile);
        if (values.isEmpty()) {
            return;
        }
        // addLast → lowest precedence, so real environment variables still win over the file.
        environment.getPropertySources().addLast(new MapPropertySource(PROPERTY_SOURCE_NAME, values));
        log.info("Loaded " + values.size() + (values.size() == 1 ? " entry" : " entries")
                + " from " + envFile.toAbsolutePath() + " (keys: " + values.keySet() + ").");
    }

    private Map<String, Object> parse(Path envFile) {
        List<String> lines;
        try {
            lines = Files.readAllLines(envFile, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read " + envFile.toAbsolutePath(), e);
        }
        Map<String, Object> values = new LinkedHashMap<>();
        for (String raw : lines) {
            String line = raw.strip();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            if (line.startsWith("export ")) {                 // tolerate `export KEY=VALUE`
                line = line.substring("export ".length()).strip();
            }
            int eq = line.indexOf('=');
            if (eq <= 0) {                                      // no key before '=' → ignore
                continue;
            }
            String key = line.substring(0, eq).strip();
            String value = unquote(line.substring(eq + 1).strip());
            values.put(key, value);
        }
        return values;
    }

    private static String unquote(String v) {
        if (v.length() >= 2
                && ((v.startsWith("\"") && v.endsWith("\"")) || (v.startsWith("'") && v.endsWith("'")))) {
            return v.substring(1, v.length() - 1);
        }
        return v;
    }
}
