package top.egon.cola.component.gateway.admin.interfaces.openapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import top.egon.cola.component.gateway.admin.application.credential.GatewayCredentialStore;
import top.egon.cola.component.gateway.admin.application.credential.GatewaySecretProtector;
import top.egon.cola.component.gateway.admin.application.reporting.GatewayHmacNonceStore;
import top.egon.cola.component.gateway.admin.application.reporting.GatewayReportAuthentication;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayApplicationEntity;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayApplicationRepository;
import top.egon.cola.component.gateway.admin.infrastructure.security.AesGcmGatewaySecretProtector;
import top.egon.cola.component.gateway.contract.reporting.GatewayCanonicalRequest;
import top.egon.cola.component.gateway.contract.reporting.GatewayRequestSigner;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GatewayReportHmacFilterTest {

    @Test
    void springSelectsTheProductionConstructor() {
        new ApplicationContextRunner()
                .withInitializer(context -> context.getBeanFactory()
                        .setConversionService(
                                ApplicationConversionService
                                        .getSharedInstance()
                        ))
                .withBean(
                        GatewayCredentialStore.class,
                        () -> mock(GatewayCredentialStore.class)
                )
                .withBean(
                        GatewayApplicationRepository.class,
                        () -> mock(GatewayApplicationRepository.class)
                )
                .withBean(
                        GatewayHmacNonceStore.class,
                        () -> mock(GatewayHmacNonceStore.class)
                )
                .withBean(
                        ObjectMapper.class,
                        () -> JsonMapper.builder()
                                .findAndAddModules()
                                .build()
                )
                .withBean(GatewayReportHmacFilter.class)
                .run(context -> assertThat(context)
                        .hasSingleBean(GatewayReportHmacFilter.class));
    }

    @Test
    void authenticatesScopeAndRejectsNonceReplay() throws Exception {
        Instant now = Instant.parse("2026-07-25T00:00:00Z");
        String accessKey = "access-key";
        String secret = "secret-key";
        GatewaySecretProtector protector =
                new AesGcmGatewaySecretProtector(new byte[32], "v1");
        GatewaySecretProtector.ProtectedSecret encrypted =
                protector.protect(secret, "app-1:" + accessKey);
        GatewayCredentialStore credentials =
                mock(GatewayCredentialStore.class);
        when(credentials.findByAccessKey(accessKey)).thenReturn(
                Optional.of(new GatewayCredentialStore.CredentialRecord(
                        "credential-1",
                        "app-1",
                        accessKey,
                        encrypted.ciphertext(),
                        encrypted.keyVersion(),
                        "ACTIVE",
                        now.minusSeconds(60),
                        null,
                        now,
                        now
                ))
        );
        GatewayApplicationRepository applications =
                mock(GatewayApplicationRepository.class);
        when(applications.findByIdAndDeletedFalse("app-1")).thenReturn(
                Optional.of(new GatewayApplicationEntity(
                        "app-1",
                        "test-biz",
                        "inventory",
                        "Inventory",
                        "test",
                        "default",
                        null,
                        "tester",
                        now
                ))
        );
        RecordingNonceStore nonces = new RecordingNonceStore();
        GatewayReportHmacFilter filter = new GatewayReportHmacFilter(
                credentials,
                applications,
                nonces,
                protector,
                JsonMapper.builder().findAndAddModules().build(),
                Duration.ofMinutes(5),
                Clock.fixed(now, ZoneOffset.UTC)
        );
        byte[] body = "{}".getBytes(StandardCharsets.UTF_8);
        MockHttpServletRequest request = signed(
                now,
                accessKey,
                secret,
                body
        );
        AtomicReference<GatewayReportAuthentication> observed =
                new AtomicReference<>();
        FilterChain chain = (incoming, response) -> observed.set(
                (GatewayReportAuthentication) incoming.getAttribute(
                        GatewayReportAuthentication.REQUEST_ATTRIBUTE
                )
        );

        filter.doFilter(
                request,
                new MockHttpServletResponse(),
                chain
        );

        assertThat(observed.get().applicationId()).isEqualTo("app-1");

        MockHttpServletResponse replay = new MockHttpServletResponse();
        filter.doFilter(
                signed(now, accessKey, secret, body),
                replay,
                chain
        );
        assertThat(replay.getStatus()).isEqualTo(409);
        assertThat(replay.getContentAsString())
                .contains("GATEWAY_REPORT_REPLAYED");
    }

    private MockHttpServletRequest signed(
            Instant now,
            String accessKey,
            String secret,
            byte[] body) {
        String path =
                "/api/v1/gateway/openapi/interface-definitions/reports";
        String nonce = "nonce-1";
        GatewayCanonicalRequest canonical = new GatewayCanonicalRequest(
                "POST",
                path,
                Map.of(),
                now.toEpochMilli(),
                nonce,
                body
        );
        GatewayRequestSigner signer = new GatewayRequestSigner();
        MockHttpServletRequest request =
                new MockHttpServletRequest("POST", path);
        request.setRequestURI(path);
        request.setContent(body);
        request.addHeader(GatewayRequestSigner.ACCESS_KEY_HEADER, accessKey);
        request.addHeader(
                GatewayRequestSigner.TIMESTAMP_HEADER,
                Long.toString(now.toEpochMilli())
        );
        request.addHeader(GatewayRequestSigner.NONCE_HEADER, nonce);
        request.addHeader(
                GatewayRequestSigner.CONTENT_SHA256_HEADER,
                canonical.contentSha256()
        );
        request.addHeader(
                GatewayRequestSigner.SIGNATURE_HEADER,
                signer.sign(canonical, secret)
        );
        request.addHeader(
                "X-Gateway-Application-Code",
                "inventory"
        );
        return request;
    }

    private static final class RecordingNonceStore
            implements GatewayHmacNonceStore {

        private final Set<String> values = new HashSet<>();

        @Override
        public boolean claim(
                String accessKey,
                String nonce,
                Instant expiresAt,
                Instant now) {
            return values.add(accessKey + ":" + nonce);
        }

        @Override
        public int deleteExpired(Instant now) {
            return 0;
        }
    }
}
