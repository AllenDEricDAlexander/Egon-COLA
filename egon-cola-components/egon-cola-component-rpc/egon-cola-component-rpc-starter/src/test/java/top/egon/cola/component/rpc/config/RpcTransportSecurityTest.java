package top.egon.cola.component.rpc.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import top.egon.cola.component.rpc.consumer.RpcConsumerChannelFactory;
import top.egon.cola.component.rpc.consumer.RpcGatewayEndpoint;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RpcTransportSecurityTest {

    @TempDir
    Path directory;

    @Test
    void rejectsImplicitPlaintext() {
        IllegalArgumentException failure = assertThrows(
                IllegalArgumentException.class,
                () -> new RpcTransportSecurity(
                        false,
                        false,
                        null,
                        null,
                        null
                )
        );
        assertTrue(failure.getMessage().contains("explicit development mode"));
    }

    @Test
    void rejectsPlaintextRegistryEndpointWhenMtlsIsConfigured()
            throws IOException {
        Path certificate = Files.createFile(directory.resolve("client.crt"));
        Path privateKey = Files.createFile(directory.resolve("client.key"));
        Path trust = Files.createFile(directory.resolve("ca.crt"));
        RpcConsumerChannelFactory channels = new RpcConsumerChannelFactory(
                new RpcTransportSecurity(
                        true,
                        false,
                        certificate.toString(),
                        privateKey.toString(),
                        trust.toString()
                )
        );
        RpcGatewayEndpoint endpoint = new RpcGatewayEndpoint(
                "engine-1",
                "lease-1",
                "127.0.0.1",
                19090,
                false,
                Instant.now().plusSeconds(30)
        );

        IllegalStateException failure = assertThrows(
                IllegalStateException.class,
                () -> channels.create(endpoint)
        );
        assertTrue(failure.getMessage().contains("plaintext"));
    }
}
