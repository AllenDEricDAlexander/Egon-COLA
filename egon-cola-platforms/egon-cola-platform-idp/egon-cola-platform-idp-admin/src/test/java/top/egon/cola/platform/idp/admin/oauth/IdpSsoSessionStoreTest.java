package top.egon.cola.platform.idp.admin.oauth;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import top.egon.cola.platform.idp.admin.oauth.infrastructure.IdpSsoSessionStore;
import top.egon.cola.platform.idp.admin.security.IdpAuthorizationAuthenticationEntryPoint;
import top.egon.cola.platform.idp.admin.security.IdpSsoAuthenticationFilter;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IdpSsoSessionStoreTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void storesOnlySubjectBehindDigestKeyAndHonorsTtl() {
        RedissonClient redisson = mock(RedissonClient.class);
        @SuppressWarnings("unchecked")
        RBucket<String> bucket = mock(RBucket.class);
        when(redisson.<String>getBucket(
                anyString(),
                eq(StringCodec.INSTANCE)
        )).thenReturn(bucket);
        when(bucket.setIfAbsent("alice-sub", Duration.ofHours(12)))
                .thenReturn(true);
        when(bucket.get()).thenReturn("alice-sub");
        SecureRandom random = mock(SecureRandom.class);
        doAnswer(invocation -> {
            Arrays.fill(invocation.<byte[]>getArgument(0), (byte) 7);
            return null;
        }).when(random).nextBytes(org.mockito.ArgumentMatchers.any(byte[].class));
        IdpSsoSessionStore store = new IdpSsoSessionStore(
                redisson,
                random,
                "identity:v1:sso-session:"
        );

        String token = store.create("alice-sub", Duration.ofHours(12));

        assertEquals("alice-sub", store.resolve(token).orElseThrow());
        verify(bucket).setIfAbsent("alice-sub", Duration.ofHours(12));
        verify(redisson, org.mockito.Mockito.times(2)).getBucket(
                "identity:v1:sso-session:"
                        + "dc4bf80c77473d130fa0de86ba4018fe98bb214005e6a5891"
                        + "d12ba91446f9e81",
                StringCodec.INSTANCE
        );
    }

    @Test
    void rejectsUnboundedSessionTtl() {
        IdpSsoSessionStore store = new IdpSsoSessionStore(
                mock(RedissonClient.class),
                new SecureRandom(),
                "identity:v1:sso-session:"
        );

        assertThrows(IllegalArgumentException.class, () -> store.create(
                "alice-sub",
                Duration.ofDays(31)
        ));
    }

    @Test
    void cookieAuthenticatesOnlyAuthorizationEndpoint() throws Exception {
        IdpSsoSessionStore store = mock(IdpSsoSessionStore.class);
        when(store.resolve("sso-token")).thenReturn(Optional.of("alice-sub"));
        IdpSsoAuthenticationFilter filter =
                new IdpSsoAuthenticationFilter(store);
        MockHttpServletRequest authorize = new MockHttpServletRequest(
                "GET",
                "/oauth2/authorize"
        );
        authorize.setCookies(new Cookie(
                IdpSsoAuthenticationFilter.COOKIE_NAME,
                "sso-token"
        ));

        filter.doFilter(
                authorize,
                new MockHttpServletResponse(),
                new MockFilterChain()
        );

        assertEquals("alice-sub", SecurityContextHolder.getContext()
                .getAuthentication().getName());
        SecurityContextHolder.clearContext();
        MockHttpServletRequest admin = new MockHttpServletRequest(
                "GET",
                "/api/v1/identity/users"
        );
        admin.setCookies(new Cookie(
                IdpSsoAuthenticationFilter.COOKIE_NAME,
                "sso-token"
        ));

        filter.doFilter(
                admin,
                new MockHttpServletResponse(),
                new MockFilterChain()
        );

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void unauthenticatedAuthorizeRedirectsToLoginWithServerBuiltReturnUri()
            throws Exception {
        IdpAuthorizationAuthenticationEntryPoint entryPoint =
                new IdpAuthorizationAuthenticationEntryPoint(
                        "http://127.0.0.1:18120",
                        "http://127.0.0.1:18121/login"
                );
        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET",
                "/oauth2/authorize"
        );
        request.setQueryString("client_id=gateway-admin-web&state=state-value");
        request.addHeader("Host", "attacker.example.test");
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(
                request,
                response,
                new InsufficientAuthenticationException("login required")
        );

        assertEquals(302, response.getStatus());
        assertTrue(response.getRedirectedUrl().startsWith(
                "http://127.0.0.1:18121/login?return_to="
        ));
        assertTrue(response.getRedirectedUrl().contains("127.0.0.1:18120"));
        assertTrue(response.getRedirectedUrl().contains("oauth2"));
        assertTrue(response.getRedirectedUrl().contains("authorize"));
        assertTrue(!response.getRedirectedUrl().contains("attacker.example.test"));
    }
}
