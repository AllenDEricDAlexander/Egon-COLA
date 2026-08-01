package top.egon.cola.platform.rbac3.admin.bootstrap.cli;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * One-shot platform administrator bootstrap command.
 */
public final class Rbac3PlatformAdminBootstrapCli {

    private static final String COMMAND = "bootstrap-platform-admin";
    private static final Set<String> ALLOWED_OPTIONS = Set.of(
            "--tenant-code", "--username");

    private final BootstrapPort bootstrapPort;

    public Rbac3PlatformAdminBootstrapCli(BootstrapPort bootstrapPort) {
        this.bootstrapPort = Objects.requireNonNull(bootstrapPort, "bootstrapPort");
    }

    public int run(String[] arguments, InputStream passwordInput) {
        Objects.requireNonNull(arguments, "arguments");
        Objects.requireNonNull(passwordInput, "passwordInput");
        Map<String, String> options = parse(arguments);
        char[] password = readPassword(passwordInput);
        try {
            bootstrapPort.bootstrap(
                    required(options, "--tenant-code"),
                    required(options, "--username"),
                    password);
            return 0;
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    private static Map<String, String> parse(String[] arguments) {
        if (arguments.length == 0 || !COMMAND.equals(arguments[0])) {
            throw new IllegalArgumentException("expected command " + COMMAND);
        }
        Map<String, String> options = new LinkedHashMap<>();
        for (int index = 1; index < arguments.length; index += 2) {
            String option = arguments[index];
            if ("--password".equals(option) || option.startsWith("--password=")) {
                throw new IllegalArgumentException("password must not be supplied as an argument");
            }
            if (!ALLOWED_OPTIONS.contains(option)) {
                throw new IllegalArgumentException("unsupported option " + option);
            }
            if (index + 1 >= arguments.length || !option.startsWith("--")) {
                throw new IllegalArgumentException("invalid bootstrap argument list");
            }
            if (options.putIfAbsent(option, arguments[index + 1]) != null) {
                throw new IllegalArgumentException("duplicate option " + option);
            }
        }
        return options;
    }

    private static char[] readPassword(InputStream input) {
        try {
            InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8);
            char[] buffer = new char[65];
            int length = 0;
            int value;
            while ((value = reader.read()) >= 0 && value != '\n' && value != '\r') {
                if (length == buffer.length) {
                    Arrays.fill(buffer, '\0');
                    throw new IllegalArgumentException("password must not exceed 64 characters");
                }
                buffer[length++] = (char) value;
            }
            if (length < 12) {
                Arrays.fill(buffer, '\0');
                throw new IllegalArgumentException(
                        "password must contain 12 to 64 characters");
            }
            char[] password = Arrays.copyOf(buffer, length);
            Arrays.fill(buffer, '\0');
            return password;
        } catch (IOException exception) {
            throw new IllegalStateException("unable to read bootstrap password", exception);
        }
    }

    private static String required(Map<String, String> options, String name) {
        String value = options.get(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value.trim();
    }

    @FunctionalInterface
    public interface BootstrapPort {

        void bootstrap(String tenantCode, String username, char[] password);
    }
}
