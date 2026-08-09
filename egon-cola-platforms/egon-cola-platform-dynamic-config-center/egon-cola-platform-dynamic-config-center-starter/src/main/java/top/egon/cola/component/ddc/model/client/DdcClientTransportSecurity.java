package top.egon.cola.component.ddc.model.client;

import org.springframework.lang.Nullable;

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

/**
 * DDC 客户端的明文开发模式或双向 TLS 配置。 /
 * Plaintext-development or mutual-TLS configuration for a DDC client.
 *
 * @param enabled                        是否启用 mTLS / whether mTLS is enabled
 * @param developmentPlaintext           是否明确允许仅用于开发的明文传输 / whether development-only plaintext transport is explicitly allowed
 * @param certificateChainPath           PEM 格式客户端 X.509 证书链路径 / path to the PEM client X.509 certificate chain
 * @param privateKeyPath                 PEM 格式客户端 PKCS#8 私钥路径 / path to the PEM client PKCS#8 private key
 * @param trustCertificateCollectionPath PEM 格式受信任 X.509 CA 证书集合路径 / path to the PEM trusted X.509 CA certificate collection
 */
public record DdcClientTransportSecurity(
        boolean enabled,
        boolean developmentPlaintext,
        @Nullable String certificateChainPath,
        @Nullable String privateKeyPath,
        @Nullable String trustCertificateCollectionPath
) {

    /**
     * 内存密钥库中客户端私钥条目使用的空密码。 / Empty password used for the client key entry in the in-memory key store.
     */
    private static final char[] EMPTY_PASSWORD = new char[0];

    /**
     * 校验明文开发模式的显式选择，或校验 mTLS 文件均可读。 /
     * Validates explicit plaintext-development opt-in or readability of all mTLS files.
     *
     * @throws IllegalArgumentException 当未显式允许明文，或任一 mTLS 文件路径无效时 / when plaintext is not explicitly allowed or any mTLS file path is invalid
     */
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

    /**
     * 创建显式启用开发明文传输的配置。 / Creates a configuration explicitly enabling development plaintext transport.
     *
     * @return 开发明文传输配置 / development plaintext transport configuration
     */
    public static DdcClientTransportSecurity developmentPlaintextConfig() {
        return new DdcClientTransportSecurity(
                false,
                true,
                null,
                null,
                null
        );
    }

    /**
     * 从客户端证书链、私钥和受信任 CA 集合构建 mTLS 上下文。 /
     * Builds an mTLS context from the client certificate chain, private key, and trusted CA collection.
     *
     * @return 已初始化的 TLS 上下文 / initialized TLS context
     * @throws IllegalStateException 当 mTLS 未启用或密钥材料无法加载时 / when mTLS is disabled or key material cannot be loaded
     */
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

    /**
     * 读取一个 PEM 文件中的全部 X.509 证书。 / Reads all X.509 certificates from a PEM file.
     *
     * @param path 证书集合文件路径 / certificate-collection file path
     * @return 非空的不可变证书列表 / nonempty immutable certificate list
     * @throws IOException              当证书文件无法读取时 / when the certificate file cannot be read
     * @throws GeneralSecurityException 当证书无法解析或集合为空时 / when certificates cannot be parsed or the collection is empty
     */
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

    /**
     * 读取未加密的 PEM PKCS#8 私钥，并依次尝试受支持的密钥算法。 /
     * Reads an unencrypted PEM PKCS#8 private key, trying each supported key algorithm.
     *
     * @param path 私钥文件路径 / private-key file path
     * @return 已解析的私钥 / parsed private key
     * @throws IOException              当私钥文件无法读取时 / when the private-key file cannot be read
     * @throws GeneralSecurityException 当 PKCS#8 数据无效或算法不受支持时 / when the PKCS#8 data is invalid or its algorithm is unsupported
     */
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

    /**
     * 要求安全材料路径指向可读的普通文件。 / Requires a security-material path to name a readable regular file.
     *
     * @param path        待校验路径文本 / path text to validate
     * @param description 用于错误消息的材料说明 / material description used in error messages
     * @throws IllegalArgumentException 当路径为空或文件不可读时 / when the path is blank or the file is unreadable
     */
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
