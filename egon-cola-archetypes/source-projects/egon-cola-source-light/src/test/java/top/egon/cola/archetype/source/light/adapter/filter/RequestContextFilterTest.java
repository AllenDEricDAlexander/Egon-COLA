package top.egon.cola.archetype.source.light.adapter.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;
import top.egon.cola.component.common.mybatis.business.EgonColaTenantIdProvider;
import top.egon.cola.platform.tianquan.shoubing.contract.AuthenticationContext;
import top.egon.cola.platform.tianquan.shoubing.contract.IdentityPrincipal;
import top.egon.cola.platform.tianquan.shoubing.starter.security.IdpAuthenticationToken;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Tests the context adapter only; signed credential verification is owned by NativeSecurityContractTest. */
class RequestContextFilterTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RequestContextFilter filter = new RequestContextFilter(objectMapper);

    @AfterEach void clearContext() { MDC.clear(); RequestContextHolder.clear(); SecurityContextHolder.clearContext(); }

    @Test
    void exposesVerifiedContextAndIgnoresAllIdentityHeaders() throws Exception {
        authenticated("verified-actor", "42");
        var request = new MockHttpServletRequest();
        request.addHeader(RequestContextFilter.TENANT_ID_HEADER, "invalid-tenant");
        request.addHeader(RequestContextFilter.OPERATOR_ID_HEADER, "forged-actor");
        request.addHeader(RequestContextFilter.REQUEST_ID_HEADER, "request-1");
        request.setAttribute(TraceIdFilter.TRACE_ID_ATTRIBUTE, "trace-1"); MDC.put("traceId", "trace-1");
        filter.doFilter(request, new MockHttpServletResponse(), (incoming, outgoing) -> {
            assertThat(RequestContextHolder.getRequired()).isEqualTo(new RequestContext("verified-actor", "request-1", "trace-1", 42L));
            assertThat(EgonColaTenantIdProvider.currentTenantId()).isEqualTo(42L);
            assertThat(MDC.get("userId")).isEqualTo("verified-actor");
            assertThat(MDC.get("egon.auth.actor-kind")).isEqualTo("USER");
        });
        assertContextCleared(); assertThat(MDC.get("traceId")).isEqualTo("trace-1");
    }

    @Test
    void restoresExistingContextAndEveryMdcKeyAfterDownstreamFailure() {
        authenticated("verified-actor", "42");
        var previous = new RequestContext("previous-actor", "previous-request", "previous-trace", 99L);
        RequestContextHolder.set(previous); MDC.setContextMap(Map.of("tenantId", "99", "userId", "previous-actor", "custom", "keep"));
        var before = MDC.getCopyOfContextMap();
        assertThatThrownBy(() -> filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), (request, response) -> {
            assertThat(RequestContextHolder.getRequired().tenantId()).isEqualTo(42L); throw new ServletException("failed");
        })).isInstanceOf(ServletException.class).hasMessage("failed");
        assertThat(RequestContextHolder.getRequired()).isSameAs(previous); assertThat(MDC.getCopyOfContextMap()).isEqualTo(before);
    }

    @Test
    void missingIdentityCannotReuseAnEarlierTenantOrInventAnonymousAuthority() throws Exception {
        MDC.put("tenantId", "99"); var response = new MockHttpServletResponse();
        filter.doFilter(new MockHttpServletRequest(), response, (request, outgoing) -> { throw new AssertionError("No verified identity"); });
        assertThat(response.getStatus()).isEqualTo(401); assertThat(response.getHeader("WWW-Authenticate")).isEqualTo("Bearer");
        assertThat(objectMapper.readTree(response.getContentAsString()).get("code").textValue()).isEqualTo("AUTHENTICATION_REQUIRED");
        assertThat(RequestContextHolder.get()).isEmpty(); assertThat(MDC.get("tenantId")).isEqualTo("99");
    }

    @Test
    void malformedVerifiedTenantFailsClosedInsteadOfUsingAHeader() throws Exception {
        for (String tenant : Set.of("invalid", "9223372036854775808")) {
            authenticated("verified-actor", tenant); var request = new MockHttpServletRequest(); request.addHeader(RequestContextFilter.TENANT_ID_HEADER, "42");
            var response = new MockHttpServletResponse();
            filter.doFilter(request, response, (incoming, outgoing) -> { throw new AssertionError("Invalid verified native identity"); });
            assertThat(response.getStatus()).isEqualTo(503); assertThat(response.getHeader("Cache-Control")).isEqualTo("no-store");
            assertThat(objectMapper.readTree(response.getContentAsString()).size()).isEqualTo(2); assertContextCleared();
        }
    }

    @Test
    void bindsConfiguredAuditAndTenantKeysWithoutChangingTheirPreviousValues() throws Exception {
        var properties = new EgonColaMybatisPlusProperties(); properties.getAudit().setUserIdMdcKey("actorSub"); properties.getTenantId().setMdcKey("tenantLong");
        authenticated("service-user", "42"); MDC.put("actorSub", "previous"); MDC.put("tenantLong", "99");
        new RequestContextFilter(objectMapper, properties).doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), (request, response) -> {
            assertThat(MDC.get("actorSub")).isEqualTo("service-user"); assertThat(MDC.get("tenantLong")).isEqualTo("42");
        });
        assertThat(MDC.get("actorSub")).isEqualTo("previous"); assertThat(MDC.get("tenantLong")).isEqualTo("99");
        assertContextCleared();
    }

    private static void authenticated(String actor, String tenant) {
        Instant now = Instant.parse("2026-10-13T00:00:00Z");
        SecurityContextHolder.getContext().setAuthentication(new IdpAuthenticationToken(new IdentityPrincipal(actor, tenant, "901", Set.of("https://light.example.test"), now, now.plusSeconds(300), AuthenticationContext.password())));
    }
    private static void assertContextCleared() {
        assertThat(RequestContextHolder.get()).isEmpty(); assertThat(MDC.get("tenantId")).isNull(); assertThat(MDC.get("userId")).isNull(); assertThat(MDC.get("requestId")).isNull();
    }
}
