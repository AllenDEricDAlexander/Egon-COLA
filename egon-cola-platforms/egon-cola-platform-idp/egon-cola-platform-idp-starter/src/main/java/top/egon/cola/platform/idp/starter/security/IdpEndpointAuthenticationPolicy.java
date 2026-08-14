package top.egon.cola.platform.idp.starter.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.AntPathMatcher;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Selects the credential type allowed for an IdP-protected endpoint.
 *
 * <p>The owning application may explicitly mark public and SERVICE-only path patterns. All
 * other paths default to USER, while an application that does not want an implicit default can
 * use the three-argument constructor with {@code defaultApplicationRequirement=false}; an
 * unclassified protected path then fails closed.</p>
 */
public final class IdpEndpointAuthenticationPolicy {

    /**
     * Credential requirement selected for one request path.
     */
    public enum Requirement {
        /**
         * Public protocol endpoint; the owning security chain decides access.
         */
        PUBLIC,
        /**
         * A verified IdP USER access token is required when credentials are present.
         */
        USER,
        /**
         * A verified IdP SERVICE access token is required when credentials are present.
         */
        SERVICE,
        /**
         * The endpoint policy is missing or ambiguous and must be rejected.
         */
        DENY
    }

    private final List<String> publicPathPatterns;
    private final List<String> servicePathPatterns;
    private final Requirement defaultRequirement;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    /**
     * Creates the default policy: explicit SERVICE paths, otherwise USER.
     */
    public IdpEndpointAuthenticationPolicy(
            Collection<String> publicPathPatterns,
            Collection<String> servicePathPatterns) {
        this(publicPathPatterns, servicePathPatterns, true);
    }

    /**
     * Creates a policy with an explicit fallback for application paths.
     *
     * @param publicPathPatterns            public path patterns; public path patterns
     * @param servicePathPatterns           SERVICE-only path patterns; SERVICE-only path patterns
     * @param defaultApplicationRequirement whether unmatched paths require USER
     */
    public IdpEndpointAuthenticationPolicy(
            Collection<String> publicPathPatterns,
            Collection<String> servicePathPatterns,
            boolean defaultApplicationRequirement) {
        this.publicPathPatterns = patterns(publicPathPatterns, "publicPathPatterns");
        this.servicePathPatterns = patterns(servicePathPatterns, "servicePathPatterns");
        this.defaultRequirement = defaultApplicationRequirement
                ? Requirement.USER : Requirement.DENY;
    }

    /**
     * Creates a policy that defaults every unclassified application path to USER.
     */
    public IdpEndpointAuthenticationPolicy() {
        this(List.of(), List.of());
    }

    /**
     * Resolves the credential requirement for the current request.
     *
     * <p>If a path matches both public and SERVICE-only rules, the ambiguity is rejected instead
     * of silently selecting one credential type.</p>
     *
     * @param request current servlet request; current servlet request
     * @return the fail-closed requirement; the fail-closed requirement
     */
    public Requirement requirement(HttpServletRequest request) {
        Objects.requireNonNull(request, "request");
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty()
                && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }
        boolean publicEndpoint = matches(publicPathPatterns, path);
        boolean serviceEndpoint = matches(servicePathPatterns, path);
        if (publicEndpoint && serviceEndpoint) {
            return Requirement.DENY;
        }
        if (publicEndpoint) {
            return Requirement.PUBLIC;
        }
        if (serviceEndpoint) {
            return Requirement.SERVICE;
        }
        return defaultRequirement;
    }

    private static List<String> patterns(
            Collection<String> patterns,
            String field) {
        Objects.requireNonNull(patterns, field);
        return patterns.stream()
                .map(pattern -> {
                    if (pattern == null || pattern.isBlank()) {
                        throw new IllegalArgumentException(field + " contains a blank pattern");
                    }
                    return pattern.trim();
                })
                .toList();
    }

    private boolean matches(
            List<String> patterns,
            String path) {
        return patterns.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }
}
