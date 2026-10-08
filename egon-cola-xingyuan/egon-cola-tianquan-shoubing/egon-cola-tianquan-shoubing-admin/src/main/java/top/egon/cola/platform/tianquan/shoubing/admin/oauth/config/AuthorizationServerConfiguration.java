package top.egon.cola.platform.tianquan.shoubing.admin.oauth.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContext;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContextHolder;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.TrustedRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.TenantIssuerService;

@Configuration(proxyBeanMethods = false)
public class AuthorizationServerConfiguration {

    @Bean("oauthPublicOrigin")
    URI oauthPublicOrigin(
            @Value("${egon.tianquan-shoubing.oauth.public-origin}") URI configuredOrigin) {
        return validatePublicOrigin(configuredOrigin);
    }

    @Bean("authorizationServerSettings")
    AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder()
                .multipleIssuersAllowed(true)
                .build();
    }

    @Bean("authorizationServerSecurityFilterChain")
    @Order(1)
    SecurityFilterChain authorizationServerSecurityFilterChain(
            HttpSecurity http,
            @Qualifier("authorizationServerSettings") AuthorizationServerSettings settings,
            @Qualifier("tenantRegisteredClientRepository") RegisteredClientRepository registeredClientRepository,
            @Qualifier("tenantAuthorizationService") OAuth2AuthorizationService authorizationService,
            @Qualifier("tenantConsentService") OAuth2AuthorizationConsentService authorizationConsentService,
            @Qualifier("tenantIssuerService") TenantIssuerService tenantIssuerService,
            @Qualifier("oauthPublicOrigin") URI publicOrigin) throws Exception {
        OAuth2AuthorizationServerConfigurer authorizationServer =
                OAuth2AuthorizationServerConfigurer.authorizationServer();
        RequestMatcher endpoints = authorizationServer.getEndpointsMatcher();
        http.securityMatcher(endpoints)
                .csrf(csrf -> csrf.ignoringRequestMatchers(endpoints))
                .with(authorizationServer, server -> server
                        .authorizationServerSettings(settings)
                        .registeredClientRepository(registeredClientRepository)
                        .authorizationService(authorizationService)
                        .authorizationConsentService(authorizationConsentService)
                        .oidc(Customizer.withDefaults()))
                .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated());

        // SAS installs its protocol endpoint filters at this anchor after the issuer context filter.
        http.addFilterBefore(new ExactIssuerContextFilter(tenantIssuerService, publicOrigin),
                AbstractPreAuthenticatedProcessingFilter.class);
        return http.build();
    }

    private static URI validatePublicOrigin(URI origin) {
        if (origin == null || !origin.isAbsolute() || !"https".equalsIgnoreCase(origin.getScheme())
                || origin.getHost() == null || origin.getUserInfo() != null
                || origin.getQuery() != null || origin.getFragment() != null
                || (origin.getRawPath() != null && !origin.getRawPath().isEmpty()
                        && !"/".equals(origin.getRawPath()))) {
            throw new IllegalArgumentException("OAUTH_PUBLIC_ORIGIN_INVALID");
        }
        return origin;
    }

    private static final class ExactIssuerContextFilter extends OncePerRequestFilter {

        private final TenantIssuerService tenantIssuerService;
        private final String publicOriginRoot;

        private ExactIssuerContextFilter(TenantIssuerService tenantIssuerService, URI publicOrigin) {
            this.tenantIssuerService = tenantIssuerService;
            String origin = publicOrigin.toASCIIString();
            this.publicOriginRoot = origin.endsWith("/") ? origin.substring(0, origin.length() - 1) : origin;
        }

        @Override
        protected void doFilterInternal(
                HttpServletRequest request,
                HttpServletResponse response,
                FilterChain filterChain) throws ServletException, IOException {
            AuthorizationServerContext current = AuthorizationServerContextHolder.getContext();
            if (current == null || current.getIssuer() == null) {
                writeError(response, HttpServletResponse.SC_NOT_FOUND, "invalid_request", "Request rejected");
                return;
            }

            TrustedRealmBO trustedRealm;
            try {
                URI requestIssuer = URI.create(current.getIssuer());
                String path = requestIssuer.getRawPath();
                if (path == null || path.isEmpty()) {
                    throw new OAuth2AuthenticationException("invalid_request");
                }
                URI configuredIssuer = URI.create(publicOriginRoot + path);
                trustedRealm = tenantIssuerService.resolveExactIssuer(configuredIssuer);
            } catch (OAuth2AuthenticationException | IllegalArgumentException failure) {
                writeError(response, HttpServletResponse.SC_NOT_FOUND, "invalid_request", "Request rejected");
                return;
            } catch (RuntimeException failure) {
                writeError(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                        "temporarily_unavailable", "Request temporarily unavailable");
                return;
            }

            response.setHeader("Cache-Control", isPublicMetadataRequest(request, trustedRealm)
                    ? "public, max-age=60" : "no-store");

            AuthorizationServerContext resolvedContext = new AuthorizationServerContext() {
                @Override
                public String getIssuer() {
                    return trustedRealm.getIssuer().toASCIIString();
                }

                @Override
                public AuthorizationServerSettings getAuthorizationServerSettings() {
                    return current.getAuthorizationServerSettings();
                }
            };
            AuthorizationServerContextHolder.setContext(resolvedContext);
            try {
                filterChain.doFilter(request, response);
            } finally {
                AuthorizationServerContextHolder.setContext(current);
            }
        }

        private static void writeError(
                HttpServletResponse response,
                int status,
                String error,
                String description) throws IOException {
            response.setStatus(status);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Cache-Control", "no-store");
            response.getWriter().write("{\"error\":\"" + error + "\",\"error_description\":\"" + description + "\"}");
        }

        private static boolean isPublicMetadataRequest(HttpServletRequest request, TrustedRealmBO trustedRealm) {
            if (!"GET".equals(request.getMethod())) {
                return false;
            }
            String path = request.getRequestURI();
            String contextPath = request.getContextPath();
            if (contextPath != null && !contextPath.isEmpty() && path.startsWith(contextPath)) {
                path = path.substring(contextPath.length());
            }
            String issuerPath = trustedRealm.getIssuer().getRawPath();
            return path.equals(issuerPath + "/.well-known/openid-configuration")
                    || path.equals(issuerPath + "/oauth2/jwks");
        }
    }
}
