package top.egon.cola.component.gateway.engine.common.security.service;

import top.egon.cola.component.gateway.engine.common.security.domain.GatewayTransportSecurity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import top.egon.cola.component.gateway.engine.http.domain.GatewayHttpEngineProperties;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GatewayTransportSecurityTest {

    @TempDir
    Path directory;

    @Test
    void rejectsImplicitPlaintext() {
        IllegalArgumentException failure = assertThrows(
                IllegalArgumentException.class,
                () -> new GatewayTransportSecurity(
                false,
                false,
                null,
                null,
                null,
                false
        ));
        assertTrue(failure.getMessage().contains("explicit development mode"));
    }

    @Test
    void rejectsUnreadableTlsMaterial() {
        IllegalArgumentException failure = assertThrows(
                IllegalArgumentException.class,
                () -> new GatewayTransportSecurity(
                true,
                false,
                directory.resolve("server.crt").toString(),
                directory.resolve("server.key").toString(),
                directory.resolve("ca.crt").toString(),
                true
        ));
        assertTrue(failure.getMessage().contains("not readable"));
    }

    @Test
    void rejectsInternalTlsWithoutClientCertificateValidation()
            throws IOException {
        Path certificate = Files.createFile(
                directory.resolve("server.crt")
        );
        Path privateKey = Files.createFile(
                directory.resolve("server.key")
        );
        GatewayTransportSecurity serverOnly =
                new GatewayTransportSecurity(
                        true,
                        false,
                        certificate.toString(),
                        privateKey.toString(),
                        null,
                        false
                );
        IllegalArgumentException failure = assertThrows(
                IllegalArgumentException.class,
                () -> new GatewayHttpEngineProperties(
                new GatewayHttpEngineProperties.Listener(
                        true,
                        "127.0.0.1",
                        0
                ),
                new GatewayHttpEngineProperties.Listener(
                        true,
                        "127.0.0.1",
                        0,
                        serverOnly
                ),
                64,
                8192,
                1024,
                java.time.Duration.ofSeconds(30),
                java.time.Duration.ofSeconds(5),
                10,
                10
        ));
        assertTrue(failure.getMessage().contains("client certificate"));
    }
}
