package top.egon.cola.platform.rbac3.starter.field;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.common.desensitize.annotation.Sensitive;
import top.egon.cola.component.common.desensitize.annotation.SensitiveScene;
import top.egon.cola.component.common.desensitize.annotation.SensitiveType;
import top.egon.cola.component.common.desensitize.jackson.SensitiveJacksonModule;
import top.egon.cola.component.common.desensitize.metadata.SensitiveMetadataResolver;
import top.egon.cola.component.common.desensitize.strategy.SensitiveStrategy;
import top.egon.cola.component.common.desensitize.strategy.SensitiveStrategyRegistry;
import top.egon.cola.platform.idp.contract.AuthenticationContext;
import top.egon.cola.platform.idp.contract.IdentityPrincipal;
import top.egon.cola.platform.rbac3.contract.authorization.Decision;
import top.egon.cola.platform.rbac3.contract.authorization.FieldAccessLevel;
import top.egon.cola.platform.rbac3.contract.authorization.FieldPolicyDecision;
import top.egon.cola.platform.rbac3.contract.authorization.SystemAuthorizationSnapshot;
import top.egon.cola.platform.rbac3.starter.security.CurrentRbac3User;
import top.egon.cola.platform.rbac3.starter.security.Rbac3AuthenticationToken;
import top.egon.cola.platform.rbac3.starter.security.Rbac3UserDetails;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class Rbac3FieldPropertyWriterTest {

    private static final Instant NOW = Instant.parse("2026-08-18T02:00:00Z");

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void missingAndNonePoliciesKeepPropertyWithNullValue() throws Exception {
        installUser(fieldPolicy(Map.of(
                "hidden", access(FieldAccessLevel.NONE, null))));

        String json = mapper().writeValueAsString(new Payload(
                "13800138000", "hidden-value", "missing-value", "raw-value"));

        assertThat(json).contains("\"hidden\":null")
                .contains("\"missing\":null")
                .doesNotContain("hidden-value")
                .doesNotContain("missing-value");
    }

    @Test
    void missingSecurityContextFailsClosed() throws Exception {
        String json = mapper().writeValueAsString(new Payload(
                "13800138000", "hidden-value", "missing-value", "raw-value"));

        assertThat(json).contains("\"mobile\":null")
                .contains("\"hidden\":null")
                .contains("\"missing\":null")
                .doesNotContain("13800138000")
                .doesNotContain("hidden-value")
                .doesNotContain("missing-value");
    }

    @Test
    void maskedReadUsesConfiguredStrategyAndReadStillHonorsSensitive() throws Exception {
        installUser(fieldPolicy(Map.of(
                "mobile", access(FieldAccessLevel.MASKED_READ, "MOBILE"),
                "sensitive", access(FieldAccessLevel.READ, null),
                "raw", access(FieldAccessLevel.WRITE, null))));

        String json = mapper().writeValueAsString(new Payload(
                "13800138000", "hidden-value", "missing-value", "raw-value"));

        assertThat(json).contains("\"mobile\":\"138****8000\"")
                .contains("\"sensitive\":\"138****8000\"")
                .contains("\"raw\":\"raw-value\"")
                .doesNotContain("hidden-value");
    }

    @Test
    void maskingFailureFailsClosedWithoutWritingOriginalValue() throws Exception {
        installUser(fieldPolicy(Map.of(
                "mobile", access(FieldAccessLevel.MASKED_READ, "MOBILE"))));
        SensitiveStrategy failing = new SensitiveStrategy() {
            @Override
            public SensitiveType type() {
                return SensitiveType.MOBILE;
            }

            @Override
            public String mask(String value) {
                throw new IllegalStateException("masking unavailable");
            }
        };
        SensitiveStrategyRegistry registry = SensitiveStrategyRegistry.defaults()
                .withOverrides(List.of(failing));

        String json = mapper(registry).writeValueAsString(new Payload(
                "13800138000", "hidden-value", "missing-value", "raw-value"));

        assertThat(json).contains("\"mobile\":null")
                .doesNotContain("13800138000");
    }

    private ObjectMapper mapper() {
        return mapper(SensitiveStrategyRegistry.defaults());
    }

    private ObjectMapper mapper(SensitiveStrategyRegistry registry) {
        return new ObjectMapper()
                .registerModule(new Rbac3FieldJacksonModule(
                        new CurrentRbac3User(), registry))
                .registerModule(new SensitiveJacksonModule(
                        registry, new SensitiveMetadataResolver()));
    }

    private void installUser(FieldPolicyDecision decision) {
        SystemAuthorizationSnapshot snapshot = new SystemAuthorizationSnapshot(
                "tenant-a", "alice-sub", "user-1", "finance", 1L, 2L,
                List.of("role-1"), Set.of("customer:read"), Map.of(),
                Map.of("customer:read:finance:customer", decision),
                "sha256:snapshot", NOW, NOW.plusSeconds(300));
        IdentityPrincipal identity = new IdentityPrincipal(
                "alice-sub", "tenant-a", "access-jti", Set.of("finance"),
                NOW.minusSeconds(30), NOW.plusSeconds(300),
                AuthenticationContext.password());
        SecurityContextHolder.getContext().setAuthentication(
                new Rbac3AuthenticationToken(new Rbac3UserDetails(identity, snapshot)));
    }

    private FieldPolicyDecision fieldPolicy(Map<String, FieldPolicyDecision.FieldAccess> fields) {
        return new FieldPolicyDecision(
                Decision.ALLOW, "FIELD_POLICY_RESOLVED", "tenant-a", "user-1",
                "customer:read", "finance", "customer", fields,
                1L, 2L, List.of(), NOW);
    }

    private FieldPolicyDecision.FieldAccess access(
            FieldAccessLevel level,
            String strategy) {
        return new FieldPolicyDecision.FieldAccess(level, strategy);
    }

    static final class Payload {
        @RBACFieldResource(
                code = "mobile",
                name = "Mobile",
                resourceCode = "customer",
                permission = "customer:read",
                maskingStrategy = SensitiveType.MOBILE)
        private final String mobile;

        @RBACFieldResource(
                code = "hidden",
                name = "Hidden",
                resourceCode = "customer",
                permission = "customer:read",
                maskingStrategy = SensitiveType.FULL)
        private final String hidden;

        @RBACFieldResource(
                code = "missing",
                name = "Missing",
                resourceCode = "customer",
                permission = "customer:read",
                maskingStrategy = SensitiveType.FULL)
        private final String missing;

        @RBACFieldResource(
                code = "sensitive",
                name = "Sensitive",
                resourceCode = "customer",
                permission = "customer:read",
                maskingStrategy = SensitiveType.FULL)
        @Sensitive(type = SensitiveType.MOBILE, scenes = SensitiveScene.RESPONSE)
        private final String sensitive;

        @RBACFieldResource(
                code = "raw",
                name = "Raw",
                resourceCode = "customer",
                permission = "customer:read",
                maskingStrategy = SensitiveType.FULL)
        private final String raw;

        private Payload(String mobile, String hidden, String missing, String raw) {
            this.mobile = mobile;
            this.hidden = hidden;
            this.missing = missing;
            this.sensitive = mobile;
            this.raw = raw;
        }

        public String getMobile() {
            return mobile;
        }

        public String getHidden() {
            return hidden;
        }

        public String getMissing() {
            return missing;
        }

        public String getSensitive() {
            return sensitive;
        }

        public String getRaw() {
            return raw;
        }
    }
}
