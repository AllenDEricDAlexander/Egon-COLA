package top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.JdbcType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mapstruct.factory.Mappers;
import org.mockito.Answers;
import org.mockito.Mockito;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.AnnotatedGenericBeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;
import top.egon.cola.component.common.mybatis.business.EgonColaTenantIdProvider;
import top.egon.cola.component.common.mybatis.business.EgonColaUserIdProvider;
import top.egon.cola.component.common.mybatis.model.EgonColaModelValidationGroups;
import top.egon.cola.component.common.mybatis.model.EgonColaModelValidationUtils;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.dao.OAuthStringListJsonbTypeHandler;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.dao.OAuthAuthorizationMapper;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthAuthorizationAttributesBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthAuthorizationBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthConsentBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthSecuritySnapshotBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.TrustedRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.enums.RealmTypeEnum;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.po.OAuthAuthorizationPO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.po.OAuthConsentPO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.converter.OAuthAuthorizationConverter;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository.impl.OAuthAuthorizationRepositoryImpl;
import top.egon.cola.platform.tianquan.shoubing.admin.token.config.OAuthKeyEncryptionProperties;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.bo.OAuthTokenBO;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.bo.OAuthTokenMetadataBO;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.enums.OAuthTokenTypeEnum;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.po.OAuthTokenPO;
import top.egon.cola.platform.tianquan.shoubing.admin.token.service.impl.OAuthPayloadCipherComponent;
import top.egon.cola.platform.tianquan.shoubing.core.resource.ResourceServerStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

class OAuthAuthorizationPersistenceContractTest {

    private static final String ADMIN_PREFIX =
            "egon-cola-xingyuan/egon-cola-tianquan-shoubing/"
                    + "egon-cola-tianquan-shoubing-admin/src/main/";
    private static final long TENANT_PARTITION_ID = 1_001L;

    @TempDir
    Path temporaryDirectory;

    @Test
    void authorizationTokenAndConsentDdlExposeExactColumnsAndBoundaries() throws IOException {
        String ddl = Files.readString(repositoryRoot().resolve(
                ADMIN_PREFIX + "resources/db/egon-mp/"
                        + "V20261008_001__initialize_oauth_tenant_schema.sql"));
        for (String table : List.of("oauth_authorization", "oauth_token", "oauth_consent")) {
            assertTrue(ddl.contains("CREATE TABLE " + table), "Missing table " + table);
        }
        for (String column : List.of("principal_name", "principal_type", "authorized_scopes",
                "audiences", "attributes_ciphertext", "absolute_expires_at", "revoked_at",
                "token_digest", "token_ciphertext", "metadata_ciphertext", "authorization_pk",
                "token_type", "consumed_at", "client_pk", "authorities", "granted_at")) {
            assertTrue(ddl.contains(column), "Missing OAuth protocol column " + column);
        }
        assertTrue(ddl.contains("ck_oauth_authorization_expiry"));
        assertTrue(ddl.contains("ck_oauth_token_digest"));
        assertTrue(ddl.contains("uk_oauth_token_digest_permanent"));
        assertTrue(ddl.contains("jsonb_typeof(authorized_scopes) = 'array'"));
        assertTrue(ddl.contains("jsonb_typeof(audiences) = 'array'"));
        assertTrue(ddl.contains("jsonb_typeof(authorities) = 'array'"));
    }

    @Test
    void protocolPOsUseExactTablesTypedJsonbArraysAndHideCiphertext() throws Exception {
        assertEquals("oauth_authorization", OAuthAuthorizationPO.class.getAnnotation(TableName.class).value());
        assertTrue(OAuthAuthorizationPO.class.getAnnotation(TableName.class).autoResultMap());
        assertEquals("oauth_token", OAuthTokenPO.class.getAnnotation(TableName.class).value());
        assertEquals("oauth_consent", OAuthConsentPO.class.getAnnotation(TableName.class).value());
        assertEquals(List.class, OAuthAuthorizationPO.class.getDeclaredField("authorizedScopes").getType());
        assertEquals(List.class, OAuthAuthorizationPO.class.getDeclaredField("audiences").getType());
        assertEquals(OAuthStringListJsonbTypeHandler.class,
                OAuthAuthorizationPO.class.getDeclaredField("authorizedScopes")
                        .getAnnotation(TableField.class).typeHandler());
        assertEquals(OAuthStringListJsonbTypeHandler.class,
                OAuthAuthorizationPO.class.getDeclaredField("audiences")
                        .getAnnotation(TableField.class).typeHandler());
        assertEquals(OAuthStringListJsonbTypeHandler.class,
                OAuthConsentPO.class.getDeclaredField("authorities")
                        .getAnnotation(TableField.class).typeHandler());
        assertTrue(OAuthTokenTypeEnum.class.getDeclaredField("code").isAnnotationPresent(EnumValue.class));
        assertEquals("AUTHORIZATION_CODE", OAuthTokenTypeEnum.CODE.getCode());
        assertEquals("ID_TOKEN", OAuthTokenTypeEnum.ID.getCode());

        OAuthAuthorizationPO authorization = new OAuthAuthorizationPO()
                .setAttributesCiphertext("ENC:v1:synthetic:nonce:ciphertext");
        OAuthTokenPO token = new OAuthTokenPO()
                .setTokenDigest("a".repeat(64))
                .setTokenCiphertext("ENC:v1:synthetic:nonce:ciphertext")
                .setMetadataCiphertext("ENC:v1:synthetic:nonce:ciphertext");
        assertFalse(authorization.toString().contains("ENC:v1:synthetic"));
        assertFalse(token.toString().contains("ENC:v1:synthetic"));
        assertFalse(new ObjectMapper().writeValueAsString(authorization).contains("ENC:v1:synthetic"));
        assertFalse(new ObjectMapper().writeValueAsString(token).contains("ENC:v1:synthetic"));
    }

    @Test
    void protocolPORequiredFieldsAndJsonArrayElementsValidateInUpdateGroup() {
        try (var validatorFactory = Validation.buildDefaultValidatorFactory()) {
            var validator = validatorFactory.getValidator();
            Set<String> authorizationPaths = validator.validate(
                            new OAuthAuthorizationPO(), EgonColaModelValidationGroups.Update.class).stream()
                    .map(violation -> violation.getPropertyPath().toString())
                    .collect(Collectors.toSet());
            assertTrue(authorizationPaths.containsAll(Set.of(
                    "clientPk", "principalName", "principalType", "grantType", "authorizedScopes",
                    "audiences", "status", "attributesCiphertext", "absoluteExpiresAt")));
            Set<String> invalidScopePaths = validator.validate(
                            new OAuthAuthorizationPO().setAuthorizedScopes(List.of("")),
                            EgonColaModelValidationGroups.Update.class).stream()
                    .map(violation -> violation.getPropertyPath().toString())
                    .collect(Collectors.toSet());
            assertTrue(invalidScopePaths.stream().anyMatch(path -> path.contains("authorizedScopes")));

            Set<String> tokenPaths = validator.validate(
                            new OAuthTokenPO(), EgonColaModelValidationGroups.Update.class).stream()
                    .map(violation -> violation.getPropertyPath().toString())
                    .collect(Collectors.toSet());
            assertTrue(tokenPaths.containsAll(Set.of(
                    "authorizationPk", "tokenType", "tokenDigest", "issuedAt", "expiresAt", "metadataCiphertext")));

            Set<String> consentPaths = validator.validate(
                            new OAuthConsentPO(), EgonColaModelValidationGroups.Update.class).stream()
                    .map(violation -> violation.getPropertyPath().toString())
                    .collect(Collectors.toSet());
            assertTrue(consentPaths.containsAll(Set.of(
                    "clientPk", "subject", "authorities", "grantedAt", "status")));

            Set<String> authorizationDeletePaths = validator.validate(
                            new OAuthAuthorizationPO(), EgonColaModelValidationGroups.Delete.class).stream()
                    .map(violation -> violation.getPropertyPath().toString())
                    .collect(Collectors.toSet());
            assertTrue(authorizationDeletePaths.containsAll(Set.of("id", "version")));
            Set<String> tokenDeletePaths = validator.validate(
                            new OAuthTokenPO(), EgonColaModelValidationGroups.Delete.class).stream()
                    .map(violation -> violation.getPropertyPath().toString())
                    .collect(Collectors.toSet());
            assertTrue(tokenDeletePaths.containsAll(Set.of("id", "version")));
            Set<String> consentDeletePaths = validator.validate(
                            new OAuthConsentPO(), EgonColaModelValidationGroups.Delete.class).stream()
                    .map(violation -> violation.getPropertyPath().toString())
                    .collect(Collectors.toSet());
            assertTrue(consentDeletePaths.containsAll(Set.of("id", "version")));
        }
    }

    @Test
    void existingJsonbStringArrayHandlerBindsScopeAndConsentListsAsOther() throws Exception {
        OAuthStringListJsonbTypeHandler handler = new OAuthStringListJsonbTypeHandler();
        PreparedStatement statement = Mockito.mock(PreparedStatement.class);
        handler.setNonNullParameter(statement, 1, List.of("openid", "profile"), JdbcType.OTHER);
        Mockito.verify(statement).setObject(1, "[\"openid\",\"profile\"]", Types.OTHER);
        ResultSet resultSet = Mockito.mock(ResultSet.class);
        Mockito.when(resultSet.getString("authorities")).thenReturn("[\"openid\",\"profile\"]");
        assertEquals(List.of("openid", "profile"), handler.getNullableResult(resultSet, "authorities"));
        Mockito.when(resultSet.getString("authorities")).thenReturn("[false]");
        assertThrows(SQLException.class, () -> handler.getNullableResult(resultSet, "authorities"));
    }

    @Test
    void authorizationTokenAndConsentMapperXmlParsesWithTenantVersionAndLockPredicates() throws IOException {
        assertMapperContract(
                "mapper/oauth/OAuthAuthorizationMapper.xml",
                "oauth_authorization",
                "top.egon.cola.platform.tianquan.shoubing.admin.oauth.dao.OAuthAuthorizationMapper",
                List.of("selectActiveById", "selectByIdForUpdate", "selectByPrincipalName",
                        "selectExpired", "selectActiveByIds"));
        assertMapperContract(
                "mapper/token/OAuthTokenMapper.xml",
                "oauth_token",
                "top.egon.cola.platform.tianquan.shoubing.admin.token.dao.OAuthTokenMapper",
                List.of("selectByTokenDigest", "selectByTokenDigestForUpdate", "selectByAuthorization",
                        "selectExpired", "selectActiveById", "selectActiveByIds"));
        assertMapperContract(
                "mapper/oauth/OAuthConsentMapper.xml",
                "oauth_consent",
                "top.egon.cola.platform.tianquan.shoubing.admin.oauth.dao.OAuthConsentMapper",
                List.of("selectConsent", "selectConsentForUpdate", "selectActiveById", "selectActiveByIds"));
    }

    @Test
    void payloadCipherUsesRealmPurposeRecordAADAndRejectsTamperingAndMisconfiguration() throws Exception {
        String cipherName =
                "top.egon.cola.platform.tianquan.shoubing.admin.token.service.impl.OAuthPayloadCipherComponent";
        assertTrue(classExists(cipherName), "Missing shared JDK payload cipher");
        Path keyFile = temporaryDirectory.resolve("test-kek.bin");
        Files.write(keyFile, new byte[32]);
        Files.setPosixFilePermissions(keyFile, PosixFilePermissions.fromString("rw-------"));
        OAuthKeyEncryptionProperties keyProperties = new OAuthKeyEncryptionProperties()
                .setKeyId("test-kek-v1").setKeyFile(keyFile);
        assertEquals("test-kek-v1", keyProperties.getKeyId());
        assertEquals(keyFile, keyProperties.getKeyFile());
        assertTrue(Files.isRegularFile(keyFile));
        assertTrue(Files.isReadable(keyFile));
        Object cipher = createCipher(cipherName, keyProperties);
        Class<?> cipherType = Class.forName(cipherName);
        var encrypt = cipherType.getMethod("encrypt", TrustedRealmBO.class,
                String.class, String.class, String.class);
        var decrypt = cipherType.getMethod("decrypt", TrustedRealmBO.class,
                String.class, String.class, String.class);
        TrustedRealmBO realm = trustedTenant();
        String plaintext = "{\"schemaVersion\":1,\"synthetic\":true}";
        String envelope = (String) encrypt.invoke(cipher, realm, "token-metadata", "a".repeat(64), plaintext);
        assertTrue(envelope.startsWith("ENC:v1:test-kek-v1:"));
        assertEquals(plaintext,
                decrypt.invoke(cipher, realm, "token-metadata", "a".repeat(64), envelope));
        assertThrows(InvocationTargetException.class,
                () -> decrypt.invoke(cipher, trustedPlatform(), "token-metadata", "a".repeat(64), envelope));
        TrustedRealmBO otherIssuer = trustedTenant().setIssuer(URI.create("https://auth.example/tenant/1001/other"));
        assertThrows(InvocationTargetException.class,
                () -> decrypt.invoke(cipher, otherIssuer, "token-metadata", "a".repeat(64), envelope));
        assertThrows(InvocationTargetException.class,
                () -> decrypt.invoke(cipher, realm, "authorization-attributes", "a".repeat(64), envelope));
        assertThrows(InvocationTargetException.class,
                () -> decrypt.invoke(cipher, realm, "token-metadata", "b".repeat(64), envelope));

        String[] parts = envelope.split(":", -1);
        assertEquals(5, parts.length);
        parts[4] = replaceFirstCipherByte(parts[4]);
        String tamperedEnvelope = String.join(":", parts);
        assertThrows(InvocationTargetException.class,
                () -> decrypt.invoke(cipher, realm, "token-metadata", "a".repeat(64), tamperedEnvelope));
        String[] unknownKeyParts = envelope.split(":", -1);
        unknownKeyParts[2] = "unknown-key-id";
        String unknownKey = String.join(":", unknownKeyParts);
        assertThrows(InvocationTargetException.class,
                () -> decrypt.invoke(cipher, realm, "token-metadata", "a".repeat(64), unknownKey));
        assertThrows(InvocationTargetException.class,
                () -> decrypt.invoke(cipher, realm, "token-metadata", "a".repeat(64), "ENC:v1:truncated"));

        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        var authorizationConverter = Mappers.getMapper(
                top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.converter.OAuthAuthorizationConverter.class);
        OAuthAuthorizationBO authorization = authorization(authorizationRow(101L, 0L), 0L);
        OAuthAuthorizationPO encryptedAuthorization = authorizationConverter.toSource(
                realm, (top.egon.cola.platform.tianquan.shoubing.admin.token.service.impl.OAuthPayloadCipherComponent) cipher,
                objectMapper, "101", authorization);
        assertTrue(encryptedAuthorization.getAttributesCiphertext().startsWith("ENC:v1:test-kek-v1:"));
        assertFalse(encryptedAuthorization.getAttributesCiphertext().contains("synthetic-authorization-nonce"));
        OAuthAuthorizationBO restoredAuthorization = authorizationConverter.toTarget(
                realm, (top.egon.cola.platform.tianquan.shoubing.admin.token.service.impl.OAuthPayloadCipherComponent) cipher,
                objectMapper, "101", encryptedAuthorization);
        assertEquals(authorization.getAttributes(), restoredAuthorization.getAttributes());
        assertThrows(IllegalArgumentException.class, () -> authorizationConverter.toTarget(
                realm, (top.egon.cola.platform.tianquan.shoubing.admin.token.service.impl.OAuthPayloadCipherComponent) cipher,
                objectMapper, "102", encryptedAuthorization));
        assertFalse(objectMapper.writeValueAsString(restoredAuthorization).contains("synthetic-authorization-nonce"));
        assertFalse(restoredAuthorization.toString().contains("synthetic-authorization-nonce"));
        OAuthAuthorizationAttributesBO unsupportedAttributes = OAuthAuthorizationAttributesBO.builder()
                .schemaVersion(2).securitySnapshot(authorization.getAttributes().getSecuritySnapshot()).build();
        OAuthAuthorizationBO unsupportedAuthorization = authorization(authorizationRow(101L, 0L), 0L)
                .setAttributes(unsupportedAttributes);
        assertThrows(IllegalArgumentException.class, () -> authorizationConverter.toSource(
                realm, (top.egon.cola.platform.tianquan.shoubing.admin.token.service.impl.OAuthPayloadCipherComponent) cipher,
                objectMapper, "101", unsupportedAuthorization));
        assertFalse(objectMapper.writeValueAsString(restoredAuthorization).contains("legacy-user-sub"));

        var tokenConverter = Mappers.getMapper(
                top.egon.cola.platform.tianquan.shoubing.admin.token.domain.converter.OAuthTokenConverter.class);
        OAuthTokenBO token = token(tokenRow(111L, 0L), 0L);
        OAuthTokenPO encryptedToken = tokenConverter.toSource(
                realm, (top.egon.cola.platform.tianquan.shoubing.admin.token.service.impl.OAuthPayloadCipherComponent) cipher,
                objectMapper, token.getTokenDigest(), token);
        assertTrue(encryptedToken.getTokenCiphertext().startsWith("ENC:v1:test-kek-v1:"));
        assertTrue(encryptedToken.getMetadataCiphertext().startsWith("ENC:v1:test-kek-v1:"));
        assertFalse(encryptedToken.getTokenCiphertext().contains("synthetic-token"));
        OAuthTokenBO restoredToken = tokenConverter.toTarget(
                realm, (top.egon.cola.platform.tianquan.shoubing.admin.token.service.impl.OAuthPayloadCipherComponent) cipher,
                objectMapper, token.getTokenDigest(), encryptedToken);
        assertEquals("synthetic-token", restoredToken.getTokenValue());
        assertEquals(token.getMetadata(), restoredToken.getMetadata());
        assertFalse(objectMapper.writeValueAsString(restoredToken).contains("synthetic-token"));
        assertFalse(restoredToken.toString().contains("synthetic-token"));
        OAuthTokenMetadataBO unsupportedMetadata = OAuthTokenMetadataBO.builder().schemaVersion(2).build();
        OAuthTokenBO unsupportedToken = token(tokenRow(111L, 0L), 0L).setMetadata(unsupportedMetadata);
        assertThrows(IllegalArgumentException.class, () -> tokenConverter.toSource(
                realm, (top.egon.cola.platform.tianquan.shoubing.admin.token.service.impl.OAuthPayloadCipherComponent) cipher,
                objectMapper, token.getTokenDigest(), unsupportedToken));

        OAuthKeyEncryptionProperties invalid = new OAuthKeyEncryptionProperties()
                .setKeyId("test-kek-v1").setKeyFile(temporaryDirectory.resolve("missing-key.bin"));
        Object invalidCipher = cipherType.getConstructor(OAuthKeyEncryptionProperties.class).newInstance(invalid);
        assertThrows(InvocationTargetException.class,
                () -> cipherType.getMethod("initializeKey").invoke(invalidCipher));
        Path shortKeyFile = temporaryDirectory.resolve("short-kek.bin");
        Files.write(shortKeyFile, new byte[31]);
        Files.setPosixFilePermissions(shortKeyFile, PosixFilePermissions.fromString("rw-------"));
        Object shortKeyCipher = cipherType.getConstructor(OAuthKeyEncryptionProperties.class).newInstance(
                new OAuthKeyEncryptionProperties().setKeyId("test-kek-v1").setKeyFile(shortKeyFile));
        assertThrows(InvocationTargetException.class,
                () -> cipherType.getMethod("initializeKey").invoke(shortKeyCipher));
    }

    @Test
    void keyEnvironmentReferencesMatchAcrossProfilesWithoutCommittedMaterial() throws IOException {
        String base = Files.readString(repositoryRoot().resolve(ADMIN_PREFIX + "resources/application.yml"));
        String local = Files.readString(repositoryRoot().resolve(ADMIN_PREFIX + "resources/application-local.yml"));
        for (String profile : List.of(base, local)) {
            assertTrue(profile.contains("key-id: ${EGON_TIANQUAN_SHOUBING_OAUTH_KEK_ID}"));
            assertTrue(profile.contains("key-file: ${EGON_TIANQUAN_SHOUBING_OAUTH_KEK_FILE}"));
        }
    }

    @Test
    void allOAuthPersistenceConvertersUsePOAsBaseConverterSource() throws Exception {
        assertConverterDirection(
                "top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.converter.OAuthAuthorizationConverter",
                "top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.po.OAuthAuthorizationPO",
                "top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthAuthorizationBO");
        assertConverterDirection(
                "top.egon.cola.platform.tianquan.shoubing.admin.token.domain.converter.OAuthTokenConverter",
                "top.egon.cola.platform.tianquan.shoubing.admin.token.domain.po.OAuthTokenPO",
                "top.egon.cola.platform.tianquan.shoubing.admin.token.domain.bo.OAuthTokenBO");
        assertConverterDirection(
                "top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.converter.OAuthConsentConverter",
                "top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.po.OAuthConsentPO",
                "top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthConsentBO");
    }

    @Test
    void namedPersistenceTypesUseSpringGeneratedMapperAndConverterBeanNames() throws Exception {
        List<String> types = List.of(
                "top.egon.cola.platform.tianquan.shoubing.admin.oauth.dao.OAuthAuthorizationMapper",
                "top.egon.cola.platform.tianquan.shoubing.admin.token.dao.OAuthTokenMapper",
                "top.egon.cola.platform.tianquan.shoubing.admin.oauth.dao.OAuthConsentMapper",
                "top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.converter.OAuthAuthorizationConverterImpl",
                "top.egon.cola.platform.tianquan.shoubing.admin.token.domain.converter.OAuthTokenConverterImpl",
                "top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.converter.OAuthConsentConverterImpl",
                "top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository.impl.OAuthAuthorizationRepositoryImpl",
                "top.egon.cola.platform.tianquan.shoubing.admin.token.repository.impl.OAuthTokenRepositoryImpl",
                "top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository.impl.OAuthConsentRepositoryImpl"
        );
        for (String type : types) {
            assertTrue(classExists(type), "Missing authorization/token/consent persistence class " + type);
        }
        assertEquals("OAuthAuthorizationMapper", generatedSpringBeanName(Class.forName(types.get(0))));
        assertEquals("OAuthTokenMapper", generatedSpringBeanName(Class.forName(types.get(1))));
        assertEquals("OAuthConsentMapper", generatedSpringBeanName(Class.forName(types.get(2))));
        assertEquals("OAuthAuthorizationConverterImpl", generatedSpringBeanName(Class.forName(types.get(3))));
        assertEquals("OAuthTokenConverterImpl", generatedSpringBeanName(Class.forName(types.get(4))));
        assertEquals("OAuthConsentConverterImpl", generatedSpringBeanName(Class.forName(types.get(5))));
    }

    @Test
    void trustedAuthorizationAndConsentQueriesRestoreTenantAndActorMdc() throws Exception {
        assertScopedRepositoryMdc(
                "top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository.impl.OAuthAuthorizationRepositoryImpl",
                "top.egon.cola.platform.tianquan.shoubing.admin.oauth.dao.OAuthAuthorizationMapper",
                "top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.converter.OAuthAuthorizationConverter",
                "findById", new Class<?>[]{TrustedRealmBO.class, Long.class},
                new Object[]{trustedTenant(), 101L}, "selectActiveById", true);
        assertScopedRepositoryMdc(
                "top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository.impl.OAuthConsentRepositoryImpl",
                "top.egon.cola.platform.tianquan.shoubing.admin.oauth.dao.OAuthConsentMapper",
                "top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.converter.OAuthConsentConverter",
                "findConsent", new Class<?>[]{TrustedRealmBO.class, Long.class, String.class},
                new Object[]{trustedTenant(), 901L, "legacy-user-sub"}, "selectConsent", false);
    }

    @Test
    void staleExpectedVersionDoesNotUpdateAuthorizationTokenOrConsentRows() throws Exception {
        EgonColaMybatisPlusProperties properties = new EgonColaMybatisPlusProperties();
        properties.getTenantId().setMdcKey(EgonColaTenantIdProvider.DEFAULT_MDC_KEY);
        properties.getAudit().setUserIdMdcKey("userId");
        EgonColaUserIdProvider userIdProvider = mock(EgonColaUserIdProvider.class);

        OAuthAuthorizationPO authorizationRow = authorizationRow(111L, 3L);
        AtomicBoolean authorizationUpdated = new AtomicBoolean();
        String authorizationMapperName =
                "top.egon.cola.platform.tianquan.shoubing.admin.oauth.dao.OAuthAuthorizationMapper";
        assertTrue(classExists(authorizationMapperName), "Missing mapper " + authorizationMapperName);
        Class<?> authorizationMapperType = Class.forName(authorizationMapperName);
        Object authorizationMapper = Mockito.mock(authorizationMapperType, invocation -> {
            if (invocation.getMethod().getName().equals("selectByIdForUpdate")) {
                return authorizationRow;
            }
            if (invocation.getMethod().getName().equals("updateExpectedVersion")) {
                authorizationUpdated.set(true);
            }
            return Answers.RETURNS_DEFAULTS.answer(invocation);
        });
        String authorizationConverterName =
                "top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.converter.OAuthAuthorizationConverter";
        assertTrue(classExists(authorizationConverterName), "Missing converter " + authorizationConverterName);
        Class<?> authorizationConverterType = Class.forName(authorizationConverterName);
        Object authorizationConverter = Mappers.getMapper((Class) authorizationConverterType);
        Class<?> cipherType = Class.forName(
                "top.egon.cola.platform.tianquan.shoubing.admin.token.service.impl.OAuthPayloadCipherComponent");
        Object cipher = Mockito.mock(cipherType, Answers.RETURNS_DEFAULTS);
        Class<?> authorizationRepositoryType = Class.forName(
                "top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository.impl.OAuthAuthorizationRepositoryImpl");
        Object authorizationRepository = authorizationRepositoryType.getConstructor(authorizationMapperType,
                EgonColaMybatisPlusProperties.class, authorizationConverterType, cipherType,
                ObjectMapper.class, EgonColaUserIdProvider.class, Clock.class)
                .newInstance(authorizationMapper, properties, authorizationConverter, cipher,
                        new ObjectMapper().findAndRegisterModules(), userIdProvider, Clock.systemUTC());
        OAuthAuthorizationBO staleAuthorization = authorization(authorizationRow, 2L);
        var authorizationSave = authorizationRepositoryType.getMethod("save", TrustedRealmBO.class,
                OAuthAuthorizationBO.class, long.class);
        InvocationTargetException authorizationFailure = assertThrows(InvocationTargetException.class,
                () -> authorizationSave.invoke(authorizationRepository, trustedTenant(), staleAuthorization, 1L));
        assertInstanceOf(org.springframework.dao.OptimisticLockingFailureException.class,
                authorizationFailure.getCause());
        assertFalse(authorizationUpdated.get());

        OAuthTokenPO tokenRow = tokenRow(121L, 3L);
        AtomicBoolean tokenUpdated = new AtomicBoolean();
        String tokenMapperName = "top.egon.cola.platform.tianquan.shoubing.admin.token.dao.OAuthTokenMapper";
        assertTrue(classExists(tokenMapperName), "Missing mapper " + tokenMapperName);
        Class<?> tokenMapperType = Class.forName(tokenMapperName);
        Object tokenMapper = Mockito.mock(tokenMapperType, invocation -> {
            if (invocation.getMethod().getName().equals("selectByTokenDigestForUpdate")) {
                return tokenRow;
            }
            if (invocation.getMethod().getName().equals("updateExpectedVersion")) {
                tokenUpdated.set(true);
            }
            return Answers.RETURNS_DEFAULTS.answer(invocation);
        });
        String tokenConverterName =
                "top.egon.cola.platform.tianquan.shoubing.admin.token.domain.converter.OAuthTokenConverter";
        assertTrue(classExists(tokenConverterName), "Missing converter " + tokenConverterName);
        Class<?> tokenConverterType = Class.forName(tokenConverterName);
        Object tokenConverter = Mappers.getMapper((Class) tokenConverterType);
        Class<?> tokenRepositoryType = Class.forName(
                "top.egon.cola.platform.tianquan.shoubing.admin.token.repository.impl.OAuthTokenRepositoryImpl");
        Object tokenRepository = tokenRepositoryType.getConstructor(tokenMapperType,
                EgonColaMybatisPlusProperties.class, tokenConverterType, cipherType,
                ObjectMapper.class, EgonColaUserIdProvider.class, Clock.class)
                .newInstance(tokenMapper, properties, tokenConverter, cipher,
                        new ObjectMapper().findAndRegisterModules(), userIdProvider, Clock.systemUTC());
        OAuthTokenBO staleToken = token(tokenRow, 2L);
        var tokenSave = tokenRepositoryType.getMethod("save", TrustedRealmBO.class, OAuthTokenBO.class, long.class);
        InvocationTargetException tokenFailure = assertThrows(InvocationTargetException.class,
                () -> tokenSave.invoke(tokenRepository, trustedTenant(), staleToken, 1L));
        assertInstanceOf(org.springframework.dao.OptimisticLockingFailureException.class, tokenFailure.getCause());
        assertFalse(tokenUpdated.get());

        OAuthConsentPO consentRow = consentRow(131L, 3L);
        AtomicBoolean consentUpdated = new AtomicBoolean();
        String consentMapperName = "top.egon.cola.platform.tianquan.shoubing.admin.oauth.dao.OAuthConsentMapper";
        assertTrue(classExists(consentMapperName), "Missing mapper " + consentMapperName);
        Class<?> consentMapperType = Class.forName(consentMapperName);
        Object consentMapper = Mockito.mock(consentMapperType, invocation -> {
            if (invocation.getMethod().getName().equals("selectConsentForUpdate")) {
                return consentRow;
            }
            if (invocation.getMethod().getName().equals("updateExpectedVersion")) {
                consentUpdated.set(true);
            }
            return Answers.RETURNS_DEFAULTS.answer(invocation);
        });
        String consentConverterName =
                "top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.converter.OAuthConsentConverter";
        assertTrue(classExists(consentConverterName), "Missing converter " + consentConverterName);
        Class<?> consentConverterType = Class.forName(consentConverterName);
        Object consentConverter = Mappers.getMapper((Class) consentConverterType);
        Class<?> consentRepositoryType = Class.forName(
                "top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository.impl.OAuthConsentRepositoryImpl");
        Object consentRepository = consentRepositoryType.getConstructor(consentMapperType,
                EgonColaMybatisPlusProperties.class, consentConverterType,
                EgonColaUserIdProvider.class, Clock.class)
                .newInstance(consentMapper, properties, consentConverter, userIdProvider, Clock.systemUTC());
        OAuthConsentBO staleConsent = consent(consentRow, 2L);
        var consentSave = consentRepositoryType.getMethod("save", TrustedRealmBO.class, OAuthConsentBO.class,
                long.class);
        InvocationTargetException consentFailure = assertThrows(InvocationTargetException.class,
                () -> consentSave.invoke(consentRepository, trustedTenant(), staleConsent, 1L));
        assertInstanceOf(org.springframework.dao.OptimisticLockingFailureException.class, consentFailure.getCause());
        assertFalse(consentUpdated.get());
    }

    @Test
    void explicitAuthorizationIdIsPreservedOnFirstInsert() throws Exception {
        long expectedId = 9_100_001L;
        OAuthAuthorizationMapper mapper = Mockito.mock(OAuthAuthorizationMapper.class);
        OAuthPayloadCipherComponent cipher = Mockito.mock(OAuthPayloadCipherComponent.class);
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        AtomicReference<String> attributesJson = new AtomicReference<>();
        when(cipher.encrypt(any(TrustedRealmBO.class), eq("authorization-attributes"), eq(Long.toString(expectedId)),
                any(String.class))).thenAnswer(invocation -> {
                    attributesJson.set(invocation.getArgument(3));
                    return "ENC:v1:test:nonce:ciphertext";
                });
        when(cipher.decrypt(any(TrustedRealmBO.class), eq("authorization-attributes"), eq(Long.toString(expectedId)),
                any(String.class))).thenAnswer(invocation -> attributesJson.get());
        when(mapper.selectByIdForUpdate(TENANT_PARTITION_ID, expectedId)).thenReturn(null);
        doAnswer(invocation -> 1).when(mapper).insert(any(OAuthAuthorizationPO.class));

        OAuthAuthorizationRepositoryImpl repository = new OAuthAuthorizationRepositoryImpl(
                mapper,
                scopedProperties(),
                Mappers.getMapper(OAuthAuthorizationConverter.class),
                cipher,
                objectMapper,
                mock(EgonColaUserIdProvider.class),
                Clock.systemUTC());
        OAuthAuthorizationBO authorization = authorization(authorizationRow(expectedId, 0L), 0L)
                .setId(expectedId)
                .setVersion(0L);

        try (ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory()) {
            EgonColaModelValidationUtils.Binding binding = EgonColaModelValidationUtils.current() == null
                    ? EgonColaModelValidationUtils.initialize(
                            new ValidationUtils(validatorFactory.getValidator()),
                            "OAuthAuthorizationPersistenceContractTest")
                    : null;
            try {
                OAuthAuthorizationBO saved = repository.save(trustedTenant(), authorization, 0L);
                assertEquals(expectedId, saved.getId());
                org.mockito.ArgumentCaptor<OAuthAuthorizationPO> inserted =
                        org.mockito.ArgumentCaptor.forClass(OAuthAuthorizationPO.class);
                Mockito.verify(mapper).insert(inserted.capture());
                assertEquals(expectedId, inserted.getValue().getId());
                assertEquals(0L, inserted.getValue().getVersion());
            } finally {
                if (binding != null) {
                    binding.close();
                }
            }
        }
    }

    @Test
    void persistenceMappersConvertersAndRepositoryImplementationsArePresent() throws Exception {
        List<String> types = List.of(
                "top.egon.cola.platform.tianquan.shoubing.admin.oauth.dao.OAuthAuthorizationMapper",
                "top.egon.cola.platform.tianquan.shoubing.admin.token.dao.OAuthTokenMapper",
                "top.egon.cola.platform.tianquan.shoubing.admin.oauth.dao.OAuthConsentMapper",
                "top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.converter.OAuthAuthorizationConverterImpl",
                "top.egon.cola.platform.tianquan.shoubing.admin.token.domain.converter.OAuthTokenConverterImpl",
                "top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.converter.OAuthConsentConverterImpl",
                "top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository.impl.OAuthAuthorizationRepositoryImpl",
                "top.egon.cola.platform.tianquan.shoubing.admin.token.repository.impl.OAuthTokenRepositoryImpl",
                "top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository.impl.OAuthConsentRepositoryImpl"
        );
        for (String name : types) {
            assertTrue(classExists(name), "Missing persistence type " + name);
        }
        assertEquals("OAuthAuthorizationMapper", generatedSpringBeanName(Class.forName(types.get(0))));
        assertEquals("OAuthTokenMapper", generatedSpringBeanName(Class.forName(types.get(1))));
        assertEquals("OAuthConsentMapper", generatedSpringBeanName(Class.forName(types.get(2))));
        assertEquals("OAuthAuthorizationConverterImpl", generatedSpringBeanName(Class.forName(types.get(3))));
        assertEquals("OAuthTokenConverterImpl", generatedSpringBeanName(Class.forName(types.get(4))));
        assertEquals("OAuthConsentConverterImpl", generatedSpringBeanName(Class.forName(types.get(5))));
    }

    @Test
    void scopedRepositoryQueriesRestoreTenantAndActorMdcAfterSuccessAndFailure() throws Exception {
        assertScopedRepositoryMdc(
                "top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository.impl.OAuthAuthorizationRepositoryImpl",
                "top.egon.cola.platform.tianquan.shoubing.admin.oauth.dao.OAuthAuthorizationMapper",
                "top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.converter.OAuthAuthorizationConverter",
                "findById", new Class<?>[]{TrustedRealmBO.class, Long.class},
                new Object[]{trustedTenant(), 101L}, "selectActiveById", true);
        assertScopedRepositoryMdc(
                "top.egon.cola.platform.tianquan.shoubing.admin.token.repository.impl.OAuthTokenRepositoryImpl",
                "top.egon.cola.platform.tianquan.shoubing.admin.token.dao.OAuthTokenMapper",
                "top.egon.cola.platform.tianquan.shoubing.admin.token.domain.converter.OAuthTokenConverter",
                "findByTokenDigest", new Class<?>[]{TrustedRealmBO.class, String.class},
                new Object[]{trustedTenant(), "a".repeat(64)}, "selectByTokenDigest", true);
        assertScopedRepositoryMdc(
                "top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository.impl.OAuthConsentRepositoryImpl",
                "top.egon.cola.platform.tianquan.shoubing.admin.oauth.dao.OAuthConsentMapper",
                "top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.converter.OAuthConsentConverter",
                "findConsent", new Class<?>[]{TrustedRealmBO.class, Long.class, String.class},
                new Object[]{trustedTenant(), 901L, "legacy-user-sub"}, "selectConsent", false);
    }

    private void assertMapperContract(
            String resource,
            String table,
            String namespace,
            List<String> statements) throws IOException {
        Path path = repositoryRoot().resolve(ADMIN_PREFIX + "resources/" + resource);
        assertTrue(Files.isRegularFile(path), "Missing mapper XML " + resource);
        String xml = Files.readString(path).toLowerCase();
        assertTrue(xml.contains("from " + table), "Wrong table mapping " + resource);
        assertTrue(xml.contains("tenant_id = #{tenantid}"), "Missing tenant predicate " + resource);
        assertTrue(xml.contains("deleted_at is null"), "Missing active predicate " + resource);
        assertTrue(xml.contains("version = #{expectedversion}"), "Missing expectedVersion predicate " + resource);
        assertTrue(xml.contains("for update"), "Missing command lock " + resource);
        assertFalse(xml.contains("${"), "Unsafe SQL interpolation " + resource);
        Configuration configuration = new Configuration();
        try (InputStream input = Files.newInputStream(path)) {
            new XMLMapperBuilder(input, configuration, path.toString(), configuration.getSqlFragments()).parse();
        }
        for (String statement : statements) {
            assertTrue(configuration.hasStatement(namespace + "." + statement, false),
                    "Missing named SQL statement " + namespace + "." + statement);
        }
        assertTrue(configuration.hasStatement(namespace + ".updateExpectedVersion", false));
        assertTrue(configuration.hasStatement(namespace + ".deleteVersionedById", false));
    }

    private static Object createCipher(String className, OAuthKeyEncryptionProperties properties)
            throws Exception {
        Class<?> type = Class.forName(className);
        Object cipher = type.getConstructor(OAuthKeyEncryptionProperties.class).newInstance(properties);
        type.getMethod("initializeKey").invoke(cipher);
        return cipher;
    }

    private static void assertConverterDirection(String converterName, String sourceName, String targetName)
            throws Exception {
        assertTrue(classExists(converterName), "Missing MapStruct converter " + converterName);
        Class<?> converter = Class.forName(converterName);
        for (Type genericInterface : converter.getGenericInterfaces()) {
            if (genericInterface instanceof ParameterizedType parameterized
                    && parameterized.getRawType().getTypeName().equals(
                    "top.egon.cola.component.common.core.converter.BaseConverter")) {
                assertEquals(sourceName, parameterized.getActualTypeArguments()[0].getTypeName());
                assertEquals(targetName, parameterized.getActualTypeArguments()[1].getTypeName());
                return;
            }
        }
        throw new AssertionError("Converter must use BaseConverter<PO,BO>: " + converterName);
    }

    private static void assertScopedRepositoryMdc(
            String repositoryName,
            String mapperName,
            String converterName,
            String methodName,
            Class<?>[] methodTypes,
            Object[] methodArguments,
            String mapperMethod,
            boolean encrypted) throws Exception {
        assertTrue(classExists(repositoryName), "Missing repository " + repositoryName);
        Class<?> mapperType = Class.forName(mapperName);
        Class<?> converterType = Class.forName(converterName);
        List<String> observed = new ArrayList<>();
        AtomicBoolean fail = new AtomicBoolean();
        Object mapper = Mockito.mock(mapperType, invocation -> {
            if (invocation.getMethod().getName().equals(mapperMethod)) {
                observed.add(MDC.get(EgonColaTenantIdProvider.DEFAULT_MDC_KEY) + "|" + MDC.get("userId"));
                if (fail.get()) {
                    throw new IllegalStateException("expected mapper failure");
                }
            }
            return Answers.RETURNS_DEFAULTS.answer(invocation);
        });
        Object converter = Mappers.getMapper((Class) converterType);
        EgonColaMybatisPlusProperties properties = scopedProperties();
        EgonColaUserIdProvider userIdProvider = Mockito.mock(EgonColaUserIdProvider.class);
        Class<?> repositoryType = Class.forName(repositoryName);
        Object repository;
        if (encrypted) {
            Class<?> cipherType = Class.forName(
                    "top.egon.cola.platform.tianquan.shoubing.admin.token.service.impl.OAuthPayloadCipherComponent");
            Object cipher = Mockito.mock(cipherType, Answers.RETURNS_DEFAULTS);
            repository = repositoryType.getConstructor(mapperType, EgonColaMybatisPlusProperties.class,
                    converterType, cipherType, ObjectMapper.class, EgonColaUserIdProvider.class, Clock.class)
                    .newInstance(mapper, properties, converter, cipher,
                            new ObjectMapper().findAndRegisterModules(), userIdProvider, Clock.systemUTC());
        } else {
            repository = repositoryType.getConstructor(mapperType, EgonColaMybatisPlusProperties.class,
                    converterType, EgonColaUserIdProvider.class, Clock.class)
                    .newInstance(mapper, properties, converter, userIdProvider, Clock.systemUTC());
        }
        var operation = repositoryType.getMethod(methodName, methodTypes);
        String previousTenant = MDC.get(EgonColaTenantIdProvider.DEFAULT_MDC_KEY);
        String previousUser = MDC.get("userId");
        MDC.put(EgonColaTenantIdProvider.DEFAULT_MDC_KEY, "7001");
        MDC.put("userId", "operator-1");
        try {
            operation.invoke(repository, methodArguments);
            assertEquals(TENANT_PARTITION_ID + "|operator-1", observed.get(0));
            assertEquals("7001", MDC.get(EgonColaTenantIdProvider.DEFAULT_MDC_KEY));
            assertEquals("operator-1", MDC.get("userId"));

            MDC.put(EgonColaTenantIdProvider.DEFAULT_MDC_KEY, "7002");
            MDC.remove("userId");
            operation.invoke(repository, methodArguments);
            assertEquals(TENANT_PARTITION_ID + "|service:tianquan-shoubing", observed.get(1));
            org.junit.jupiter.api.Assertions.assertNull(MDC.get("userId"));
            assertEquals("7002", MDC.get(EgonColaTenantIdProvider.DEFAULT_MDC_KEY));

            MDC.put(EgonColaTenantIdProvider.DEFAULT_MDC_KEY, "7003");
            MDC.put("userId", "operator-3");
            fail.set(true);
            InvocationTargetException failure = assertThrows(InvocationTargetException.class,
                    () -> operation.invoke(repository, methodArguments));
            assertInstanceOf(IllegalStateException.class, failure.getCause());
            assertEquals(TENANT_PARTITION_ID + "|operator-3", observed.get(2));
            assertEquals("7003", MDC.get(EgonColaTenantIdProvider.DEFAULT_MDC_KEY));
            assertEquals("operator-3", MDC.get("userId"));
        } finally {
            restoreMdc(EgonColaTenantIdProvider.DEFAULT_MDC_KEY, previousTenant);
            restoreMdc("userId", previousUser);
        }
    }

    private static String generatedSpringBeanName(Class<?> type) {
        return new org.springframework.context.annotation.AnnotationBeanNameGenerator()
                .generateBeanName(new AnnotatedGenericBeanDefinition(type), new DefaultListableBeanFactory());
    }

    private static boolean classExists(String name) {
        try {
            Class.forName(name);
            return true;
        } catch (ClassNotFoundException missing) {
            return false;
        }
    }

    private static void restoreMdc(String key, String value) {
        if (value == null) {
            MDC.remove(key);
        } else {
            MDC.put(key, value);
        }
    }

    private static String replaceFirstCipherByte(String encoded) {
        byte[] bytes = java.util.Base64.getUrlDecoder().decode(encoded);
        bytes[0] ^= 1;
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static TrustedRealmBO trustedTenant() {
        return TrustedRealmBO.builder()
                .issuer(URI.create("https://auth.example/tenants/1001"))
                .realmKey("tenant:1001")
                .realmType(RealmTypeEnum.TENANT)
                .storagePartitionId(TENANT_PARTITION_ID)
                .businessTenantId(TENANT_PARTITION_ID)
                .trustVersion(0L)
                .build();
    }

    private static TrustedRealmBO trustedPlatform() {
        return TrustedRealmBO.builder()
                .issuer(URI.create("https://auth.example/platform"))
                .realmKey("platform")
                .realmType(RealmTypeEnum.PLATFORM)
                .storagePartitionId(9_001L)
                .businessTenantId(null)
                .trustVersion(0L)
                .build();
    }

    private static EgonColaMybatisPlusProperties scopedProperties() {
        EgonColaMybatisPlusProperties properties = new EgonColaMybatisPlusProperties();
        properties.getTenantId().setMdcKey(EgonColaTenantIdProvider.DEFAULT_MDC_KEY);
        properties.getAudit().setUserIdMdcKey("userId");
        return properties;
    }

    private static OAuthAuthorizationPO authorizationRow(long id, long version) {
        OAuthAuthorizationPO po = new OAuthAuthorizationPO();
        po.setId(id);
        po.setTenantId(TENANT_PARTITION_ID);
        po.setVersion(version);
        po.setClientPk(901L);
        po.setPrincipalName("legacy-user-sub");
        po.setPrincipalType("USER");
        po.setGrantType("authorization_code");
        po.setAuthorizedScopes(List.of("openid"));
        po.setAudiences(List.of("https://api.example/orders"));
        po.setStatus("ACTIVE");
        po.setAuthTime(Instant.parse("2026-10-08T00:00:00Z"));
        po.setAcr("PASSWORD");
        po.setAttributesCiphertext("ENC:v1:test:nonce:ciphertext");
        po.setAbsoluteExpiresAt(Instant.parse("2026-10-08T00:10:00Z"));
        return po;
    }

    private static OAuthAuthorizationBO authorization(OAuthAuthorizationPO po, long version) {
        OAuthSecuritySnapshotBO snapshot = OAuthSecuritySnapshotBO.builder()
                .realmGeneration(1L).clientGeneration(2L).subjectGeneration(3L)
                .readyEpoch("ready-1").revision(4L).build();
        OAuthAuthorizationAttributesBO attributes = OAuthAuthorizationAttributesBO.builder()
                .schemaVersion(1).nonce("synthetic-authorization-nonce").securitySnapshot(snapshot).build();
        return OAuthAuthorizationBO.builder()
                .id(po.getId()).version(version).clientPk(po.getClientPk())
                .principalName(po.getPrincipalName()).principalType(po.getPrincipalType())
                .grantType(po.getGrantType()).authorizedScopes(po.getAuthorizedScopes())
                .audiences(po.getAudiences()).status(po.getStatus()).authTime(po.getAuthTime())
                .acr(po.getAcr()).attributes(attributes).absoluteExpiresAt(po.getAbsoluteExpiresAt())
                .build();
    }

    private static OAuthTokenPO tokenRow(long id, long version) {
        OAuthTokenPO po = new OAuthTokenPO();
        po.setId(id);
        po.setTenantId(TENANT_PARTITION_ID);
        po.setVersion(version);
        po.setAuthorizationPk(111L);
        po.setTokenType(OAuthTokenTypeEnum.ACCESS);
        po.setTokenDigest("a".repeat(64));
        po.setTokenCiphertext("ENC:v1:test:nonce:ciphertext");
        po.setJti("123456");
        po.setIssuedAt(Instant.parse("2026-10-08T00:00:00Z"));
        po.setExpiresAt(Instant.parse("2026-10-08T00:05:00Z"));
        po.setMetadataCiphertext("ENC:v1:test:nonce:ciphertext");
        return po;
    }

    private static OAuthTokenBO token(OAuthTokenPO po, long version) {
        return OAuthTokenBO.builder()
                .id(po.getId()).version(version).authorizationPk(po.getAuthorizationPk())
                .tokenType(po.getTokenType()).tokenDigest(po.getTokenDigest()).tokenValue("synthetic-token")
                .jti(po.getJti()).issuedAt(po.getIssuedAt()).expiresAt(po.getExpiresAt())
                .metadata(OAuthTokenMetadataBO.builder().schemaVersion(1).invalidated(false)
                        .codeConsumed(false).build())
                .build();
    }

    private static OAuthConsentPO consentRow(long id, long version) {
        OAuthConsentPO po = new OAuthConsentPO();
        po.setId(id);
        po.setTenantId(TENANT_PARTITION_ID);
        po.setVersion(version);
        po.setClientPk(901L);
        po.setSubject("legacy-user-sub");
        po.setAuthorities(List.of("openid", "profile"));
        po.setGrantedAt(Instant.parse("2026-10-08T00:00:00Z"));
        po.setStatus("ACTIVE");
        return po;
    }

    private static OAuthConsentBO consent(OAuthConsentPO po, long version) {
        return OAuthConsentBO.builder()
                .id(po.getId()).version(version).clientPk(po.getClientPk()).subject(po.getSubject())
                .authorities(po.getAuthorities()).grantedAt(po.getGrantedAt()).status(po.getStatus())
                .build();
    }

    private static Path repositoryRoot() {
        Path cursor = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        while (cursor != null) {
            if (Files.isRegularFile(cursor.resolve("pom.xml"))
                    && Files.isDirectory(cursor.resolve("egon-cola-xingyuan"))) {
                return cursor;
            }
            cursor = cursor.getParent();
        }
        throw new IllegalStateException("Repository root was not found");
    }
}
