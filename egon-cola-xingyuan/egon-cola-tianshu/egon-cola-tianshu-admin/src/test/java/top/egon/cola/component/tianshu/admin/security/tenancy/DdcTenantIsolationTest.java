package top.egon.cola.component.tianshu.admin.security.tenancy;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import top.egon.cola.component.tianshu.admin.config.DdcAdminProperties;
import top.egon.cola.component.tianshu.admin.security.management.DdcAdminAuthenticationEntryPoint;
import top.egon.cola.platform.tianquan.shoubing.contract.AuthenticationContext;
import top.egon.cola.platform.tianquan.shoubing.contract.IdentityPrincipal;
import top.egon.cola.platform.tianquan.jianshen.contract.authorization.SystemAuthorizationSnapshot;
import top.egon.cola.platform.tianquan.jianshen.starter.security.Rbac3AuthenticationToken;
import top.egon.cola.platform.tianquan.jianshen.starter.security.Rbac3UserDetails;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DdcTenantIsolationTest {
    @Test
    void tenantContextStillRestoresWithOAuthScopedUsers() throws Exception {
        var base = authentication("tenant-a", "admin-a");
        var scoped = new Rbac3AuthenticationToken(base.getPrincipal(), List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("SCOPE_TIANSHU_READ")));
        SecurityContextHolder.getContext().setAuthentication(scoped);
        MDC.put("tenantId", "outer-tenant"); MDC.put("userId", "outer-user");
        var denied = new MockHttpServletResponse();
        filter().doFilter(request("/api/v1/tianshu/configs", "foreign-tenant"), denied,
                (incoming, outgoing) -> { throw new AssertionError("foreign expected tenant must not reach a mapper"); });
        assertThat(denied.getStatus()).isEqualTo(403);
        assertThat(MDC.get("tenantId")).isEqualTo("outer-tenant"); assertThat(MDC.get("userId")).isEqualTo("outer-user");
        SecurityContextHolder.getContext().setAuthentication(scoped);
        assertThatThrownBy(() -> filter().doFilter(request("/api/v1/tianshu/configs", "tenant-a"), new MockHttpServletResponse(),
                (incoming, outgoing) -> { assertThat(MDC.get("tenantId")).isEqualTo("101"); throw new IllegalStateException("controlled callback failure"); }))
                .hasMessage("controlled callback failure");
        assertThat(MDC.get("tenantId")).isEqualTo("outer-tenant"); assertThat(MDC.get("userId")).isEqualTo("outer-user");
    }


    private final DdcTenantRegistry registry = registry();
    private final DdcTenantContextExecutor executor = new DdcTenantContextExecutor();

    @AfterEach
    void clearThreadContext() {
        SecurityContextHolder.clearContext();
        MDC.clear();
    }

    @Test
    void mapsVerifiedUserAndHmacCredentialsToOneExactStorageTenant() {
        DdcTenantBindingDTO userBinding = registry.requireByIdentityTenantId("tenant-a");
        DdcCredentialTenantDTO credentialBinding = registry.requireCredentialBinding("runtime-a");

        assertThat(userBinding.getTenantId()).isEqualTo(101L);
        assertThat(credentialBinding.getTenantId()).isEqualTo(101L);
        assertThat(credentialBinding.getNamespacePatterns()).containsExactly("orders-*");
        assertThatThrownBy(() -> registry.requireByIdentityTenantId("unregistered"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> registry.requireByIdentityTenantId("tenant-disabled"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> registry.requireCredentialBinding("unregistered-key"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void bindsMdcFromTheRegisteredUserAndRestoresPriorContextAfterFailure() {
        MDC.put("tenantId", "999");
        MDC.put("userId", "system:outer");
        DdcTenantContextDTO context = new DdcTenantContextDTO(101L, "tenant-a", "user:admin-a");

        assertThatThrownBy(() -> executor.run(context, () -> {
            assertThat(MDC.get("tenantId")).isEqualTo("101");
            assertThat(MDC.get("userId")).isEqualTo("user:admin-a");
            throw new IllegalStateException("callback failure");
        })).hasMessage("callback failure");

        assertThat(MDC.get("tenantId")).isEqualTo("999");
        assertThat(MDC.get("userId")).isEqualTo("system:outer");
    }

    @Test
    void expectedTenantHeaderConfirmsButNeverSelectsTheAuthenticatedTenant() throws Exception {
        DdcTenantContextFilter filter = filter();
        SecurityContextHolder.getContext().setAuthentication(authentication("tenant-a", "admin-a"));
        AtomicReference<String> observedTenant = new AtomicReference<>();
        MockHttpServletRequest accepted = request("/api/v1/tianshu/ai/resources", "tenant-a");
        MockHttpServletResponse acceptedResponse = new MockHttpServletResponse();
        FilterChain acceptedChain = (request, response) -> observedTenant.set(MDC.get("tenantId"));

        filter.doFilter(accepted, acceptedResponse, acceptedChain);

        assertThat(acceptedResponse.getStatus()).isEqualTo(200);
        assertThat(observedTenant.get()).isEqualTo("101");
        assertThat(MDC.get("tenantId")).isNull();

        SecurityContextHolder.getContext().setAuthentication(authentication("tenant-a", "admin-a"));
        MockHttpServletRequest mismatch = request("/api/v1/tianshu/ai/resources", "tenant-b");
        MockHttpServletResponse mismatchResponse = new MockHttpServletResponse();
        filter.doFilter(mismatch, mismatchResponse,
                (request, response) -> { throw new AssertionError("mismatched tenant reached the endpoint"); });

        assertThat(mismatchResponse.getStatus()).isEqualTo(403);
        assertThat(mismatchResponse.getContentAsString()).contains("TIANSHU_ADMIN_TENANT_MISMATCH");

        SecurityContextHolder.getContext().setAuthentication(authentication("tenant/a", "admin-c"));
        MockHttpServletRequest encoded = request(
                "/api/v1/tianshu/ai/resources", "tenant%2Fa");
        MockHttpServletResponse encodedResponse = new MockHttpServletResponse();
        filter.doFilter(encoded, encodedResponse,
                (request, response) -> observedTenant.set(MDC.get("tenantId")));
        assertThat(encodedResponse.getStatus()).isEqualTo(200);
        assertThat(observedTenant.get()).isEqualTo("303");

        SecurityContextHolder.getContext().setAuthentication(authentication("tenant/a", "admin-c"));
        MockHttpServletRequest doubleEncoded = request(
                "/api/v1/tianshu/ai/resources", "tenant%252Fa");
        MockHttpServletResponse doubleEncodedResponse = new MockHttpServletResponse();
        filter.doFilter(doubleEncoded, doubleEncodedResponse,
                (request, response) -> { throw new AssertionError("expected tenant was decoded more than once"); });
        assertThat(doubleEncodedResponse.getStatus()).isEqualTo(403);
    }

    @Test
    void legacyRestMayOmitTheExpectedHeaderButNewAiRoutesRequireIt() throws Exception {
        DdcTenantContextFilter filter = filter();
        SecurityContextHolder.getContext().setAuthentication(authentication("tenant-a", "admin-a"));
        MockHttpServletRequest legacy = request("/api/v1/tianshu/configs", null);
        MockHttpServletResponse legacyResponse = new MockHttpServletResponse();
        filter.doFilter(legacy, legacyResponse, (request, response) -> { });
        assertThat(legacyResponse.getStatus()).isEqualTo(200);

        SecurityContextHolder.getContext().setAuthentication(authentication("tenant-a", "admin-a"));
        MockHttpServletRequest missingAiHeader = request("/api/v1/tianshu/ai/resources", null);
        MockHttpServletResponse missingAiHeaderResponse = new MockHttpServletResponse();
        filter.doFilter(missingAiHeader, missingAiHeaderResponse,
                (request, response) -> { throw new AssertionError("AI route accepted a missing tenant header"); });
        assertThat(missingAiHeaderResponse.getStatus()).isEqualTo(403);
        assertThat(missingAiHeaderResponse.getContentAsString()).contains("TIANSHU_ADMIN_TENANT_MISMATCH");
    }

    private DdcTenantContextFilter filter() {
        return new DdcTenantContextFilter(
                registry,
                executor,
                new DdcAdminAuthenticationEntryPoint(new ObjectMapper()));
    }

    private MockHttpServletRequest request(String uri, String tenantHeader) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
        if (tenantHeader != null) {
            request.addHeader(DdcTenantContextFilter.TENANT_HEADER, tenantHeader);
        }
        return request;
    }

    private Rbac3AuthenticationToken authentication(String tenantId, String subject) {
        Instant now = Instant.parse("2026-09-27T00:00:00Z");
        IdentityPrincipal identity = new IdentityPrincipal(
                subject, tenantId, "token-1", Set.of("tianshu-admin-web"), now,
                now.plusSeconds(900), AuthenticationContext.password());
        SystemAuthorizationSnapshot snapshot = new SystemAuthorizationSnapshot(
                tenantId, subject, "user-101", "tianshu-admin", 1L, 1L,
                List.of("tianshu-admin"), Set.of("TIANSHU_READ"), Map.of(), Map.of(),
                "snapshot-checksum", now, now.plusSeconds(900));
        Rbac3UserDetails principal = new Rbac3UserDetails(identity, snapshot);
        return new Rbac3AuthenticationToken(principal);
    }

    private DdcTenantRegistry registry() {
        DdcTenantBindingDTO enabled = DdcTenantBindingDTO.builder()
                .tenantId(101L)
                .identityTenantId("tenant-a")
                .enabled(true)
                .build();
        DdcTenantBindingDTO disabled = DdcTenantBindingDTO.builder()
                .tenantId(202L)
                .identityTenantId("tenant-disabled")
                .enabled(false)
                .build();
        DdcTenantBindingDTO encodedIdentity = DdcTenantBindingDTO.builder()
                .tenantId(303L)
                .identityTenantId("tenant/a")
                .enabled(true)
                .build();
        DdcCredentialTenantDTO credentialBinding = DdcCredentialTenantDTO.builder()
                .credentialId("runtime-a")
                .tenantId(101L)
                .namespacePatterns(List.of("orders-*"))
                .build();
        DdcTenantAdminProperties tenantProperties = new DdcTenantAdminProperties();
        tenantProperties.setBindings(List.of(enabled, disabled, encodedIdentity));
        tenantProperties.setCredentialBindings(List.of(credentialBinding));
        DdcAdminProperties.Credential configuredCredential = new DdcAdminProperties.Credential();
        configuredCredential.setCredentialId("runtime-a");
        configuredCredential.setAccessKey("runtime-key");
        configuredCredential.setSecret("runtime-secret");
        configuredCredential.setClientType("SDK");
        configuredCredential.setAllowedOperations(List.of("CONFIG_PULL", "AI_READ"));
        DdcAdminProperties adminProperties = new DdcAdminProperties();
        adminProperties.getRpc().setCredentials(List.of(configuredCredential));
        DdcTenantRegistry registry = new DdcTenantRegistry(tenantProperties, adminProperties);
        registry.initialize();
        return registry;
    }
}
