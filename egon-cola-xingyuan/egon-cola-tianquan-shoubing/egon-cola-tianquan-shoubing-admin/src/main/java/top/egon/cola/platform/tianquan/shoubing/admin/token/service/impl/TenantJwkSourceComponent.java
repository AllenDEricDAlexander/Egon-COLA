package top.egon.cola.platform.tianquan.shoubing.admin.token.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.KeySourceException;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.TrustedRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository.OAuthRealmRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.TenantIssuerService;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.bo.OAuthSigningKeyBO;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.enums.OAuthKeyStatusEnum;
import top.egon.cola.platform.tianquan.shoubing.admin.token.repository.OAuthSigningKeyRepository;

@Component("tenantJwkSourceComponent")
@Slf4j
@RequiredArgsConstructor
@Validated
public class TenantJwkSourceComponent implements JWKSource<SecurityContext> {

    private static final int MINIMUM_RSA_MODULUS_BITS = 3072;
    private static final int MAX_PUBLIC_KEYS = 8;
    private static final Set<OAuthKeyStatusEnum> PUBLISHED_STATUSES = Set.of(
            OAuthKeyStatusEnum.ACTIVE,
            OAuthKeyStatusEnum.PUBLISHED,
            OAuthKeyStatusEnum.VERIFY_ONLY);

    @Qualifier("tenantIssuerService")
    private final TenantIssuerService tenantIssuerService;
    @Qualifier("oAuthRealmRepository")
    private final OAuthRealmRepository realmRepository;
    @Qualifier("oAuthSigningKeyRepository")
    private final OAuthSigningKeyRepository signingKeyRepository;
    @Qualifier("oauthKeyCipherComponent")
    private final OAuthKeyCipherComponent keyCipher;

    @Override
    public List<JWK> get(JWKSelector selector, SecurityContext context) throws KeySourceException {
        if (selector == null) {
            throw new KeySourceException("OAuth JWK selector is required");
        }
        TrustedRealmBO trustedRealm = tenantIssuerService.resolveCurrentIssuer();
        List<OAuthSigningKeyBO> keys = signingKeyRepository.findByStatuses(
                trustedRealm, PUBLISHED_STATUSES, 0L, MAX_PUBLIC_KEYS);
        if (isPublicJwkRequest(selector.getMatcher())) {
            List<JWK> publicKeys = new java.util.ArrayList<>(
                    keys.stream().map(TenantJwkSourceComponent::publicKey).toList());
            return selector.select(new JWKSet(publicKeys));
        }

        OAuthRealmBO realm = realmRepository.findByIssuer(trustedRealm)
                .orElseThrow(() -> new KeySourceException("Trusted signing realm is unavailable"));
        OAuthSigningKeyBO active = keys.stream()
                .filter(key -> key.getStatus() == OAuthKeyStatusEnum.ACTIVE)
                .filter(key -> key.getKid() != null && key.getKid().equals(realm.getActiveKid()))
                .findFirst()
                .orElseThrow(() -> new KeySourceException("Active signing key is unavailable"));
        RSAKey signingKey = signingKey(trustedRealm, active);
        return selector.select(new JWKSet(List.of(signingKey)));
    }

    private static boolean isPublicJwkRequest(com.nimbusds.jose.jwk.JWKMatcher matcher) {
        return matcher.isPublicOnly()
                || (!matcher.isPrivateOnly()
                        && !matcher.hasKeyID()
                        && !matcher.hasKeyUse()
                        && isEmpty(matcher.getKeyTypes())
                        && isEmpty(matcher.getKeyOperations())
                        && isEmpty(matcher.getAlgorithms()));
    }

    private static boolean isEmpty(java.util.Collection<?> values) {
        return values == null || values.isEmpty();
    }

    private RSAKey signingKey(TrustedRealmBO trustedRealm, OAuthSigningKeyBO key) throws KeySourceException {
        RSAKey publicKey = publicKey(key);
        if (!key.getKid().equals(publicKey.getKeyID())) {
            throw new KeySourceException("Active signing key id does not match public key");
        }
        String privateKeyPem = keyCipher.decryptPrivateKey(
                trustedRealm, key.getKid(), key.getPrivateKeyCiphertext());
        try {
            PrivateKey privateKey = KeyFactory.getInstance("RSA")
                    .generatePrivate(new PKCS8EncodedKeySpec(decodePrivateKey(privateKeyPem)));
            if (!(privateKey instanceof RSAPrivateCrtKey rsaPrivateKey)
                    || !publicKey.getModulus().decodeToBigInteger().equals(rsaPrivateKey.getModulus())
                    || publicKey.getModulus().decodeToBigInteger().bitLength() < MINIMUM_RSA_MODULUS_BITS) {
                throw new KeySourceException("Active signing key pair is invalid");
            }
            return new RSAKey.Builder(publicKey.toRSAPublicKey())
                    .privateKey(rsaPrivateKey)
                    .keyID(key.getKid())
                    .algorithm(JWSAlgorithm.RS256)
                    .keyUse(KeyUse.SIGNATURE)
                    .build();
        } catch (KeySourceException failure) {
            throw failure;
        } catch (Exception failure) {
            throw new KeySourceException("Active signing key cannot be loaded", failure);
        }
    }

    private static RSAKey publicKey(OAuthSigningKeyBO key) {
        JsonNode json = key.getPublicJwk();
        if (json == null || json.isNull() || !json.isObject()) {
            throw new IllegalStateException("OAUTH_PUBLIC_JWK_INVALID");
        }
        try {
            JWK parsed = JWK.parse(json.toString());
            if (!(parsed instanceof RSAKey rsaKey)
                    || rsaKey.getKeyID() == null || !rsaKey.getKeyID().equals(key.getKid())
                    || !JWSAlgorithm.RS256.equals(rsaKey.getAlgorithm())
                    || !KeyUse.SIGNATURE.equals(rsaKey.getKeyUse())
                    || rsaKey.getModulus().decodeToBigInteger().bitLength() < MINIMUM_RSA_MODULUS_BITS) {
                throw new IllegalStateException("OAUTH_PUBLIC_JWK_INVALID");
            }
            return rsaKey.toPublicJWK();
        } catch (java.text.ParseException failure) {
            throw new IllegalStateException("OAUTH_PUBLIC_JWK_INVALID", failure);
        }
    }

    private static byte[] decodePrivateKey(String value) {
        if (value == null || value.length() > 16_384 || !value.contains("-----BEGIN PRIVATE KEY-----")) {
            throw new IllegalArgumentException("OAUTH_SIGNING_PRIVATE_KEY_INVALID");
        }
        String encoded = value.replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        return Base64.getDecoder().decode(encoded);
    }
}
