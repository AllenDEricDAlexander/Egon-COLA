package top.egon.cola.platform.idp.starter.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Objects;

/**
 * Request-scoped hand-off of the already verified USER access token.
 */
public final class VerifiedUserTokenCarrier {
    private static final String ATTRIBUTE = VerifiedUserTokenCarrier.class.getName() + ".rawUserAccessToken";

    private VerifiedUserTokenCarrier() {
    }

    public static void set(HttpServletRequest request, String token) {
        Objects.requireNonNull(request, "request").setAttribute(ATTRIBUTE, required(token));
    }

    public static String current(HttpServletRequest request) {
        Object value = Objects.requireNonNull(request, "request").getAttribute(ATTRIBUTE);
        if (!(value instanceof String token) || token.isBlank()) {
            throw new IllegalStateException("verified USER access token is unavailable");
        }
        return token;
    }

    public static String current() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes servlet)) {
            throw new IllegalStateException("verified USER access token is unavailable");
        }
        return current(servlet.getRequest());
    }

    public static void clear(HttpServletRequest request) {
        if (request != null) request.removeAttribute(ATTRIBUTE);
    }

    private static String required(String token) {
        if (token == null || token.isBlank() || token.length() > 16_384) {
            throw new IllegalArgumentException("USER access token is invalid");
        }
        return token;
    }
}
