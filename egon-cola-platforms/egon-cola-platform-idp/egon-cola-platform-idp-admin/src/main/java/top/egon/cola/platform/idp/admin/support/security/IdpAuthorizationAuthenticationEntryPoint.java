package top.egon.cola.platform.idp.admin.support.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.Objects;

/** Redirects unauthenticated authorization requests to the central login UI. */
public final class IdpAuthorizationAuthenticationEntryPoint
        implements AuthenticationEntryPoint {

    private final String issuer;
    private final String loginUri;

    public IdpAuthorizationAuthenticationEntryPoint(
            String issuer,
            String loginUri
    ) {
        this.issuer = absoluteHttpUri(issuer, "issuer", false);
        this.loginUri = absoluteHttpUri(loginUri, "loginUri", true);
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authenticationException
    ) throws IOException, ServletException {
        String authorizationUri = UriComponentsBuilder.fromUriString(issuer)
                .path(request.getRequestURI())
                .replaceQuery(request.getQueryString())
                .build(true)
                .toUriString();
        String location = UriComponentsBuilder.fromUriString(loginUri)
                .queryParam("return_to", authorizationUri)
                .build()
                .encode()
                .toUriString();
        response.sendRedirect(location);
    }

    private static String absoluteHttpUri(
            String value,
            String name,
            boolean pathAllowed
    ) {
        String exact = Objects.requireNonNull(value, name).trim();
        URI uri = URI.create(exact);
        if (!("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()))
                || uri.getHost() == null
                || !pathAllowed && !uri.getPath().isEmpty()
                && !"/".equals(uri.getPath())
                || uri.getQuery() != null
                || uri.getFragment() != null) {
            throw new IllegalArgumentException(name + " must be an HTTP origin");
        }
        return exact.endsWith("/")
                ? exact.substring(0, exact.length() - 1)
                : exact;
    }
}
