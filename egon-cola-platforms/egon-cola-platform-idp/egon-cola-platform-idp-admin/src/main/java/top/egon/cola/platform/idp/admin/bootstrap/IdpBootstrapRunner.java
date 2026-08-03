package top.egon.cola.platform.idp.admin.bootstrap;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

public final class IdpBootstrapRunner {

    private static final String ADMIN_ARGUMENT = "--idp-bootstrap-admin=";
    private static final String PASSWORD_ENVIRONMENT =
            "IDP_BOOTSTRAP_PASSWORD";
    private static final String PASSWORD_FILE_ENVIRONMENT =
            "IDP_BOOTSTRAP_PASSWORD_FILE";

    private final BootstrapPort bootstrapPort;

    public IdpBootstrapRunner(BootstrapPort bootstrapPort) {
        this.bootstrapPort = Objects.requireNonNull(
                bootstrapPort,
                "bootstrapPort"
        );
    }

    public int run(String[] arguments, Map<String, String> environment) {
        Objects.requireNonNull(arguments, "arguments");
        Objects.requireNonNull(environment, "environment");
        String username = null;
        for (String argument : arguments) {
            if (argument == null) {
                throw new IllegalArgumentException(
                        "bootstrap argument must not be null"
                );
            }
            if (argument.startsWith("--password")) {
                throw new IllegalArgumentException(
                        "bootstrap password must not be passed as an argument"
                );
            }
            if (!argument.startsWith(ADMIN_ARGUMENT)) {
                continue;
            }
            if (username != null) {
                throw new IllegalArgumentException(
                        "bootstrap administrator may be specified only once"
                );
            }
            username = required(
                    argument.substring(ADMIN_ARGUMENT.length()),
                    "bootstrap username"
            );
        }
        if (username == null) {
            return 0;
        }
        String secret = passwordSecret(environment);
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    PASSWORD_FILE_ENVIRONMENT + " or "
                            + PASSWORD_ENVIRONMENT
                            + " is required for bootstrap"
            );
        }
        char[] password = secret.toCharArray();
        try {
            if (password.length < 12) {
                throw new IllegalArgumentException(
                        "bootstrap password must contain at least 12 characters"
                );
            }
            bootstrapPort.bootstrap(username, password);
            return 0;
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    private String passwordSecret(Map<String, String> environment) {
        String passwordFile = environment.get(PASSWORD_FILE_ENVIRONMENT);
        if (passwordFile == null || passwordFile.isBlank()) {
            return environment.get(PASSWORD_ENVIRONMENT);
        }
        Path path = Path.of(passwordFile.trim());
        if (!Files.isRegularFile(path) || !Files.isReadable(path)) {
            throw new IllegalStateException(
                    PASSWORD_FILE_ENVIRONMENT
                            + " must reference a readable regular file"
            );
        }
        try {
            return Files.readString(path, StandardCharsets.UTF_8)
                    .replaceFirst("[\\r\\n]+$", "");
        } catch (IOException exception) {
            throw new IllegalStateException(
                    PASSWORD_FILE_ENVIRONMENT + " cannot be read",
                    exception
            );
        }
    }

    private String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    public interface BootstrapPort {
        void bootstrap(String username, char[] password);
    }
}
