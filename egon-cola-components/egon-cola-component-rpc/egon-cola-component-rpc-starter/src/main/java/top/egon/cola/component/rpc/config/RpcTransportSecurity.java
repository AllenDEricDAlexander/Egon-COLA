package top.egon.cola.component.rpc.config;

import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.netty.handler.ssl.ClientAuth;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;

import javax.net.ssl.SSLException;
import java.nio.file.Files;
import java.nio.file.Path;

public record RpcTransportSecurity(
        boolean enabled,
        boolean developmentPlaintext,
        String certificateChainPath,
        String privateKeyPath,
        String trustCertificateCollectionPath
) {

    public RpcTransportSecurity {
        if (!enabled) {
            if (!developmentPlaintext) {
                throw new IllegalArgumentException(
                        "RPC plaintext requires explicit development mode"
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

    public static RpcTransportSecurity developmentPlaintextConfig() {
        return new RpcTransportSecurity(
                false,
                true,
                null,
                null,
                null
        );
    }

    public SslContext serverContext() {
        requireEnabled();
        try {
            return GrpcSslContexts.forServer(
                            Path.of(certificateChainPath).toFile(),
                            Path.of(privateKeyPath).toFile()
                    )
                    .trustManager(
                            Path.of(trustCertificateCollectionPath).toFile()
                    )
                    .clientAuth(ClientAuth.REQUIRE)
                    .build();
        } catch (SSLException failure) {
            throw new IllegalStateException(
                    "failed to create RPC server mTLS context",
                    failure
            );
        }
    }

    public SslContext clientContext() {
        requireEnabled();
        try {
            return GrpcSslContexts.forClient()
                    .trustManager(
                            Path.of(trustCertificateCollectionPath).toFile()
                    )
                    .keyManager(
                            Path.of(certificateChainPath).toFile(),
                            Path.of(privateKeyPath).toFile()
                    )
                    .build();
        } catch (SSLException failure) {
            throw new IllegalStateException(
                    "failed to create RPC client mTLS context",
                    failure
            );
        }
    }

    private void requireEnabled() {
        if (!enabled) {
            throw new IllegalStateException("RPC TLS is not enabled");
        }
    }

    private static void requireReadable(String path, String description) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException(
                    "RPC " + description + " path is required"
            );
        }
        Path file = Path.of(path);
        if (!Files.isRegularFile(file) || !Files.isReadable(file)) {
            throw new IllegalArgumentException(
                    "RPC " + description + " file is not readable: " + file
            );
        }
    }
}
