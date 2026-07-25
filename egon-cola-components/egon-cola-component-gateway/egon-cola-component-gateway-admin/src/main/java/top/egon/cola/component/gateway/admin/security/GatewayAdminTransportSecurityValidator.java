package top.egon.cola.component.gateway.admin.security;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Objects;

@Component
public final class GatewayAdminTransportSecurityValidator
        implements ApplicationRunner {

    private static final String PREFIX =
            "gateway.admin.transport-security";

    private final Environment environment;

    public GatewayAdminTransportSecurityValidator(Environment environment) {
        this.environment = Objects.requireNonNull(
                environment,
                "environment"
        );
    }

    @Override
    public void run(ApplicationArguments args) {
        validate(environment, PREFIX);
    }

    static void validate(Environment environment, String prefix) {
        String mode = required(environment, prefix + ".mode")
                .toUpperCase(Locale.ROOT);
        if ("DEVELOPMENT_PLAINTEXT".equals(mode)) {
            return;
        }
        if (!"MTLS".equals(mode)) {
            throw new IllegalStateException(
                    prefix + ".mode must be DEVELOPMENT_PLAINTEXT or MTLS"
            );
        }
        if (!environment.getProperty(
                "server.ssl.enabled",
                Boolean.class,
                false
        )) {
            throw new IllegalStateException(
                    "MTLS requires server.ssl.enabled=true"
            );
        }
        if (!"need".equalsIgnoreCase(environment.getProperty(
                "server.ssl.client-auth",
                ""
        ))) {
            throw new IllegalStateException(
                    "MTLS requires server.ssl.client-auth=need"
            );
        }
        String bundle = required(environment, "server.ssl.bundle");
        required(
                environment,
                "spring.ssl.bundle.pem." + bundle
                        + ".keystore.certificate"
        );
        required(
                environment,
                "spring.ssl.bundle.pem." + bundle
                        + ".keystore.private-key"
        );
        required(
                environment,
                "spring.ssl.bundle.pem." + bundle
                        + ".truststore.certificate"
        );
    }

    private static String required(
            Environment environment,
            String property) {
        String value = environment.getProperty(property);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(property + " is required");
        }
        return value.trim();
    }
}
