package top.egon.cola.platform.rbac3.starter.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import top.egon.cola.platform.rbac3.starter.runtime.Rbac3RuntimeSnapshotReader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class Rbac3BearerAuthenticationFilterTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void neverTreatsTrustedIdentityHeadersAsAuthentication() throws Exception {
        Rbac3BearerAuthenticationFilter filter = new Rbac3BearerAuthenticationFilter(
                new Rbac3JwtVerifier(token -> {
                    throw new AssertionError("decoder must not be called");
                }),
                mock(Rbac3RuntimeSnapshotReader.class),
                new ObjectMapper());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User-Id", "20001");
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(chain.getRequest()).isSameAs(request);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void rejectsMultipleAuthorizationHeadersBeforeTokenParsing() throws Exception {
        Rbac3BearerAuthenticationFilter filter = new Rbac3BearerAuthenticationFilter(
                new Rbac3JwtVerifier(token -> {
                    throw new AssertionError("decoder must not be called");
                }),
                mock(Rbac3RuntimeSnapshotReader.class),
                new ObjectMapper());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer one");
        request.addHeader("Authorization", "Bearer two");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString())
                .contains("AUTHORIZATION_HEADER_INVALID")
                .doesNotContain("one")
                .doesNotContain("two");
    }
}
