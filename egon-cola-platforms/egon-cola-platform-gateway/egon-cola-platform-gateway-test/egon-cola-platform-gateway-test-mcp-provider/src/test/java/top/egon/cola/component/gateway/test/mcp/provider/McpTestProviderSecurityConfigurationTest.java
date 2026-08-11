package top.egon.cola.component.gateway.test.mcp.provider;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import top.egon.cola.platform.idp.starter.security.IdpBearerAuthenticationFilter;
import top.egon.cola.platform.idp.contract.ServiceIdentityPrincipal;
import top.egon.cola.platform.rbac3.starter.security.Rbac3BearerAuthenticationFilter;

import java.net.URI;
import java.time.Instant;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(McpJobController.class)
@Import(McpTestProviderSecurityConfiguration.class)
class McpTestProviderSecurityConfigurationTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private IdpBearerAuthenticationFilter idpFilter;

    @MockitoBean
    private Rbac3BearerAuthenticationFilter rbac3Filter;

    @BeforeEach
    void authenticateTheReviewedBearerFixture() throws Exception {
        doAnswer(invocation -> {
            HttpServletRequest request = invocation.getArgument(0);
            FilterChain chain = invocation.getArgument(2);
            if ("Bearer provider-resource-token".equals(
                    request.getHeader("Authorization"))) {
                var context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(new TestingAuthenticationToken(
                        "fixture-user",
                        null,
                        "ROLE_USER"
                ));
                SecurityContextHolder.setContext(context);
            } else if (request.getHeader("Authorization") != null
                    && request.getHeader("Authorization").startsWith(
                    "Bearer service-")) {
                boolean allowed = "Bearer service-allowed".equals(
                        request.getHeader("Authorization")
                );
                var context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(new TestingAuthenticationToken(
                        servicePrincipal(allowed),
                        null,
                        "ROLE_SERVICE"
                ));
                SecurityContextHolder.setContext(context);
            }
            try {
                chain.doFilter(
                        invocation.getArgument(0),
                        invocation.getArgument(1)
                );
            } finally {
                SecurityContextHolder.clearContext();
            }
            return null;
        }).when(idpFilter).doFilter(any(), any(), any());
        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(
                    invocation.getArgument(0),
                    invocation.getArgument(1)
            );
            return null;
        }).when(rbac3Filter).doFilter(any(), any(), any());
    }

    @Test
    void requiresAnAuthenticatedIdpResourceToken() throws Exception {
        mvc.perform(get("/api/mcp-fixtures/query"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void permitsTheAuthenticatedMcpFixtureOnly() throws Exception {
        mvc.perform(get("/api/mcp-fixtures/query")
                        .header("Authorization",
                                "Bearer provider-resource-token")
                        .queryParam("prefix", "qa"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0]").value("qa-1"))
                .andExpect(jsonPath("$.items[1]").value("qa-2"));
        verify(rbac3Filter, atLeastOnce()).doFilter(any(), any(), any());

        mvc.perform(get("/unregistered")
                        .header("Authorization",
                                "Bearer provider-resource-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void requiresTheIdpProviderScopeForServicePrincipals() throws Exception {
        mvc.perform(get("/api/mcp-fixtures/query")
                        .header("Authorization", "Bearer service-denied"))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/mcp-fixtures/query")
                        .header("Authorization", "Bearer service-allowed"))
                .andExpect(status().isOk());
    }

    private ServiceIdentityPrincipal servicePrincipal(boolean allowed) {
        Instant issuedAt = Instant.parse("2026-08-11T12:00:00Z");
        return new ServiceIdentityPrincipal(
                "gateway-engine-service",
                "tenant-b",
                "gateway-engine-service",
                "token-id",
                URI.create(
                        "https://api.egon.internal/local/identity/"
                                + "gateway-test-mcp-provider"
                ),
                1L,
                Set.of(allowed
                        ? "mcp:operation:invoke"
                        : "mcp:operation:inspect"),
                "identity",
                "gateway-engine-default",
                "local",
                "gateway-engine-local",
                issuedAt,
                issuedAt.plusSeconds(300)
        );
    }
}
