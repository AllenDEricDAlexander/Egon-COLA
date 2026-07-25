package top.egon.cola.component.gateway.engine.security;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Instant;

public record GatewayTransportSecurity(
        boolean enabled,
        boolean developmentPlaintext,
        String certificateChainPath,
        String privateKeyPath,
        String trustCertificateCollectionPath,
        boolean clientCertificateRequired
) {

    public GatewayTransportSecurity {
        if (!enabled) {
            if (!developmentPlaintext) {
                throw new IllegalArgumentException(
                        "plaintext transport requires explicit development mode"
                );
            }
        } else {
            requireReadable(certificateChainPath, "certificate chain");
            requireReadable(privateKeyPath, "private key");
            if (clientCertificateRequired) {
                requireReadable(
                        trustCertificateCollectionPath,
                        "trust certificate collection"
                );
            }
        }
    }

    public static GatewayTransportSecurity developmentPlaintextConfig() {
        return new GatewayTransportSecurity(
                false,
                true,
                null,
                null,
                null,
                false
        );
    }

    public Path certificateChainFile() {
        return Path.of(certificateChainPath);
    }

    public Path privateKeyFile() {
        return Path.of(privateKeyPath);
    }

    public Path trustCertificateCollectionFile() {
        return Path.of(trustCertificateCollectionPath);
    }

    public long certificateExpiryEpochSeconds() {
        if (!enabled) {
            return 0;
        }
        try (InputStream input = Files.newInputStream(
                certificateChainFile()
        )) {
            X509Certificate certificate = (X509Certificate)
                    CertificateFactory.getInstance("X.509")
                            .generateCertificate(input);
            return certificate.getNotAfter().toInstant().getEpochSecond();
        } catch (IOException | CertificateException failure) {
            throw new IllegalStateException(
                    "failed to read TLS certificate expiry",
                    failure
            );
        }
    }

    public long secondsUntilExpiry(Instant now) {
        if (!enabled) {
            return 0;
        }
        return certificateExpiryEpochSeconds() - now.getEpochSecond();
    }

    private static void requireReadable(String path, String description) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException(
                    description + " path is required"
            );
        }
        Path file = Path.of(path);
        if (!Files.isRegularFile(file) || !Files.isReadable(file)) {
            throw new IllegalArgumentException(
                    description + " file is not readable: " + file
            );
        }
    }
}
