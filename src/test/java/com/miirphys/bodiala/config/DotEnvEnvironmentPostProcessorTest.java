package com.miirphys.bodiala.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.logging.DeferredLog;
import org.springframework.core.env.StandardEnvironment;

/** Verifies the {@code .env} loader: parsing, precedence, and the missing-file no-op. */
class DotEnvEnvironmentPostProcessorTest {

    // DeferredLogFactory is a functional interface (getLog(Supplier<Log>)); a throwaway log is fine here.
    private final DotEnvEnvironmentPostProcessor processor =
            new DotEnvEnvironmentPostProcessor(destination -> new DeferredLog());

    @Test
    void loadsKeyValuePairsSoPlaceholdersCanResolve(@TempDir Path dir) throws Exception {
        Path env = dir.resolve(".env");
        Files.writeString(env, """
                # a comment line
                REZLIVE_API_KEY=abc123
                REZLIVE_AGENT_CODE = AG-01
                REZLIVE_USER_NAME="quoted user"
                export REZLIVE_BASE_URL=http://example/action

                NO_EQUALS_SIGN_IS_IGNORED
                """);

        StandardEnvironment environment = new StandardEnvironment();
        System.setProperty("dotenv.path", env.toString());
        try {
            processor.postProcessEnvironment(environment, null);
        } finally {
            System.clearProperty("dotenv.path");
        }

        assertThat(environment.getProperty("REZLIVE_API_KEY")).isEqualTo("abc123");
        assertThat(environment.getProperty("REZLIVE_AGENT_CODE")).isEqualTo("AG-01");   // trims spaces
        assertThat(environment.getProperty("REZLIVE_USER_NAME")).isEqualTo("quoted user"); // strips quotes
        assertThat(environment.getProperty("REZLIVE_BASE_URL")).isEqualTo("http://example/action"); // export prefix
        assertThat(environment.getProperty("NO_EQUALS_SIGN_IS_IGNORED")).isNull();
    }

    @Test
    void realEnvironmentVariablesTakePrecedenceOverTheFile(@TempDir Path dir) throws Exception {
        // Pick a key that actually exists in the process environment so we can assert the file loses.
        String existingKey = System.getenv().keySet().stream().findFirst().orElse(null);
        org.junit.jupiter.api.Assumptions.assumeTrue(existingKey != null, "no env vars to test precedence");
        String realValue = System.getenv(existingKey);

        Path env = dir.resolve(".env");
        Files.writeString(env, existingKey + "=dotenv_should_lose\n");

        StandardEnvironment environment = new StandardEnvironment();
        System.setProperty("dotenv.path", env.toString());
        try {
            processor.postProcessEnvironment(environment, null);
        } finally {
            System.clearProperty("dotenv.path");
        }

        assertThat(environment.getProperty(existingKey)).isEqualTo(realValue);
    }

    @Test
    void missingFileIsANoOp(@TempDir Path dir) {
        StandardEnvironment environment = new StandardEnvironment();
        System.setProperty("dotenv.path", dir.resolve("absent.env").toString());
        try {
            processor.postProcessEnvironment(environment, null);
        } finally {
            System.clearProperty("dotenv.path");
        }

        assertThat(environment.getPropertySources().contains("dotenv")).isFalse();
    }
}
