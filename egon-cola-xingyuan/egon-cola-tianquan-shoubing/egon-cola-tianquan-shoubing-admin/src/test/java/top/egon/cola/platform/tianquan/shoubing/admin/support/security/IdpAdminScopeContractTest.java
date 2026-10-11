package top.egon.cola.platform.tianquan.shoubing.admin.support.security;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import top.egon.cola.platform.tianquan.jianshen.contract.authorization.SystemAuthorizationSnapshot;
import top.egon.cola.platform.tianquan.jianshen.starter.authorization.AuthorizationService;
import top.egon.cola.platform.tianquan.jianshen.starter.authorization.DefaultAuthorizationService;
import top.egon.cola.platform.tianquan.jianshen.starter.autoconfigure.Rbac3StarterProperties;
import top.egon.cola.platform.tianquan.shoubing.admin.identity.controller.IdentityProfileController;
import top.egon.cola.platform.tianquan.shoubing.contract.IdentityPrincipal;
import top.egon.cola.platform.tianquan.shoubing.core.observability.IdpSecurityMetricsRecorder;
import top.egon.cola.platform.tianquan.shoubing.starter.security.AccessTokenVerification;
import top.egon.cola.platform.tianquan.shoubing.starter.security.IdpAuthenticationToken;
import top.egon.cola.platform.tianquan.shoubing.starter.security.IdpJwtVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/** Independent scope/PDP decisions through the actual port; the JWT is locally RS256 verified. */
class IdpAdminScopeContractTest {
    private static final Instant NOW = Instant.parse("2026-10-11T00:00:00Z");
    private static final String ISSUER = "https://idp.example/legacy";
    private static final Set<String> KNOWN = Set.of(
            "tianquan-shoubing:audit:read", "tianquan-shoubing:bootstrap:read",
            "tianquan-shoubing:identity-user:create", "tianquan-shoubing:identity-user:password-reset",
            "tianquan-shoubing:identity-user:read", "tianquan-shoubing:identity-user:revoke-all", "tianquan-shoubing:identity-user:update",
            "tianquan-shoubing:identity:self:read", "tianquan-shoubing:oauth-client:create", "tianquan-shoubing:oauth-client:read",
            "tianquan-shoubing:oauth-client:update", "tianquan-shoubing:resource-server:create", "tianquan-shoubing:resource-server:grant",
            "tianquan-shoubing:resource-server:read", "tianquan-shoubing:resource-server:status", "tianquan-shoubing:signing-key:activate",
            "tianquan-shoubing:signing-key:publish", "tianquan-shoubing:signing-key:read", "tianquan-shoubing:signing-key:retire",
            "tianquan-shoubing:tenant:manage", "tianquan-shoubing:tenant:read");
    private final LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private RSAKey key;
    private IdpJwtVerifier verifier;

    @BeforeEach
    void prepare() throws Exception {
        validator.afterPropertiesSet();
        key = new RSAKeyGenerator(2048).keyID("scope-fixture-key").algorithm(JWSAlgorithm.RS256).generate();
        var decoder = NimbusJwtDecoder.withPublicKey(key.toRSAPublicKey()).validateType(false).build();
        var timestamps = new JwtTimestampValidator(java.time.Duration.ZERO); timestamps.setClock(clock);
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(timestamps, new JwtIssuerValidator(ISSUER)));
        verifier = new IdpJwtVerifier(decoder, "legacy-platform-audience", clock);
    }

    @AfterEach
    void cleanup() { validator.close(); SecurityContextHolder.clearContext(); }

    @Test
    void scopeAndPdpIndependentForKnown21Permissions() throws Exception {
        assertThat(KNOWN).hasSize(21);
        var missing = authenticate(Set.of("unrelated:read"));
        var fine = spy(actualAuthorization(missing, KNOWN));
        var port = port(fine);
        for (String permission : KNOWN) {
            assertThatThrownBy(() -> port.require(missing, permission)).isInstanceOf(AccessDeniedException.class);
        }
        verify(fine, never()).requirePermission(any());
        var user = authenticate(KNOWN);
        for (String permission : KNOWN) { port.require(user, permission); }
        var denied = port(actualAuthorization(user, Set.of()));
        IdentityPrincipal verified = user;
        assertThatThrownBy(() -> denied.require(verified, "tianquan-shoubing:identity:self:read")).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void unknownPermissionIsNotDerivedIntoAScopeAndOriginalProfileDtoIsPreserved() throws Exception {
        var user = authenticate(KNOWN);
        var fine = spy(actualAuthorization(user, KNOWN));
        var port = port(fine);
        String unknown = "tianquan-shoubing:future:manage";
        assertThatThrownBy(() -> port.require(user, unknown)).isInstanceOf(AccessDeniedException.class);
        verify(fine, never()).requirePermission(any());
        assertThat(new IdentityProfileController(port).me(user)).isSameAs(user);
    }

    private IdpAdminAuthorizationPort port(AuthorizationService fine) {
        var beans = new DefaultListableBeanFactory(); beans.registerSingleton("authorizationService", fine);
        var properties = new Rbac3StarterProperties();
        properties.getUserScopePolicy().setEnabled(true).setPermissionScopes(KNOWN.stream().collect(
                java.util.stream.Collectors.toUnmodifiableMap(value -> value, Set::of)));
        return new IdpSecurityConfig().idpAdminAuthorizationPort(beans.getBeanProvider(AuthorizationService.class), properties,
                null, IdpSecurityMetricsRecorder.NOOP, validator);
    }

    private AuthorizationService actualAuthorization(IdentityPrincipal user, Set<String> permissions) {
        var snapshot = new SystemAuthorizationSnapshot("1001", "10001", "20001", "tianquan-shoubing", 1, 2, List.of("50001"), null, null,
                permissions, Set.of(), Map.of(), Map.of(), "sha256:scope-fixture", NOW, NOW.plusSeconds(300));
        return new DefaultAuthorizationService(() -> new AuthorizationService.RuntimeAuthorizationContext(user, snapshot, false),
                request -> AuthorizationService.OperationSodResult.allowed(), request -> AuthorizationService.FenceResult.allowed(NOW), clock);
    }

    private IdentityPrincipal authenticate(Set<String> scopes) throws Exception {
        var claims = new java.util.LinkedHashMap<String, Object>();
        claims.put("iss", ISSUER); claims.put("sub", "10001"); claims.put("aud", List.of("legacy-platform-audience"));
        claims.put("iat", NOW.minusSeconds(1).getEpochSecond()); claims.put("nbf", NOW.minusSeconds(1).getEpochSecond());
        claims.put("exp", NOW.plusSeconds(300).getEpochSecond()); claims.put("jti", "901"); claims.put("tid", "1001");
        claims.put("principal_type", "USER"); claims.put("scope", List.copyOf(scopes)); claims.put("acr", "PASSWORD");
        claims.put("auth_time", NOW.minusSeconds(1).getEpochSecond());
        var jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).type(new JOSEObjectType("at+jwt")).keyID("scope-fixture-key").build(), JWTClaimsSet.parse(claims));
        jwt.sign(new RSASSASigner(key));
        var checked = verifier.verifyUser(jwt.serialize());
        assertThat(checked).isInstanceOf(AccessTokenVerification.Valid.class);
        var valid = (AccessTokenVerification.Valid<IdentityPrincipal>) checked;
        SecurityContextHolder.getContext().setAuthentication(new IdpAuthenticationToken(valid.principal(), valid.scopes().stream()
                .map(value -> new SimpleGrantedAuthority("SCOPE_" + value)).toList()));
        return valid.principal();
    }
}
