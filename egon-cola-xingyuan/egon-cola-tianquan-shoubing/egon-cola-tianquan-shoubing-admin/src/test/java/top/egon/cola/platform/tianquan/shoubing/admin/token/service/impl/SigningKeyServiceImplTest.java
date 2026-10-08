package top.egon.cola.platform.tianquan.shoubing.admin.token.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import java.net.URI;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.TrustedRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.enums.RealmTypeEnum;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository.OAuthRealmRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.bo.OAuthSigningKeyBO;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.converter.SigningKeyViewConverter;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.dto.PublishSigningKeyDTO;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.enums.OAuthKeyStatusEnum;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.vo.SigningKeyVO;
import top.egon.cola.platform.tianquan.shoubing.admin.token.repository.OAuthSigningKeyRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SigningKeyServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-10-08T00:00:00Z");
    private static final String CIPHERTEXT = "ENC:v1:test-key:AAAAAAAAAAAAAAAA:BBBBBBBBBBBBBBBBBBBBBB";
    private static final TrustedRealmBO TENANT = TrustedRealmBO.builder()
            .issuer(URI.create("https://idp.example.test/tenants/7"))
            .realmKey("tenant:7")
            .realmType(RealmTypeEnum.TENANT)
            .storagePartitionId(7L)
            .businessTenantId(7L)
            .trustVersion(1L)
            .build();

    private final OAuthSigningKeyRepository keys = mock(OAuthSigningKeyRepository.class);
    private final OAuthRealmRepository realms = mock(OAuthRealmRepository.class);
    private final OAuthKeyCipherComponent cipher = mock(OAuthKeyCipherComponent.class);
    private final SigningKeyViewConverter views = mock(SigningKeyViewConverter.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private SigningKeyServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SigningKeyServiceImpl(keys, realms, cipher, views, objectMapper,
                Clock.fixed(NOW, ZoneOffset.UTC));
        when(views.toTarget(any(OAuthSigningKeyBO.class), eq(TENANT.getIssuer()), anyBoolean()))
                .thenAnswer(invocation -> {
                    OAuthSigningKeyBO value = invocation.getArgument(0);
                    return SigningKeyVO.builder().kid(value.getKid()).algorithm(value.getAlgorithm())
                            .status(value.getStatus().name()).issuer(TENANT.getIssuer().toASCIIString())
                            .publicJwk(value.getPublicJwk().toString()).version(value.getVersion())
                            .runtimeServing(invocation.getArgument(2)).build();
                });
    }

    @Test
    void publishesRealmBoundRsaKeyAndStoresOnlyTheCiphertext() throws Exception {
        KeyPair pair = rsaKeyPair();
        when(realms.findByRealmKeyForUpdate(TENANT)).thenReturn(Optional.of(realm("ACTIVE", null, 0L, 0L)));
        when(keys.findByKidForUpdate(TENANT, "key-1")).thenReturn(Optional.empty());
        when(cipher.decryptPrivateKey(TENANT, "key-1", CIPHERTEXT)).thenReturn(privatePem(pair));
        when(keys.save(eq(TENANT), any(OAuthSigningKeyBO.class), eq(0L)))
                .thenAnswer(invocation -> invocation.getArgument(1));

        SigningKeyVO result = service.publish(TENANT, publishCommand("key-1", pair, CIPHERTEXT));

        assertThat(result.getKid()).isEqualTo("key-1");
        assertThat(result.getIssuer()).isEqualTo(TENANT.getIssuer().toASCIIString());
        var saved = org.mockito.ArgumentCaptor.forClass(OAuthSigningKeyBO.class);
        verify(keys).save(eq(TENANT), saved.capture(), eq(0L));
        assertThat(saved.getValue().getPrivateKeyCiphertext()).isEqualTo(CIPHERTEXT);
        assertThat(saved.getValue().getPublicJwk().get("d")).isNull();
    }

    @Test
    void rejectsPublicAndPrivateKeyPairMismatchBeforeSaving() throws Exception {
        KeyPair suppliedPrivate = rsaKeyPair();
        KeyPair differentPublic = rsaKeyPair();
        when(realms.findByRealmKeyForUpdate(TENANT)).thenReturn(Optional.of(realm("ACTIVE", null, 0L, 0L)));
        when(keys.findByKidForUpdate(TENANT, "key-2")).thenReturn(Optional.empty());
        when(cipher.decryptPrivateKey(TENANT, "key-2", CIPHERTEXT)).thenReturn(privatePem(suppliedPrivate));

        assertThatThrownBy(() -> service.publish(TENANT,
                publishCommand("key-2", differentPublic, CIPHERTEXT)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("OAUTH_SIGNING_KEY_PAIR_MISMATCH");
        org.mockito.Mockito.verify(keys, org.mockito.Mockito.never()).save(any(), any(), anyLong());
    }

    @Test
    void activationWaitsForPublicationWindowAndAdvancesRealmTrustVersion() throws Exception {
        KeyPair pair = rsaKeyPair();
        OAuthSigningKeyBO published = key("key-3", pair, OAuthKeyStatusEnum.PUBLISHED,
                NOW.minusSeconds(24 * 60 * 60L), 0L);
        when(realms.findByRealmKeyForUpdate(TENANT)).thenReturn(Optional.of(realm("ACTIVE", null, 4L, 8L)));
        when(keys.findByKidForUpdate(TENANT, "key-3")).thenReturn(Optional.of(published));
        when(keys.findByStatuses(eq(TENANT), eq(EnumSet.of(OAuthKeyStatusEnum.ACTIVE)), eq(0L), eq(8)))
                .thenReturn(List.of());
        when(cipher.decryptPrivateKey(TENANT, "key-3", CIPHERTEXT)).thenReturn(privatePem(pair));
        when(keys.save(eq(TENANT), any(OAuthSigningKeyBO.class), any(Long.class)))
                .thenAnswer(invocation -> invocation.getArgument(1));
        when(realms.save(eq(TENANT), any(OAuthRealmBO.class), eq(4L)))
                .thenAnswer(invocation -> invocation.getArgument(1));

        SigningKeyVO active = service.activate(TENANT, "key-3", 0L);

        assertThat(active.getStatus()).isEqualTo("ACTIVE");
        var savedRealm = org.mockito.ArgumentCaptor.forClass(OAuthRealmBO.class);
        verify(realms).save(eq(TENANT), savedRealm.capture(), eq(4L));
        assertThat(savedRealm.getValue().getActiveKid()).isEqualTo("key-3");
        assertThat(savedRealm.getValue().getTrustVersion()).isEqualTo(9L);
    }

    @Test
    void rejectsStaleKeyVersionWithoutUpdatingEitherRecord() {
        when(realms.findByRealmKeyForUpdate(TENANT)).thenReturn(Optional.of(realm("ACTIVE", null, 1L, 1L)));
        OAuthSigningKeyBO key = OAuthSigningKeyBO.builder().version(2L).kid("key-4").build();
        when(keys.findByKidForUpdate(TENANT, "key-4")).thenReturn(Optional.of(key));

        assertThatThrownBy(() -> service.activate(TENANT, "key-4", 1L))
                .isInstanceOf(org.springframework.dao.OptimisticLockingFailureException.class)
                .hasMessage("OAUTH_SIGNING_KEY_VERSION_CONFLICT");
        org.mockito.Mockito.verify(keys, org.mockito.Mockito.never()).save(any(), any(), anyLong());
        org.mockito.Mockito.verify(realms, org.mockito.Mockito.never()).save(any(), any(), anyLong());
    }

    private static PublishSigningKeyDTO publishCommand(String kid, KeyPair pair, String ciphertext) throws Exception {
        return PublishSigningKeyDTO.builder().kid(kid).encryptedPrivateKey(ciphertext)
                .publicJwk(publicJwk(kid, pair)).build();
    }

    private static OAuthSigningKeyBO key(String kid, KeyPair pair, OAuthKeyStatusEnum status,
            Instant publishedAt, long version) throws Exception {
        return OAuthSigningKeyBO.builder().id(10L).version(version).kid(kid).algorithm("RS256")
                .publicJwk(new ObjectMapper().readTree(publicJwk(kid, pair))).privateKeyCiphertext(CIPHERTEXT)
                .status(status).publishedAt(publishedAt).build();
    }

    private static OAuthRealmBO realm(String status, String activeKid, long version, long trustVersion) {
        return OAuthRealmBO.builder().id(1L).version(version).issuer(TENANT.getIssuer()).realmKey(TENANT.getRealmKey())
                .realmType(RealmTypeEnum.TENANT).businessTenantId(7L).storagePartitionId(7L)
                .status(status).activeKid(activeKid).trustVersion(trustVersion).build();
    }

    private static KeyPair rsaKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(3072);
        return generator.generateKeyPair();
    }

    private static String publicJwk(String kid, KeyPair pair) throws Exception {
        RSAKey key = new RSAKey.Builder((RSAPublicKey) pair.getPublic())
                .privateKey((RSAPrivateKey) pair.getPrivate())
                .keyID(kid).algorithm(JWSAlgorithm.RS256).keyUse(KeyUse.SIGNATURE).build();
        return key.toPublicJWK().toJSONString();
    }

    private static String privatePem(KeyPair pair) {
        return "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getMimeEncoder(64, new byte[] {'\n'}).encodeToString(pair.getPrivate().getEncoded())
                + "\n-----END PRIVATE KEY-----";
    }
}
