package top.egon.cola.component.yuheng.engine.bootstrap.config;

import jakarta.validation.Validation;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import top.egon.cola.platform.tianquan.shoubing.starter.audit.OAuthUsageObservationProperties;
import top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterProperties;
import top.egon.cola.platform.tianquan.shoubing.starter.observability.IdpSecurityObservationReceiverEnum;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GatewayEngineSecurityObservationStartupCheckTest {
    private static final URI ISSUER = URI.create("https://idp.example/tenants/1001");
    private static final String FIRST_TLS = "egon.cola.component.yuheng.engine.http.public-tls.enabled";
    private static final String SECOND_TLS = "egon.cola.component.yuheng.engine.http.internal-tls.enabled";

    @Test
    void allowsExplicitLocalLoopbackRedisAndPreservesProductionTls() {
        var configuration = new GatewayEngineManagementSecurityConfiguration();
        var usage = usage();
        var identity = identity();
        var environment = environment();
        try (var validators = Validation.buildDefaultValidatorFactory()) {
            for (String endpoint : List.of("redis://127.0.0.1:6379", "redis://localhost:6379", "redis://[::1]:6379")) {
                identity.getRevocation().setPrimaryEndpoint(endpoint);
                assertThatCode(() -> configuration.securityObservationStartupCheck(usage, () -> true, identity, validators.getValidator(), environment))
                        .doesNotThrowAnyException();
            }
            usage.setAllowLocalPlaintext(false);
            environment.setActiveProfiles("production");
            environment.setProperty("DEPLOYMENT_ENV", "production");
            identity.getRevocation().setPrimaryEndpoint("rediss://redis.example:6379");
            assertThatCode(() -> configuration.securityObservationStartupCheck(usage, () -> true, identity, validators.getValidator(), environment))
                    .doesNotThrowAnyException();
        }
    }

    @Test
    void rejectsPlaintextWithoutEveryLocalOptInAndRejectsForeignOrDecoratedEndpoints() {
        var configuration = new GatewayEngineManagementSecurityConfiguration();
        try (var validators = Validation.buildDefaultValidatorFactory()) {
            for (String missing : List.of("flag", "profile", "environment", "environment-absent")) {
                var usage = usage();
                var environment = environment();
                if (missing.equals("flag")) { usage.setAllowLocalPlaintext(false); }
                if (missing.equals("profile")) { environment.setActiveProfiles("operations"); }
                if (missing.equals("environment")) { environment.setProperty("DEPLOYMENT_ENV", "production"); }
                if (missing.equals("environment-absent")) {
                    ((org.springframework.core.env.MapPropertySource) environment.getPropertySources().get("mockProperties"))
                            .getSource().remove("DEPLOYMENT_ENV");
                }
                var identity = identity();
                assertThatThrownBy(() -> configuration.securityObservationStartupCheck(usage, () -> true, identity, validators.getValidator(), environment))
                        .isInstanceOf(IllegalStateException.class);
            }
            for (String endpoint : List.of("redis://redis.example:6379", "redis://127.0.0.2:6379",
                    "redis://localhost.example:6379", "redis://user@127.0.0.1:6379",
                    "redis://127.0.0.1:6379?database=1", "redis://127.0.0.1:6379#fragment")) {
                var identity = identity();
                identity.getRevocation().setPrimaryEndpoint(endpoint);
                assertThatThrownBy(() -> configuration.securityObservationStartupCheck(usage(), () -> true, identity, validators.getValidator(), environment()))
                        .isInstanceOf(IllegalStateException.class);
            }
        }
    }

    @Test
    void localRedisNeverDisablesAuditOrListenerTlsGuards() {
        var configuration = new GatewayEngineManagementSecurityConfiguration();
        try (var validators = Validation.buildDefaultValidatorFactory()) {
            assertThatThrownBy(() -> configuration.securityObservationStartupCheck(usage(), () -> false, identity(), validators.getValidator(), environment()))
                    .hasMessage("YUHENG_SECURITY_OBSERVATION_NOT_READY");
            assertThatThrownBy(() -> configuration.securityObservationStartupCheck(usage().setEnabled(false), () -> true, identity(), validators.getValidator(), environment()))
                    .hasMessage("YUHENG_SECURITY_OBSERVATION_NOT_READY");
            for (String setting : List.of(FIRST_TLS, SECOND_TLS)) {
                var environment = environment().withProperty(setting, "false");
                assertThatThrownBy(() -> configuration.securityObservationStartupCheck(usage(), () -> true, identity(), validators.getValidator(), environment))
                        .hasMessage("YUHENG_SECURITY_OBSERVATION_NOT_READY");
            }
        }
    }

    private static OAuthUsageObservationProperties usage() {
        return new OAuthUsageObservationProperties(true, ISSUER, true, IdpSecurityObservationReceiverEnum.BIZ);
    }

    private static MockEnvironment environment() {
        var environment = new MockEnvironment().withProperty("DEPLOYMENT_ENV", "local")
                .withProperty(FIRST_TLS, "true").withProperty(SECOND_TLS, "true");
        environment.setActiveProfiles("operations", "local");
        return environment;
    }

    private static IdpStarterProperties identity() {
        var properties = new IdpStarterProperties();
        properties.setResourceServerId("controlled-resource");
        properties.setResourceUri(URI.create("https://resource.example"));
        properties.getRevocation().setPrimaryEndpoint("redis://127.0.0.1:6379");
        properties.getRevocation().setEpoch("controlled-epoch");
        var issuer = new IdpStarterProperties.TrustedIssuerSettings();
        issuer.setIssuer(ISSUER.toASCIIString());
        issuer.setJwksUri(ISSUER + "/.well-known/jwks.json");
        issuer.setRealmKey("tenant:1001");
        issuer.setRealmType("TENANT");
        issuer.setTenantId("1001");
        properties.setTrustedIssuers(Map.of("controlled", issuer));
        return properties;
    }
}
