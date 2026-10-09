package top.egon.cola.platform.tianquan.shoubing.admin.oauth.controller.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.savedrequest.DefaultSavedRequest;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.TrustedRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.enums.RealmTypeEnum;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.TenantIssuerService;
import top.egon.cola.platform.tianquan.shoubing.core.identity.AuthenticatedIdentity;
import top.egon.cola.platform.tianquan.shoubing.core.identity.IdentityException;
import top.egon.cola.platform.tianquan.shoubing.core.identity.IdentityFacade;
import top.egon.cola.platform.tianquan.shoubing.core.port.TenantMembershipPort;
import top.egon.cola.component.common.crypto.digest.Digests;

@Component("tenantLoginAuthenticationFilter")
@RequiredArgsConstructor
@Slf4j
public class TenantLoginAuthenticationFilter extends OncePerRequestFilter {

    private static final int MAXIMUM_BODY_BYTES = 16 * 1024;
    private static final String ACR_PASSWORD = "PASSWORD";

    @Qualifier("identityFacade")
    @NonNull
    private final IdentityFacade identities;
    @Qualifier("tenantMembershipPort")
    @NonNull
    private final TenantMembershipPort tenantMemberships;
    @Qualifier("tenantIssuerService")
    @NonNull
    private final TenantIssuerService tenantIssuerService;
    @Qualifier("oauthAuthorizationRequestCache")
    @NonNull
    private final RequestCache requestCache;
    @Qualifier("egonColaMybatisPlusClock")
    @NonNull
    private final Clock clock;
    @Qualifier("oauthLoginSessionAuthenticationStrategy")
    @NonNull
    private final SessionAuthenticationStrategy sessionAuthenticationStrategy;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = path(request);
        return !"POST".equals(request.getMethod()) || path == null
                || !path.matches("/tenants/[1-9][0-9]{0,18}/login");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        if (request.getContentLengthLong() > MAXIMUM_BODY_BYTES || request.getQueryString() != null) {
            writeError(response, HttpStatus.BAD_REQUEST, "Request rejected");
            return;
        }
        String path = path(request);
        TrustedRealmBO trustedRealm = tenantIssuerService.resolveCurrentIssuer();
        if (!validTenantPath(path, trustedRealm) || !hasAuthorizedRequest(request, trustedRealm)) {
            writeError(response, HttpStatus.NOT_FOUND, "Request rejected");
            return;
        }
        String[] usernames = request.getParameterMap().get("username");
        String[] passwords = request.getParameterMap().get("password");
        String[] csrf = request.getParameterMap().get("_csrf");
        if (!onlyLoginParameters(request.getParameterMap().keySet())
                || !oneValue(usernames) || !oneValue(passwords) || !oneValue(csrf)
                || usernames[0].isBlank() || usernames[0].length() > 128
                || passwords[0].isEmpty() || passwords[0].length() > 1024) {
            writeError(response, HttpStatus.BAD_REQUEST, "Request rejected");
            return;
        }

        char[] password = passwords[0].toCharArray();
        try {
            SavedRequest savedRequest = requestCache.getRequest(request, response);
            boolean freshRequest = savedRequest instanceof DefaultSavedRequest saved && requestsFreshAuthentication(saved);
            var current = SecurityContextHolder.getContext().getAuthentication();
            boolean currentSubjectProof = freshRequest && current instanceof UsernamePasswordAuthenticationToken
                    && current.isAuthenticated();
            AuthenticatedIdentity identity = currentSubjectProof
                    ? identities.authenticateCurrent(current.getName(), password, clock.instant())
                    : identities.authenticate(usernames[0], password, "SAS_LOGIN", clock.instant());
            if (currentSubjectProof && !current.getName().equals(identity.identitySub())) {
                writeError(response, HttpStatus.UNAUTHORIZED, "Sign in failed");
                return;
            }
            if (identity.mustChangePassword()) {
                writeError(response, HttpStatus.FORBIDDEN, "Password change is required");
                return;
            }
            TenantMembershipPort.TenantMembership membership = tenantMemberships.resolve(
                    identity.identitySub(), Long.toString(trustedRealm.getBusinessTenantId()));
            if (membership == null || membership.status() != TenantMembershipPort.MembershipStatus.ACTIVE) {
                writeError(response, HttpStatus.FORBIDDEN, "Request rejected");
                return;
            }

            URI authorizeUri = authorizeUri(savedRequest, trustedRealm);
            if (authorizeUri == null) {
                writeError(response, HttpStatus.NOT_FOUND, "Request rejected");
                return;
            }
            request.getSession(true);
            UsernamePasswordAuthenticationToken authentication = UsernamePasswordAuthenticationToken.authenticated(
                    User.withUsername(identity.identitySub()).password("").authorities("ROLE_USER").build(),
                    "",
                    List.of(() -> "ROLE_USER"));
            Instant authenticatedAt = clock.instant();
            Map<String, Object> details = new HashMap<>();
            details.put("authTime", authenticatedAt);
            details.put("acr", currentSubjectProof ? "STRONG" : ACR_PASSWORD);
            if (freshRequest && savedRequest instanceof DefaultSavedRequest saved) {
                details.put("reauthenticatedRequest", authorizationRequestDigest(trustedRealm.getIssuer(), saved.getParameterMap()));
                details.put("reauthenticationExpiresAt", authenticatedAt.plusSeconds(300));
            }
            authentication.setDetails(details);
            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
            securityContext.setAuthentication(authentication);
            sessionAuthenticationStrategy.onAuthentication(authentication, request, response);
            SecurityContextHolder.setContext(securityContext);
            securityContextRepository.saveContext(securityContext, request, response);
            requestCache.removeRequest(request, response);
            response.setStatus(HttpServletResponse.SC_SEE_OTHER);
            response.setHeader(HttpHeaders.LOCATION,
                    authorizeUri.getRawPath() + (authorizeUri.getRawQuery() == null ? "" : "?" + authorizeUri.getRawQuery()));
            response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
            response.setHeader("Referrer-Policy", "no-referrer");
        } catch (IdentityException failure) {
            writeError(response, HttpStatus.UNAUTHORIZED, "Sign in failed");
        } catch (TenantMembershipPort.TenantMembershipException failure) {
            writeError(response, HttpStatus.SERVICE_UNAVAILABLE, "Request temporarily unavailable");
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    private static boolean validTenantPath(String path, TrustedRealmBO trustedRealm) {
        if (path == null || trustedRealm.getRealmType() != RealmTypeEnum.TENANT
                || trustedRealm.getBusinessTenantId() == null) {
            return false;
        }
        return path.equals("/tenants/" + trustedRealm.getBusinessTenantId() + "/login")
                && path.equals(trustedRealm.getIssuer().getRawPath() + "/login");
    }

    private static boolean requestsFreshAuthentication(DefaultSavedRequest request) {
        String[] prompt = request.getParameterMap().get("prompt");
        return oneValue(prompt) && Arrays.asList(prompt[0].split(" ", -1)).contains("login")
                || request.getParameterMap().containsKey("max_age");
    }

    public static String authorizationRequestDigest(URI issuer, Map<String, String[]> parameters) {
        String canonicalIssuer = Objects.requireNonNull(issuer, "issuer").toASCIIString();
        StringBuilder normalized = new StringBuilder().append(canonicalIssuer.length()).append(':').append(canonicalIssuer);
        parameters.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            normalized.append(entry.getKey().length()).append(':').append(entry.getKey())
                    .append(':').append(entry.getValue().length).append(':');
            for (String value : entry.getValue()) {
                normalized.append(value.length()).append(':').append(value);
            }
        });
        return Digests.sha256Hex(normalized.toString());
    }

    private boolean hasAuthorizedRequest(HttpServletRequest request, TrustedRealmBO trustedRealm) {
        return authorizeUri(requestCache.getRequest(request, null), trustedRealm) != null;
    }

    private static URI authorizeUri(SavedRequest savedRequest, TrustedRealmBO trustedRealm) {
        if (!(savedRequest instanceof DefaultSavedRequest saved) || !"GET".equals(saved.getMethod())) {
            return null;
        }
        try {
            URI uri = URI.create(saved.getRedirectUrl());
            if (!uri.isAbsolute() || uri.getUserInfo() != null || uri.getFragment() != null
                    || !Objects.equals(uri.getRawPath(), trustedRealm.getIssuer().getRawPath() + "/oauth2/authorize")) {
                return null;
            }
            return uri;
        } catch (IllegalArgumentException failure) {
            return null;
        }
    }

    private static String path(HttpServletRequest request) {
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        return contextPath == null || contextPath.isEmpty() || !path.startsWith(contextPath)
                ? path : path.substring(contextPath.length());
    }

    private static boolean oneValue(String[] values) {
        return values != null && values.length == 1 && values[0] != null;
    }

    private static boolean onlyLoginParameters(Set<String> names) {
        return names.stream().allMatch(name -> Set.of("username", "password", "_csrf").contains(name));
    }

    private static void writeError(HttpServletResponse response, HttpStatus status, String message)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType("text/html;charset=UTF-8");
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        response.setHeader("Referrer-Policy", "no-referrer");
        response.getWriter().write("<!doctype html><html><body><p>" + message + "</p></body></html>");
    }
}
