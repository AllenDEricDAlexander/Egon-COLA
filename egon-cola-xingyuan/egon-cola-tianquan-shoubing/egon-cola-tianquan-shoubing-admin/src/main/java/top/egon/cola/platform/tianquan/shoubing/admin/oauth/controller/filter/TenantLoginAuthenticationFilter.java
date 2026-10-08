package top.egon.cola.platform.tianquan.shoubing.admin.oauth.controller.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.time.Clock;
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

@Component("tenantLoginAuthenticationFilter")
public class TenantLoginAuthenticationFilter extends OncePerRequestFilter {

    private static final int MAXIMUM_BODY_BYTES = 16 * 1024;
    private static final String ACR_PASSWORD = "urn:egon:loa:password";

    @Qualifier("identityFacade")
    private final IdentityFacade identities;
    @Qualifier("tenantMembershipPort")
    private final TenantMembershipPort tenantMemberships;
    @Qualifier("tenantIssuerService")
    private final TenantIssuerService tenantIssuerService;
    @Qualifier("oauthAuthorizationRequestCache")
    private final RequestCache requestCache;
    @Qualifier("egonColaMybatisPlusClock")
    private final Clock clock;
    @Qualifier("oauthLoginSessionAuthenticationStrategy")
    private final SessionAuthenticationStrategy sessionAuthenticationStrategy;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    public TenantLoginAuthenticationFilter(
            @Qualifier("identityFacade") IdentityFacade identities,
            @Qualifier("tenantMembershipPort") TenantMembershipPort tenantMemberships,
            @Qualifier("tenantIssuerService") TenantIssuerService tenantIssuerService,
            @Qualifier("oauthAuthorizationRequestCache") RequestCache requestCache,
            @Qualifier("egonColaMybatisPlusClock") Clock clock,
            @Qualifier("oauthLoginSessionAuthenticationStrategy") SessionAuthenticationStrategy sessionAuthenticationStrategy) {
        this.identities = Objects.requireNonNull(identities, "identities");
        this.tenantMemberships = Objects.requireNonNull(tenantMemberships, "tenantMemberships");
        this.tenantIssuerService = Objects.requireNonNull(tenantIssuerService, "tenantIssuerService");
        this.requestCache = Objects.requireNonNull(requestCache, "requestCache");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.sessionAuthenticationStrategy = Objects.requireNonNull(
                sessionAuthenticationStrategy, "sessionAuthenticationStrategy");
    }

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
            AuthenticatedIdentity identity = identities.authenticate(
                    usernames[0], password, "SAS_LOGIN", clock.instant());
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

            SavedRequest savedRequest = requestCache.getRequest(request, response);
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
            authentication.setDetails(Map.of("authTime", clock.instant(), "acr", ACR_PASSWORD));
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
