package top.egon.cola.component.ddc.management.client;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;

public record DdcClientTransportSecurity(
        boolean enabled,
        boolean developmentPlaintext,
        String certificateChainPath,
        String privateKeyPath,
        String trustCertificateCollectionPath
) {

    private static final char[] EMPTY_PASSWORD = new char[0];

    public DdcClientTransportSecurity {
        if (!enabled) {
            if (!developmentPlaintext) {
                throw new IllegalArgumentException(
                        "DDC plaintext requires explicit development mode"
                );
            }
        } else {
            requireReadable(certificateChainPath, "certificate chain");
            requireReadable(privateKeyPath, "private key");
            requireReadable(
                    trustCertificateCollectionPath,
                    "trust certificate collection"
            );
        }
    }

    public static DdcClientTransportSecurity developmentPlaintextConfig() {
        return new DdcClientTransportSecurity(
                false,
                true,
                null,
                null,
                null
        );
    }

    public SSLContext sslContext() {
        if (!enabled) {
            throw new IllegalStateException("DDC mTLS is not enabled");
        }
        try {
            List<X509Certificate> clientChain =
                    certificates(Path.of(certificateChainPath));
            PrivateKey privateKey = privateKey(Path.of(privateKeyPath));
            KeyStore keys = KeyStore.getInstance(KeyStore.getDefaultType());
            keys.load(null, null);
            keys.setKeyEntry(
                    "ddc-client",
                    privateKey,
                    EMPTY_PASSWORD,
                    clientChain.toArray(Certificate[]::new)
            );
            KeyManagerFactory keyManagers = KeyManagerFactory.getInstance(
                    KeyManagerFactory.getDefaultAlgorithm()
            );
            keyManagers.init(keys, EMPTY_PASSWORD);

            KeyStore trust = KeyStore.getInstance(
                    KeyStore.getDefaultType()
            );
            trust.load(null, null);
            List<X509Certificate> trusted = certificates(
                    Path.of(trustCertificateCollectionPath)
            );
            for (int index = 0; index < trusted.size(); index++) {
                trust.setCertificateEntry(
                        "ddc-ca-" + index,
                        trusted.get(index)
                );
            }
            TrustManagerFactory trustManagers =
                    TrustManagerFactory.getInstance(
                            TrustManagerFactory.getDefaultAlgorithm()
                    );
            trustManagers.init(trust);

            SSLContext context = SSLContext.getInstance("TLS");
            context.init(
                    keyManagers.getKeyManagers(),
                    trustManagers.getTrustManagers(),
                    new SecureRandom()
            );
            return context;
        } catch (IOException | GeneralSecurityException failure) {
            throw new IllegalStateException(
                    "failed to create DDC client mTLS context",
                    failure
            );
        }
    }

    private static List<X509Certificate> certificates(Path path)
            throws IOException, GeneralSecurityException {
        try (InputStream input = Files.newInputStream(path)) {
            Collection<? extends Certificate> parsed =
                    CertificateFactory.getInstance("X.509")
                            .generateCertificates(input);
            List<X509Certificate> certificates = new ArrayList<>();
            parsed.forEach(certificate ->
                    certificates.add((X509Certificate) certificate)
            );
            if (certificates.isEmpty()) {
                throw new GeneralSecurityException(
                        "certificate collection is empty"
                );
            }
            return List.copyOf(certificates);
        }
    }

    private static PrivateKey privateKey(Path path)
            throws IOException, GeneralSecurityException {
        String pem = Files.readString(path)
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(
                Base64.getDecoder().decode(pem)
        );
        GeneralSecurityException last = null;
        for (String algorithm : List.of("RSA", "EC", "Ed25519")) {
            try {
                return KeyFactory.getInstance(algorithm)
                        .generatePrivate(spec);
            } catch (GeneralSecurityException failure) {
                last = failure;
            }
        }
        throw new GeneralSecurityException(
                "unsupported PKCS#8 private key",
                last
        );
    }

    private static void requireReadable(String path, String description) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException(
                    "DDC " + description + " path is required"
            );
        }
        Path file = Path.of(path);
        if (!Files.isRegularFile(file) || !Files.isReadable(file)) {
            throw new IllegalArgumentException(
                    "DDC " + description + " file is not readable: " + file
            );
        }
    }
}
