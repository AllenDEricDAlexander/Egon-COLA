package top.egon.cola.platform.rbac3.admin.bootstrap.controller.cli;

import top.egon.cola.platform.rbac3.admin.bootstrap.service.PlatformAdminBootstrapService;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Bootstraps RBAC membership; credentials are created and managed by IdP.
 */
public final class Rbac3PlatformAdminBootstrapCli {

    private static final String COMMAND = "bootstrap-platform-admin";
    private static final Set<String> ALLOWED_OPTIONS = Set.of("--tenant-code", "--identity-sub");

    private final PlatformAdminBootstrapService bootstrapPort;

    public Rbac3PlatformAdminBootstrapCli(PlatformAdminBootstrapService bootstrapPort) {
        this.bootstrapPort = Objects.requireNonNull(bootstrapPort, "bootstrapPort");
    }

    public int run(String[] arguments) {
        Objects.requireNonNull(arguments, "arguments");
        Map<String, String> options = parse(arguments);
        bootstrapPort.bootstrap(required(options, "--tenant-code"),
                required(options, "--identity-sub"));
        return 0;
    }

    private static Map<String, String> parse(String[] arguments) {
        if (arguments.length == 0 || !COMMAND.equals(arguments[0])) {
            throw new IllegalArgumentException("expected command " + COMMAND);
        }
        Map<String, String> options = new LinkedHashMap<>();
        if ((arguments.length - 1) % 2 != 0) {
            throw new IllegalArgumentException("invalid bootstrap argument list");
        }
        for (int index = 1; index < arguments.length; index += 2) {
            String option = arguments[index];
            if (!ALLOWED_OPTIONS.contains(option)) {
                throw new IllegalArgumentException("unsupported option " + option);
            }
            if (options.putIfAbsent(option, arguments[index + 1]) != null) {
                throw new IllegalArgumentException("duplicate option " + option);
            }
        }
        return options;
    }

    private static String required(Map<String, String> options, String name) {
        String value = options.get(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value.trim();
    }
}
