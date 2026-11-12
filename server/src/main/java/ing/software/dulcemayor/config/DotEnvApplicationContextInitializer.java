package ing.software.dulcemayor.config;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads environment variables from a local .env file if present, without overriding
 * OS-level environment variables or explicit JVM system properties.
 */
public class DotEnvApplicationContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final List<Path> CANDIDATE_PATHS = List.of(
            Paths.get(".env"),
            Paths.get("server/.env"),
            Paths.get("../.env")
    );

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        ConfigurableEnvironment environment = applicationContext.getEnvironment();
        Map<String, Object> envMap = new HashMap<>();

        for (Path path : CANDIDATE_PATHS) {
            if (Files.isRegularFile(path)) {
                loadFile(path, envMap);
                break;
            }
        }

        if (!envMap.isEmpty()) {
            environment.getPropertySources().addLast(new MapPropertySource("dotenvProperties", envMap));
        }
    }

    private void loadFile(Path path, Map<String, Object> envMap) {
        try {
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            for (String rawLine : lines) {
                String line = rawLine.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int eqIdx = line.indexOf('=');
                if (eqIdx <= 0) {
                    continue;
                }
                String key = line.substring(0, eqIdx).trim();
                String value = line.substring(eqIdx + 1).trim();
                if ((value.startsWith("\"") && value.endsWith("\""))
                        || (value.startsWith("'") && value.endsWith("'"))) {
                    if (value.length() >= 2) {
                        value = value.substring(1, value.length() - 1);
                    }
                }
                envMap.putIfAbsent(key, value);
            }
        } catch (IOException ignored) {
            // Optional .env file; ignore read issues
        }
    }
}

