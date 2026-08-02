package top.egon.cola.platform.idp.admin.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import top.egon.cola.platform.idp.admin.oauth.infrastructure.IdpSsoSessionStore;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

/** Authenticates only the OAuth authorization endpoint from the host-only SSO cookie. */
public final class IdpSsoAuthenticationFilter extends OncePerRequestFilter {

    public static final String COOKIE_NAME = "EGON_IDP_SSO";

    private final IdpSsoSessionStore sessions;

    public IdpSsoAuthenticationFilter(IdpSsoSessionStore sessions) {
        this.sessions = Objects.requireNonNull(sessions, "sessions");
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"/oauth2/authorize".equals(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String token = cookie(request, COOKIE_NAME);
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            sessions.resolve(token).ifPresent(subject -> SecurityContextHolder.getContext()
                    .setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                            subject, "", List.of())));
        }
        filterChain.doFilter(request, response);
    }

    private String cookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
