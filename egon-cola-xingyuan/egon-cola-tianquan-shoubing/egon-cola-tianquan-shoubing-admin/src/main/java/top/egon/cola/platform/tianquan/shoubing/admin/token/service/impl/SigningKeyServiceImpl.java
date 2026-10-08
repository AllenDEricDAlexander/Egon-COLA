package top.egon.cola.platform.tianquan.shoubing.admin.token.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import java.net.URI;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.HashSet;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.TrustedRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository.OAuthRealmRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.bo.OAuthSigningKeyBO;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.converter.SigningKeyViewConverter;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.dto.PublishSigningKeyDTO;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.enums.OAuthKeyStatusEnum;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.vo.SigningKeyVO;
import top.egon.cola.platform.tianquan.shoubing.admin.token.repository.OAuthSigningKeyRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.token.service.SigningKeyService;

@Service("signingKeyServiceImpl")
@Slf4j
@RequiredArgsConstructor
public class SigningKeyServiceImpl implements SigningKeyService {

    private static final int MINIMUM_RSA_BITS = 3072;
    private static final long PUBLICATION_WINDOW_SECONDS = Duration.ofHours(24).getSeconds();
    private static final Duration KEY_OVERLAP = Duration.ofSeconds(690L);
    private static final Set<OAuthKeyStatusEnum> DIRECTORY_STATUSES = EnumSet.allOf(OAuthKeyStatusEnum.class);
    private static final Set<String> PUBLIC_JWK_FIELDS = Set.of("kty", "kid", "alg", "use", "n", "e");

    @NonNull
    @Qualifier("oAuthSigningKeyRepository")
    private final OAuthSigningKeyRepository keys;
    @NonNull
    @Qualifier("oAuthRealmRepository")
    private final OAuthRealmRepository realms;
    @NonNull
    @Qualifier("oauthKeyCipherComponent")
    private final OAuthKeyCipherComponent keyCipher;
    @NonNull
    @Qualifier("signingKeyViewConverterImpl")
    private final SigningKeyViewConverter views;
    @NonNull
    @Qualifier("jacksonObjectMapper")
    private final ObjectMapper objectMapper;
    @NonNull
    @Qualifier("idpClock")
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public List<SigningKeyVO> list(TrustedRealmBO trustedRealm) {
        TrustedRealmBO realm = requireRealm(trustedRealm);
        OAuthRealmBO currentRealm = realms.findByRealmKey(realm)
                .orElseThrow(() -> new IllegalArgumentException("OAUTH_SIGNING_REALM_NOT_FOUND"));
        return keys.findByStatuses(realm, DIRECTORY_STATUSES, 0L, 8).stream()
                .sorted(java.util.Comparator.comparing(OAuthSigningKeyBO::getKid))
                .map(key -> view(realm, currentRealm, key))
                .toList();
    }

    @Override
    @Transactional
    public SigningKeyVO publish(TrustedRealmBO trustedRealm, PublishSigningKeyDTO command) {
        TrustedRealmBO realm = requireRealm(trustedRealm);
        PublishSigningKeyDTO input = Objects.requireNonNull(command, "command");
        String kid = requiredKid(input.getKid());
        OAuthRealmBO registeredRealm = realms.findByRealmKeyForUpdate(realm)
                .orElseThrow(() -> new IllegalArgumentException("OAUTH_SIGNING_REALM_NOT_FOUND"));
        if (!"ACTIVE".equals(registeredRealm.getStatus())) {
            throw new IllegalStateException("OAUTH_SIGNING_REALM_NOT_ACTIVE");
        }
        if (keys.findByKidForUpdate(realm, kid).isPresent()) {
            throw new IllegalStateException("OAUTH_SIGNING_KEY_ALREADY_EXISTS");
        }
        JsonNode publicJwk = validatedPublicJwk(kid, input.getPublicJwk());
        validateKeyPair(realm, kid, input.getEncryptedPrivateKey(), publicJwk);
        Instant now = clock.instant();
        OAuthSigningKeyBO saved = keys.save(realm, OAuthSigningKeyBO.builder()
                .version(0L)
                .kid(kid)
                .algorithm("RS256")
                .publicJwk(publicJwk)
                .privateKeyCiphertext(input.getEncryptedPrivateKey())
                .status(OAuthKeyStatusEnum.PUBLISHED)
                .publishedAt(now)
                .build(), 0L);
        return view(realm, registeredRealm, saved);
    }

    @Override
    @Transactional
    public SigningKeyVO activate(TrustedRealmBO trustedRealm, String kid, long expectedVersion) {
        TrustedRealmBO realm = requireRealm(trustedRealm);
        String exactKid = requiredKid(kid);
        OAuthRealmBO currentRealm = realms.findByRealmKeyForUpdate(realm)
                .orElseThrow(() -> new IllegalArgumentException("OAUTH_SIGNING_REALM_NOT_FOUND"));
        OAuthSigningKeyBO target = keys.findByKidForUpdate(realm, exactKid)
                .orElseThrow(() -> new IllegalArgumentException("OAUTH_SIGNING_KEY_NOT_FOUND"));
        if (target.getVersion() != expectedVersion) {
            throw new OptimisticLockingFailureException("OAUTH_SIGNING_KEY_VERSION_CONFLICT");
        }
        if (target.getStatus() == OAuthKeyStatusEnum.ACTIVE
                && exactKid.equals(currentRealm.getActiveKid())) {
            return view(realm, currentRealm, target);
        }
        if (target.getStatus() != OAuthKeyStatusEnum.PUBLISHED
                || target.getPublishedAt() == null
                || target.getPublishedAt().plusSeconds(PUBLICATION_WINDOW_SECONDS).isAfter(clock.instant())) {
            throw new IllegalStateException("OAUTH_SIGNING_KEY_PUBLICATION_WINDOW_NOT_ELAPSED");
        }
        validateKeyPair(realm, exactKid, target.getPrivateKeyCiphertext(), target.getPublicJwk());

        long nextRealmVersion = Math.addExact(currentRealm.getVersion(), 1L);
        long nextTrustVersion = Math.addExact(currentRealm.getTrustVersion(), 1L);
        Instant now = clock.instant();
        for (OAuthSigningKeyBO active : keys.findByStatuses(
                realm, EnumSet.of(OAuthKeyStatusEnum.ACTIVE), 0L, 8)) {
            if (!active.getKid().equals(exactKid)) {
                OAuthSigningKeyBO verifyOnly = copy(active)
                        .version(Math.addExact(active.getVersion(), 1L))
                        .status(OAuthKeyStatusEnum.VERIFY_ONLY)
                        .verifyUntil(now.plus(KEY_OVERLAP))
                        .build();
                keys.save(realm, verifyOnly, active.getVersion());
            }
        }
        OAuthSigningKeyBO activeTarget = copy(target)
                .version(Math.addExact(target.getVersion(), 1L))
                .status(OAuthKeyStatusEnum.ACTIVE)
                .activatedAt(target.getActivatedAt() == null ? now : target.getActivatedAt())
                .build();
        OAuthSigningKeyBO savedKey = keys.save(realm, activeTarget, target.getVersion());
        OAuthRealmBO changedRealm = copy(currentRealm)
                .version(nextRealmVersion)
                .trustVersion(nextTrustVersion)
                .activeKid(exactKid)
                .build();
        OAuthRealmBO savedRealm = realms.save(realm, changedRealm, currentRealm.getVersion());
        return view(realm, savedRealm, savedKey);
    }

    @Override
    @Transactional
    public SigningKeyVO retire(TrustedRealmBO trustedRealm, String kid, long expectedVersion) {
        TrustedRealmBO realm = requireRealm(trustedRealm);
        String exactKid = requiredKid(kid);
        OAuthRealmBO currentRealm = realms.findByRealmKeyForUpdate(realm)
                .orElseThrow(() -> new IllegalArgumentException("OAUTH_SIGNING_REALM_NOT_FOUND"));
        OAuthSigningKeyBO target = keys.findByKidForUpdate(realm, exactKid)
                .orElseThrow(() -> new IllegalArgumentException("OAUTH_SIGNING_KEY_NOT_FOUND"));
        if (target.getVersion() != expectedVersion) {
            throw new OptimisticLockingFailureException("OAUTH_SIGNING_KEY_VERSION_CONFLICT");
        }
        if (exactKid.equals(currentRealm.getActiveKid()) || target.getStatus() == OAuthKeyStatusEnum.ACTIVE) {
            throw new IllegalStateException("OAUTH_ACTIVE_SIGNING_KEY_CANNOT_RETIRE");
        }
        if (target.getStatus() == OAuthKeyStatusEnum.RETIRED) {
            return view(realm, currentRealm, target);
        }
        if (target.getStatus() != OAuthKeyStatusEnum.VERIFY_ONLY
                || target.getVerifyUntil() == null || !clock.instant().isAfter(target.getVerifyUntil())) {
            throw new IllegalStateException("OAUTH_SIGNING_KEY_RETENTION_REQUIRED");
        }
        Instant now = clock.instant();
        OAuthSigningKeyBO retired = copy(target)
                .version(Math.addExact(target.getVersion(), 1L))
                .status(OAuthKeyStatusEnum.RETIRED)
                .retiredAt(now)
                .build();
        OAuthSigningKeyBO saved = keys.save(realm, retired, target.getVersion());
        return view(realm, currentRealm, saved);
    }

    private SigningKeyVO view(TrustedRealmBO realm, OAuthRealmBO currentRealm, OAuthSigningKeyBO key) {
        boolean serving = key.getStatus() == OAuthKeyStatusEnum.ACTIVE
                && Objects.equals(currentRealm.getActiveKid(), key.getKid())
                && canLoad(realm, key);
        return views.toTarget(key, realm.getIssuer(), serving);
    }

    private boolean canLoad(TrustedRealmBO realm, OAuthSigningKeyBO key) {
        try {
            validateKeyPair(realm, key.getKid(), key.getPrivateKeyCiphertext(), key.getPublicJwk());
            return true;
        } catch (RuntimeException failure) {
            log.warn("OAuth signing key is not locally loadable; kid={}", key.getKid());
            return false;
        }
    }

    private void validateKeyPair(TrustedRealmBO realm, String kid, String ciphertext, JsonNode publicJwk) {
        RSAKey publicKey = parsePublicJwk(kid, publicJwk);
        String privatePem = keyCipher.decryptPrivateKey(realm, kid, ciphertext);
        byte[] encoded = decodePrivateKey(privatePem);
        try {
            PrivateKey privateKey = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(encoded));
            if (!(privateKey instanceof RSAPrivateCrtKey privateRsa)
                    || publicKey.getModulus().decodeToBigInteger().bitLength() < MINIMUM_RSA_BITS
                    || !publicKey.getModulus().decodeToBigInteger().equals(privateRsa.getModulus())
                    || !publicKey.getPublicExponent().decodeToBigInteger().equals(privateRsa.getPublicExponent())) {
                throw new IllegalArgumentException("OAUTH_SIGNING_KEY_PAIR_MISMATCH");
            }
        } catch (java.security.GeneralSecurityException failure) {
            throw new IllegalArgumentException("OAUTH_SIGNING_PRIVATE_KEY_INVALID", failure);
        } finally {
            Arrays.fill(encoded, (byte) 0);
        }
    }

    private JsonNode validatedPublicJwk(String kid, String raw) {
        if (raw == null || raw.isBlank() || raw.length() > 32_768) {
            throw new IllegalArgumentException("OAUTH_PUBLIC_JWK_INVALID");
        }
        try {
            JsonNode root = objectMapper.readTree(raw);
            if (root == null || !root.isObject() || root.size() != PUBLIC_JWK_FIELDS.size()
                    || !PUBLIC_JWK_FIELDS.containsAll(fieldSet(root))
                    || !"RSA".equals(text(root, "kty")) || !kid.equals(text(root, "kid"))
                    || !"RS256".equals(text(root, "alg")) || !"sig".equals(text(root, "use"))) {
                throw new IllegalArgumentException("OAUTH_PUBLIC_JWK_INVALID");
            }
            parsePublicJwk(kid, root);
            return root;
        } catch (java.io.IOException failure) {
            throw new IllegalArgumentException("OAUTH_PUBLIC_JWK_INVALID", failure);
        }
    }

    private static RSAKey parsePublicJwk(String kid, JsonNode node) {
        try {
            JWK jwk = JWK.parse(node.toString());
            if (!(jwk instanceof RSAKey rsa) || !kid.equals(rsa.getKeyID())
                    || !JWSAlgorithm.RS256.equals(rsa.getAlgorithm()) || !KeyUse.SIGNATURE.equals(rsa.getKeyUse())
                    || rsa.getModulus().decodeToBigInteger().bitLength() < MINIMUM_RSA_BITS) {
                throw new IllegalArgumentException("OAUTH_PUBLIC_JWK_INVALID");
            }
            return rsa.toPublicJWK();
        } catch (java.text.ParseException failure) {
            throw new IllegalArgumentException("OAUTH_PUBLIC_JWK_INVALID", failure);
        }
    }

    private static Set<String> fieldSet(JsonNode node) {
        Set<String> values = new HashSet<>();
        node.fieldNames().forEachRemaining(values::add);
        return values;
    }

    private static String text(JsonNode node, String name) {
        JsonNode value = node.get(name);
        return value == null || !value.isTextual() || value.textValue().isBlank() ? null : value.textValue();
    }

    private static byte[] decodePrivateKey(String pem) {
        if (pem == null || pem.length() > 16_384 || !pem.contains("-----BEGIN PRIVATE KEY-----")) {
            throw new IllegalArgumentException("OAUTH_SIGNING_PRIVATE_KEY_INVALID");
        }
        String encoded = pem.replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        try {
            return Base64.getDecoder().decode(encoded);
        } catch (IllegalArgumentException failure) {
            throw new IllegalArgumentException("OAUTH_SIGNING_PRIVATE_KEY_INVALID", failure);
        }
    }

    private static OAuthSigningKeyBO.OAuthSigningKeyBOBuilder copy(OAuthSigningKeyBO key) {
        return OAuthSigningKeyBO.builder()
                .id(key.getId())
                .version(key.getVersion())
                .kid(key.getKid())
                .algorithm(key.getAlgorithm())
                .publicJwk(key.getPublicJwk())
                .privateKeyCiphertext(key.getPrivateKeyCiphertext())
                .status(key.getStatus())
                .publishedAt(key.getPublishedAt())
                .activatedAt(key.getActivatedAt())
                .lastIssuedAt(key.getLastIssuedAt())
                .verifyUntil(key.getVerifyUntil())
                .retiredAt(key.getRetiredAt())
                .createdAt(key.getCreatedAt())
                .updatedAt(key.getUpdatedAt());
    }

    private static OAuthRealmBO.OAuthRealmBOBuilder copy(OAuthRealmBO realm) {
        return OAuthRealmBO.builder()
                .id(realm.getId())
                .version(realm.getVersion())
                .issuer(realm.getIssuer())
                .realmKey(realm.getRealmKey())
                .realmType(realm.getRealmType())
                .businessTenantId(realm.getBusinessTenantId())
                .storagePartitionId(realm.getStoragePartitionId())
                .status(realm.getStatus())
                .activeKid(realm.getActiveKid())
                .trustVersion(realm.getTrustVersion());
    }

    private static TrustedRealmBO requireRealm(TrustedRealmBO realm) {
        TrustedRealmBO value = Objects.requireNonNull(realm, "trustedRealm");
        value.validateTrustedPartition();
        return value;
    }

    private static String requiredKid(String value) {
        if (value == null || !value.matches("[A-Za-z0-9_-]{1,128}")) {
            throw new IllegalArgumentException("OAUTH_SIGNING_KEY_KID_INVALID");
        }
        return value;
    }
}
