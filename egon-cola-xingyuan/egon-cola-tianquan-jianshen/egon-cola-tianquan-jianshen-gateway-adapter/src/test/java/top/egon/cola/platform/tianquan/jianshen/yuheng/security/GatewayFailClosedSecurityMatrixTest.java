package top.egon.cola.platform.tianquan.jianshen.yuheng.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import reactor.core.publisher.Mono;
import top.egon.cola.component.yuheng.contract.protocol.AccessZone;
import top.egon.cola.component.yuheng.contract.protocol.GatewayProtocol;
import top.egon.cola.component.yuheng.core.context.GatewayPrincipal;
import top.egon.cola.component.yuheng.core.security.AuthorizationDecision;
import top.egon.cola.component.yuheng.core.security.GatewayAuthContext;
import top.egon.cola.component.yuheng.core.security.SecurityDecision;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GatewayFailClosedSecurityMatrixTest {

    private static final Instant NOW = Instant.parse("2026-07-30T08:00:00Z");
    @TempDir java.nio.file.Path temporary;

    @Test
    void scopeRuntimeFailuresNeverBecomeAuthorizationAllow() {
        var providers = List.of(
                new Rbac3BizAppScopeAuthorizationProvider(
                        ignored -> {
                            throw new IllegalStateException("redis lost");
                        }),
                new Rbac3BizAppScopeAuthorizationProvider(
                        ignored -> {
                            throw new IllegalArgumentException("scope malformed");
                        }));

        for (Rbac3BizAppScopeAuthorizationProvider provider : providers) {
            var decision = Mono.from(provider.authorize(context())).block();
            assertEquals(SecurityDecision.ERROR, decision.decision());
            assertEquals("TIANQUAN_JIANSHEN_SCOPE_RUNTIME_UNAVAILABLE",
                    decision.reason());
        }
    }

    @Test
    void preservesBusinessAndApplicationDenials() {
        var denials = List.of(
                AuthorizationDecision.deny("TIANQUAN_JIANSHEN_BUSINESS_SCOPE_DENIED"),
                AuthorizationDecision.deny("TIANQUAN_JIANSHEN_APPLICATION_SCOPE_DENIED"));

        for (AuthorizationDecision denial : denials) {
            var provider = new Rbac3BizAppScopeAuthorizationProvider(
                    ignored -> denial);

            assertEquals(denial,
                    Mono.from(provider.authorize(context())).block());
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void mandatoryScopeAndFineDenialsShareOneRealStrictProducerResult() throws Exception {
        var validator = new org.springframework.validation.beanvalidation.LocalValidatorFactoryBean(); validator.afterPropertiesSet();
        var redis = org.mockito.Mockito.mock(org.redisson.api.RedissonClient.class);
        org.redisson.api.RStream<String, String> stream = org.mockito.Mockito.mock(org.redisson.api.RStream.class);
        org.mockito.Mockito.doReturn(stream).when(redis).getStream(org.mockito.ArgumentMatchers.eq("egon:oauth:audit:v1"),
                org.mockito.ArgumentMatchers.eq(org.redisson.client.codec.StringCodec.INSTANCE));
        org.mockito.Mockito.when(stream.add(org.mockito.ArgumentMatchers.any(org.redisson.api.stream.StreamAddArgs.class)))
                .thenReturn(new org.redisson.api.StreamMessageId(1, 1));
        var key = temporary.resolve("audit.key"); java.nio.file.Files.writeString(key, "0123456789abcdef0123456789abcdef");
        java.nio.file.Files.setPosixFilePermissions(key, Set.of(java.nio.file.attribute.PosixFilePermission.OWNER_READ, java.nio.file.attribute.PosixFilePermission.OWNER_WRITE));
        var issuer = java.net.URI.create("https://idp.example/tenants/1001"); var resource = java.net.URI.create("https://api.example/payments");
        var producer = new top.egon.cola.platform.tianquan.shoubing.starter.audit.TokenUsageAuditComponent(redis,
                new ObjectMapper().findAndRegisterModules(), new top.egon.cola.platform.tianquan.shoubing.starter.audit.OAuthAuditProperties(
                        "egon:oauth:audit:v1", temporary.resolve("spool"), 1_048_576, key),
                new top.egon.cola.platform.tianquan.shoubing.starter.audit.AuditDeliveryProperties(), java.time.Clock.fixed(NOW, java.time.ZoneOffset.UTC), validator);
        var converter = (top.egon.cola.platform.tianquan.shoubing.starter.security.domain.converter.TokenUsageAuditEventConverter)
                Class.forName("top.egon.cola.platform.tianquan.shoubing.starter.security.domain.converter.TokenUsageAuditEventConverterImpl").getConstructor().newInstance();
        var usage = new top.egon.cola.platform.tianquan.shoubing.starter.audit.OAuthUsageObservationProperties(true, issuer, false,
                top.egon.cola.platform.tianquan.shoubing.starter.observability.IdpSecurityObservationReceiverEnum.BIZ);
        top.egon.cola.platform.tianquan.shoubing.core.observability.IdpSecurityMetricsRecorder metrics = sample -> { };
        var observations = new top.egon.cola.platform.tianquan.shoubing.starter.security.IdpObservedVerificationComponent(producer, converter,
                usage, metrics, java.time.Clock.fixed(NOW, java.time.ZoneOffset.UTC), validator, resource);
        var proof = new top.egon.cola.platform.tianquan.shoubing.starter.security.domain.TokenAuditProofBO(issuer, resource, "a".repeat(64), null, "b".repeat(64));
        var evidence = new top.egon.cola.platform.tianquan.shoubing.yuheng.security.domain.GatewayVerifiedIdentityBO(proof, Set.of("payments:other"));
        var principal = new GatewayPrincipal("operator", "USER", "1001", null, true, java.util.Map.of(), evidence);
        var scoped = new GatewayAuthContext(AccessZone.PUBLIC, GatewayProtocol.HTTP, "payments.read", "payments", "policy", "/payments", "GET",
                Set.of("bearer"), principal, "127.0.0.1", "trace", "request", NOW.plusSeconds(5), "release", java.util.Map.of(), Set.of("payments:read"));
        var scope = new top.egon.cola.platform.tianquan.shoubing.yuheng.security.IdpOAuthScopeAuthorizationProvider(observations, metrics, validator);
        var fine = new Rbac3BizAppScopeAuthorizationProvider(ignored -> AuthorizationDecision.deny("BIZ_DENIED"), observations, metrics, validator);
        java.util.Map<Object, java.util.Map.Entry<Object, Runnable>> states = new java.util.HashMap<>();
        java.util.function.BiFunction<Object, java.util.function.Supplier<java.util.Map.Entry<Object, Runnable>>, Object> owner =
                (type, factory) -> states.computeIfAbsent(type, ignored -> factory.get()).getKey();
        try {
            var decisions = Mono.from(scope.authorize(scoped)).flatMap(first -> Mono.from(fine.authorize(scoped)).map(second -> List.of(first, second)))
                    .contextWrite(context -> context.put(GatewayAuthContext.class, owner)).block();
            assertEquals(SecurityDecision.DENY, decisions.get(0).decision()); assertEquals(SecurityDecision.DENY, decisions.get(1).decision());
            org.mockito.Mockito.verify(stream, org.mockito.Mockito.times(1)).add(org.mockito.ArgumentMatchers.any(org.redisson.api.stream.StreamAddArgs.class));
            var marker = (top.egon.cola.platform.tianquan.shoubing.starter.security.domain.IdpAuthorizationObservationContextBO)
                    states.get(top.egon.cola.platform.tianquan.shoubing.starter.security.domain.IdpAuthorizationObservationContextBO.class).getKey();
            org.junit.jupiter.api.Assertions.assertTrue(marker.isConfirmed());
            states.values().forEach(entry -> entry.getValue().run());
            org.junit.jupiter.api.Assertions.assertFalse(marker.isActive());
            states.clear();
            org.mockito.Mockito.when(stream.add(org.mockito.ArgumentMatchers.any(org.redisson.api.stream.StreamAddArgs.class)))
                    .thenThrow(new org.redisson.client.RedisException("controlled broker outage"));
            var failed = Mono.from(scope.authorize(scoped)).flatMap(first -> Mono.from(fine.authorize(scoped)).map(second -> List.of(first, second)))
                    .contextWrite(context -> context.put(GatewayAuthContext.class, owner)).block();
            assertEquals(SecurityDecision.ERROR, failed.get(0).decision()); assertEquals(SecurityDecision.ERROR, failed.get(1).decision());
            org.mockito.Mockito.verify(stream, org.mockito.Mockito.times(2)).add(org.mockito.ArgumentMatchers.any(org.redisson.api.stream.StreamAddArgs.class));
        } finally { states.values().forEach(entry -> entry.getValue().run()); validator.close(); }
    }

    private static GatewayAuthContext context() {
        return new GatewayAuthContext(
                AccessZone.PUBLIC, GatewayProtocol.HTTP, "operation", "route",
                "policy", "/payments", "POST", Set.of("bearer"),
                new GatewayPrincipal(
                        "user", "USER", "tenant", null, true, java.util.Map.of()),
                "127.0.0.1", "trace", "request", NOW.plusSeconds(5),
                "release");
    }
}
