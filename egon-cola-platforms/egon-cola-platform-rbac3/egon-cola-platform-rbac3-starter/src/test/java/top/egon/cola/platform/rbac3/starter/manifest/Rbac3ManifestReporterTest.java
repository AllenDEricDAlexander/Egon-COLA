package top.egon.cola.platform.rbac3.starter.manifest;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.rbac3.contract.manifest.ResourceManifest;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class Rbac3ManifestReporterTest {

    @Test
    void reportsWithServiceCredentialInsteadOfUserToken() throws Exception {
        ResourceManifest manifest = new ResourceManifest(
                "1", "finance", "Finance", "1.0.0", "build-1", 1L,
                Instant.parse("2026-07-30T08:00:00Z"), "sha256:manifest",
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
        AtomicReference<Rbac3ManifestReporter.ServiceCredential> credential =
                new AtomicReference<>();
        Rbac3ManifestReporter reporter = new Rbac3ManifestReporter(
                List.of(() -> manifest),
                (reported, serviceCredential) -> credential.set(serviceCredential),
                () -> new Rbac3ManifestReporter.ServiceCredential(
                        "fixture-provider", "service-secret"));

        reporter.run(null);

        assertThat(credential.get().clientId()).isEqualTo("fixture-provider");
    }
}
