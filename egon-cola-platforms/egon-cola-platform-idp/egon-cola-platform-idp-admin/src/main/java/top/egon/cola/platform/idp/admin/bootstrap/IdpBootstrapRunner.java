package top.egon.cola.platform.idp.admin.bootstrap;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

public final class IdpBootstrapRunner {

    private static final String ADMIN_ARGUMENT = "--idp-bootstrap-admin=";
    private static final String PASSWORD_ENVIRONMENT =
            "IDP_BOOTSTRAP_PASSWORD";

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
        String secret = environment.get(PASSWORD_ENVIRONMENT);
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    PASSWORD_ENVIRONMENT + " is required for bootstrap"
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
