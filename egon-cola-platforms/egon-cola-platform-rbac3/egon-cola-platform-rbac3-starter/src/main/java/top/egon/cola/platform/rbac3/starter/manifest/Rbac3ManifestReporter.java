package top.egon.cola.platform.rbac3.starter.manifest;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import top.egon.cola.platform.rbac3.contract.manifest.ResourceManifest;

import java.util.List;
import java.util.Objects;

/**
 * Reports startup manifests using service identity supplied by the application.
 */
public final class Rbac3ManifestReporter implements ApplicationRunner {

    private final List<Rbac3ManifestContributor> contributors;
    private final ManifestTransport transport;
    private final ServiceCredentialSupplier credentialSupplier;

    public Rbac3ManifestReporter(
            List<Rbac3ManifestContributor> contributors,
            ManifestTransport transport,
            ServiceCredentialSupplier credentialSupplier
    ) {
        this.contributors = List.copyOf(contributors);
        this.transport = Objects.requireNonNull(transport, "transport");
        this.credentialSupplier = Objects.requireNonNull(
                credentialSupplier, "credentialSupplier");
    }

    @Override
    public void run(ApplicationArguments args) {
        ServiceCredential credential = credentialSupplier.get();
        contributors.stream()
                .map(Rbac3ManifestContributor::contribute)
                .forEach(manifest -> transport.report(manifest, credential));
    }

    @FunctionalInterface
    public interface ManifestTransport {
        void report(ResourceManifest manifest, ServiceCredential credential);
    }

    @FunctionalInterface
    public interface ServiceCredentialSupplier {
        ServiceCredential get();
    }

    public record ServiceCredential(String clientId, String secret) {
        public ServiceCredential {
            clientId = required(clientId, "clientId");
            secret = required(secret, "secret");
        }

        private static String required(String value, String name) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException(name + " is required");
            }
            return value.trim();
        }
    }
}
