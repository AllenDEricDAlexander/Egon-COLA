package top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import jakarta.validation.Validation;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
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
import top.egon.cola.platform.tianquan.shoubing.admin.audit.domain.bo.OAuthAuditBO;
import top.egon.cola.platform.tianquan.shoubing.admin.audit.domain.po.OAuthAuditPO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.TrustedRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.bo.OAuthRevocationBO;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.bo.OAuthSecurityStateBO;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.bo.OAuthSigningKeyBO;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.enums.OAuthKeyStatusEnum;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.enums.OAuthSecurityTargetEnum;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.po.OAuthRevocationPO;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.po.OAuthSecurityStatePO;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.po.OAuthSigningKeyPO;
import top.egon.cola.platform.tianquan.shoubing.admin.token.service.impl.OAuthKeyCipherComponent;
import top.egon.cola.platform.tianquan.shoubing.admin.token.service.impl.OAuthPayloadCipherComponent;
import top.egon.cola.platform.tianquan.shoubing.admin.token.config.OAuthKeyEncryptionProperties;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.enums.RealmTypeEnum;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class OAuthSigningKeyPersistenceContractTest {

    private static final String ADMIN_PREFIX =
            "egon-cola-xingyuan/egon-cola-tianquan-shoubing/"
                    + "egon-cola-tianquan-shoubing-admin/src/main/";
    private static final long TENANT_PARTITION_ID = 1_001L;

    @TempDir
    Path temporaryDirectory;

    @Test
    void signingSecurityRevocationAndAuditTablesMatchTheAcceptedDdl() throws IOException {
        String ddl = Files.readString(repositoryRoot().resolve(
                ADMIN_PREFIX + "resources/db/egon-mp/V20261008_001__initialize_oauth_tenant_schema.sql"));
        for (String table : List.of(
                "oauth_signing_key", "oauth_security_state", "oauth_revocation", "oauth_audit")) {
            assertTrue(ddl.contains("CREATE TABLE " + table), "Missing table " + table);
        }
        for (String column : List.of(
                "kid", "algorithm", "public_jwk", "private_key_ciphertext", "status", "verify_until",
                "target_type", "target_key", "generation", "enabled", "revision", "projection_status",
                "event_id", "event_type", "subject_ref", "client_ref", "token_ref", "resource_ref",
                "reason_code", "occurred_at", "audit_kind", "public_id", "actor_sub", "target_sub",
                "identity_tenant_id")) {
            assertTrue(ddl.contains(column), "Missing OAuth security column " + column);
        }
        assertTrue(ddl.contains("uk_key_one_active"));
        assertTrue(ddl.contains("uk_oauth_audit_event_permanent"));
        assertTrue(ddl.contains("jsonb_typeof(payload) = 'object'"));
        assertTrue(ddl.contains("octet_length(payload::text) <= 16384"));
        assertTrue(ddl.contains("CREATE INDEX ix_audit_identity_page"));
        assertTrue(ddl.contains("CREATE INDEX ix_audit_identity_actor"));
    }

    @Test
    void persistenceModelsUseTypedJsonbEnumsAndMaskPrivateMaterial() throws Exception {
        assertEquals("oauth_signing_key", OAuthSigningKeyPO.class.getAnnotation(TableName.class).value());
        assertTrue(OAuthSigningKeyPO.class.getAnnotation(TableName.class).autoResultMap());
        assertEquals("oauth_security_state", OAuthSecurityStatePO.class.getAnnotation(TableName.class).value());
        assertEquals("oauth_revocation", OAuthRevocationPO.class.getAnnotation(TableName.class).value());
        assertTrue(OAuthAuditPO.class.getAnnotation(TableName.class).autoResultMap());
        assertEquals(JacksonTypeHandler.class,
                OAuthSigningKeyPO.class.getDeclaredField("publicJwk").getAnnotation(TableField.class).typeHandler());
        assertEquals(JacksonTypeHandler.class,
                OAuthAuditPO.class.getDeclaredField("payload").getAnnotation(TableField.class).typeHandler());
        assertTrue(OAuthKeyStatusEnum.class.getDeclaredField("code").isAnnotationPresent(EnumValue.class));
        assertTrue(OAuthSecurityTargetEnum.class.getDeclaredField("code").isAnnotationPresent(EnumValue.class));
        assertEquals("COMPROMISED", OAuthKeyStatusEnum.COMPROMISED.getCode());
        assertEquals("SUBJECT", OAuthSecurityTargetEnum.SUBJECT.getCode());
        assertEquals("KID", OAuthSecurityTargetEnum.KID.getCode());

        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        String ciphertext = "ENC:v1:test-kek-v1:synthetic-nonce:synthetic-ciphertext";
        OAuthSigningKeyPO key = new OAuthSigningKeyPO()
                .setKid("tenant-1001-k1")
                .setPrivateKeyCiphertext(ciphertext)
                .setPublicJwk(JsonNodeFactory.instance.objectNode().put("kty", "RSA"));
        assertFalse(key.toString().contains(ciphertext));
        assertFalse(mapper.writeValueAsString(key).contains(ciphertext));
        OAuthSecurityStatePO state = new OAuthSecurityStatePO().setTargetKey("legacy-user-sub");
        assertFalse(state.toString().contains("legacy-user-sub"));
        assertFalse(mapper.writeValueAsString(state).contains("legacy-user-sub"));
        OAuthRevocationPO revocation = new OAuthRevocationPO().setTargetKey("123456789");
        assertFalse(revocation.toString().contains("123456789"));
        assertFalse(mapper.writeValueAsString(revocation).contains("123456789"));
        OAuthAuditPO audit = new OAuthAuditPO()
                .setEventId("event-1")
                .setEventType("KEY_CHANGED")
                .setResult("SUCCESS")
                .setReasonCode("KEY_PUBLISHED")
                .setOccurredAt(Instant.parse("2026-10-08T00:00:00Z"))
                .setAuditKind("OAUTH")
                .setPublicId("public-event-1")
                .setTokenRef("a".repeat(64))
                .setIdentityTenantId("tenant-private-ref")
                .setPayload(JsonNodeFactory.instance.objectNode().put("schemaVersion", 1));
        assertFalse(audit.toString().contains("a".repeat(64)));
        assertFalse(mapper.writeValueAsString(audit).contains("a".repeat(64)));
        assertFalse(mapper.writeValueAsString(audit).contains("schemaVersion"));
        assertFalse(mapper.writeValueAsString(audit).contains("tenant-private-ref"));
    }

    @Test
    void keyCipherUsesSharedPayloadEnvelopeAndBindsTrustedIssuerRealmAndKid() throws Exception {
        Path keyFile = temporaryDirectory.resolve("test-kek.bin");
        Files.write(keyFile, new byte[32]);
        Files.setPosixFilePermissions(keyFile, PosixFilePermissions.fromString("rw-------"));
        OAuthKeyEncryptionProperties properties = new OAuthKeyEncryptionProperties()
                .setKeyId("test-kek-v1")
                .setKeyFile(keyFile);
        OAuthPayloadCipherComponent payloadCipher = new OAuthPayloadCipherComponent(properties);
        payloadCipher.initializeKey();
        OAuthKeyCipherComponent keyCipher = new OAuthKeyCipherComponent(payloadCipher);
        TrustedRealmBO realm = trustedTenant();
        String privateKey = "synthetic-pkcs8-private-key";
        String ciphertext = keyCipher.encryptPrivateKey(realm, "tenant-1001-k1", privateKey);
        assertTrue(ciphertext.startsWith("ENC:v1:test-kek-v1:"));
        assertEquals(privateKey, keyCipher.decryptPrivateKey(realm, "tenant-1001-k1", ciphertext));
        assertThrows(IllegalArgumentException.class,
                () -> keyCipher.decryptPrivateKey(realm, "tenant-1001-k2", ciphertext));
        TrustedRealmBO otherIssuer = trustedTenant().setIssuer(URI.create("https://auth.example/tenant/1001/other"));
        assertThrows(IllegalArgumentException.class,
                () -> keyCipher.decryptPrivateKey(otherIssuer, "tenant-1001-k1", ciphertext));
        assertFalse(keyCipher.toString().contains(privateKey));
    }

    @Test
    void mappersConvertersAndRepositoriesArePresentWithGeneratedBeanNames() throws Exception {
        List<String> types = List.of(
                "top.egon.cola.platform.tianquan.shoubing.admin.token.dao.OAuthSigningKeyMapper",
                "top.egon.cola.platform.tianquan.shoubing.admin.token.dao.OAuthSecurityStateMapper",
                "top.egon.cola.platform.tianquan.shoubing.admin.token.dao.OAuthRevocationMapper",
                "top.egon.cola.platform.tianquan.shoubing.admin.audit.dao.OAuthAuditMapper",
                "top.egon.cola.platform.tianquan.shoubing.admin.token.domain.converter.OAuthSigningKeyConverterImpl",
                "top.egon.cola.platform.tianquan.shoubing.admin.token.domain.converter.OAuthSecurityStateConverterImpl",
                "top.egon.cola.platform.tianquan.shoubing.admin.token.domain.converter.OAuthRevocationConverterImpl",
                "top.egon.cola.platform.tianquan.shoubing.admin.audit.domain.converter.OAuthAuditConverterImpl",
                "top.egon.cola.platform.tianquan.shoubing.admin.token.repository.impl.OAuthSigningKeyRepositoryImpl",
                "top.egon.cola.platform.tianquan.shoubing.admin.token.repository.impl.OAuthSecurityStateRepositoryImpl",
                "top.egon.cola.platform.tianquan.shoubing.admin.token.repository.impl.OAuthRevocationRepositoryImpl",
                "top.egon.cola.platform.tianquan.shoubing.admin.audit.repository.impl.OAuthAuditRepositoryImpl");
        for (String type : types) {
            assertTrue(classExists(type), "Missing Step 7 persistence type " + type);
        }
        assertConverterDirection(
                "top.egon.cola.platform.tianquan.shoubing.admin.token.domain.converter.OAuthSigningKeyConverter",
                "top.egon.cola.platform.tianquan.shoubing.admin.token.domain.po.OAuthSigningKeyPO",
                "top.egon.cola.platform.tianquan.shoubing.admin.token.domain.bo.OAuthSigningKeyBO");
        assertConverterDirection(
                "top.egon.cola.platform.tianquan.shoubing.admin.token.domain.converter.OAuthSecurityStateConverter",
                "top.egon.cola.platform.tianquan.shoubing.admin.token.domain.po.OAuthSecurityStatePO",
                "top.egon.cola.platform.tianquan.shoubing.admin.token.domain.bo.OAuthSecurityStateBO");
        assertConverterDirection(
                "top.egon.cola.platform.tianquan.shoubing.admin.token.domain.converter.OAuthRevocationConverter",
                "top.egon.cola.platform.tianquan.shoubing.admin.token.domain.po.OAuthRevocationPO",
                "top.egon.cola.platform.tianquan.shoubing.admin.token.domain.bo.OAuthRevocationBO");
        assertConverterDirection(
                "top.egon.cola.platform.tianquan.shoubing.admin.audit.domain.converter.OAuthAuditConverter",
                "top.egon.cola.platform.tianquan.shoubing.admin.audit.domain.po.OAuthAuditPO",
                "top.egon.cola.platform.tianquan.shoubing.admin.audit.domain.bo.OAuthAuditBO");
        assertEquals("OAuthSigningKeyMapper", generatedSpringBeanName(Class.forName(types.get(0))));
        assertEquals("OAuthSecurityStateMapper", generatedSpringBeanName(Class.forName(types.get(1))));
        assertEquals("OAuthRevocationMapper", generatedSpringBeanName(Class.forName(types.get(2))));
        assertEquals("OAuthAuditMapper", generatedSpringBeanName(Class.forName(types.get(3))));
        assertEquals("OAuthSigningKeyConverterImpl", generatedSpringBeanName(Class.forName(types.get(4))));
        assertEquals("OAuthSecurityStateConverterImpl", generatedSpringBeanName(Class.forName(types.get(5))));
        assertEquals("OAuthRevocationConverterImpl", generatedSpringBeanName(Class.forName(types.get(6))));
        assertEquals("OAuthAuditConverterImpl", generatedSpringBeanName(Class.forName(types.get(7))));
    }

    @Test
    void mapperXmlParsesWithTenantActiveExpectedVersionAndCommandLocks() throws IOException {
        assertMapperContract(
                "mapper/token/OAuthSigningKeyMapper.xml",
                "oauth_signing_key",
                "top.egon.cola.platform.tianquan.shoubing.admin.token.dao.OAuthSigningKeyMapper",
                List.of("selectByKid", "selectByKidForUpdate", "selectByStatuses", "selectActiveByIds"));
        assertMapperContract(
                "mapper/token/OAuthSecurityStateMapper.xml",
                "oauth_security_state",
                "top.egon.cola.platform.tianquan.shoubing.admin.token.dao.OAuthSecurityStateMapper",
                List.of("selectByTarget", "selectByTargetForUpdate", "selectByRevisionAfter", "selectActiveByIds"));
        assertMapperContract(
                "mapper/token/OAuthRevocationMapper.xml",
                "oauth_revocation",
                "top.egon.cola.platform.tianquan.shoubing.admin.token.dao.OAuthRevocationMapper",
                List.of("selectByTarget", "selectByTargetForUpdate", "selectByProjectionStatus", "selectExpired"));
        assertMapperContract(
                "mapper/audit/OAuthAuditMapper.xml",
                "oauth_audit",
                "top.egon.cola.platform.tianquan.shoubing.admin.audit.dao.OAuthAuditMapper",
                List.of("selectByEventId", "selectByEventIdForUpdate", "selectByOccurredAt",
                        "selectIdentityPage", "countIdentityPage", "selectExpired"), false);
    }

    @Test
    void auditIdentityQueryKeepsCompatibilityFiltersAndDoesNotApplyOAuthWindow() throws IOException {
        String resource = "mapper/audit/OAuthAuditMapper.xml";
        Path path = repositoryRoot().resolve(ADMIN_PREFIX + "resources/" + resource);
        assertTrue(Files.isRegularFile(path), "Missing mapper XML " + resource);
        String xml = Files.readString(path).toLowerCase();
        assertTrue(xml.contains("audit_kind = 'identity'"));
        assertTrue(xml.contains("actor_sub = #{actorsub}"));
        assertTrue(xml.contains("event_type = #{eventtype}"));
        assertTrue(xml.contains("result = #{result}"));
        assertTrue(xml.contains("trace_id = #{traceid}"));
        assertTrue(xml.contains("occurred_at &gt;= #{from}"));
        assertTrue(xml.contains("occurred_at &lt; #{to}"));
        assertTrue(xml.contains("order by occurred_at desc, public_id desc"));
        assertFalse(xml.contains("interval '31 days'"));
        assertFalse(xml.contains("${"));
    }

    @Test
    void poValidationGroupsCoverUpdateAndDeleteOperations() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            Set<String> keyUpdatePaths = validator.validate(
                            new OAuthSigningKeyPO().setAlgorithm(null), EgonColaModelValidationGroups.Update.class)
                    .stream().map(violation -> violation.getPropertyPath().toString()).collect(Collectors.toSet());
            assertTrue(keyUpdatePaths.containsAll(Set.of(
                    "kid", "algorithm", "publicJwk", "privateKeyCiphertext", "status", "publishedAt")));
            Set<String> stateUpdatePaths = validator.validate(
                            new OAuthSecurityStatePO(), EgonColaModelValidationGroups.Update.class).stream()
                    .map(violation -> violation.getPropertyPath().toString()).collect(Collectors.toSet());
            assertTrue(stateUpdatePaths.containsAll(Set.of("targetType", "targetKey", "generation", "enabled",
                    "revision", "reasonCode")));
            Set<String> revocationUpdatePaths = validator.validate(
                            new OAuthRevocationPO(), EgonColaModelValidationGroups.Update.class).stream()
                    .map(violation -> violation.getPropertyPath().toString()).collect(Collectors.toSet());
            assertTrue(revocationUpdatePaths.containsAll(Set.of("targetType", "targetKey", "reasonCode",
                    "revokedAt", "revision", "projectionStatus")));
            Set<String> auditUpdatePaths = validator.validate(
                            new OAuthAuditPO().setPayload(null), EgonColaModelValidationGroups.Update.class).stream()
                    .map(violation -> violation.getPropertyPath().toString()).collect(Collectors.toSet());
            assertTrue(auditUpdatePaths.containsAll(Set.of("eventId", "eventType", "result", "reasonCode",
                    "occurredAt", "payload", "auditKind", "publicId")));
            assertTrue(validator.validate(new OAuthSigningKeyPO(), EgonColaModelValidationGroups.Delete.class).stream()
                    .map(violation -> violation.getPropertyPath().toString()).collect(Collectors.toSet())
                    .containsAll(Set.of("id", "version")));
        }
    }

    @Test
    void scopedRepositoriesRestoreTenantAndActorMdcAfterSuccessAndFailure() throws Exception {
        assertScopedRepositoryMdc(
                "top.egon.cola.platform.tianquan.shoubing.admin.token.repository.impl.OAuthSigningKeyRepositoryImpl",
                "top.egon.cola.platform.tianquan.shoubing.admin.token.dao.OAuthSigningKeyMapper",
                "top.egon.cola.platform.tianquan.shoubing.admin.token.domain.converter.OAuthSigningKeyConverter",
                "findByKid", new Class<?>[]{TrustedRealmBO.class, String.class},
                new Object[]{trustedTenant(), "tenant-1001-k1"}, "selectByKid");
        assertScopedRepositoryMdc(
                "top.egon.cola.platform.tianquan.shoubing.admin.token.repository.impl.OAuthSecurityStateRepositoryImpl",
                "top.egon.cola.platform.tianquan.shoubing.admin.token.dao.OAuthSecurityStateMapper",
                "top.egon.cola.platform.tianquan.shoubing.admin.token.domain.converter.OAuthSecurityStateConverter",
                "findByTarget", new Class<?>[]{TrustedRealmBO.class, OAuthSecurityTargetEnum.class, String.class},
                new Object[]{trustedTenant(), OAuthSecurityTargetEnum.SUBJECT, "legacy-user-sub"}, "selectByTarget");
        assertScopedRepositoryMdc(
                "top.egon.cola.platform.tianquan.shoubing.admin.token.repository.impl.OAuthRevocationRepositoryImpl",
                "top.egon.cola.platform.tianquan.shoubing.admin.token.dao.OAuthRevocationMapper",
                "top.egon.cola.platform.tianquan.shoubing.admin.token.domain.converter.OAuthRevocationConverter",
                "findByTarget", new Class<?>[]{TrustedRealmBO.class, OAuthSecurityTargetEnum.class, String.class},
                new Object[]{trustedTenant(), OAuthSecurityTargetEnum.JTI, "123456"}, "selectByTarget");
        assertScopedRepositoryMdc(
                "top.egon.cola.platform.tianquan.shoubing.admin.audit.repository.impl.OAuthAuditRepositoryImpl",
                "top.egon.cola.platform.tianquan.shoubing.admin.audit.dao.OAuthAuditMapper",
                "top.egon.cola.platform.tianquan.shoubing.admin.audit.domain.converter.OAuthAuditConverter",
                "findByEventId", new Class<?>[]{TrustedRealmBO.class, String.class},
                new Object[]{trustedTenant(), "event-1"}, "selectByEventId");
    }

    @Test
    void identicalKidIsQueriedWithinEachTrustedTenantPartition() throws Exception {
        String repositoryName =
                "top.egon.cola.platform.tianquan.shoubing.admin.token.repository.impl.OAuthSigningKeyRepositoryImpl";
        assertTrue(classExists(repositoryName), "Missing OAuth signing-key repository");
        Class<?> repositoryType = Class.forName(repositoryName);
        Class<?> mapperType = Class.forName(
                "top.egon.cola.platform.tianquan.shoubing.admin.token.dao.OAuthSigningKeyMapper");
        Class<?> converterType = Class.forName(
                "top.egon.cola.platform.tianquan.shoubing.admin.token.domain.converter.OAuthSigningKeyConverter");
        List<Long> queriedTenants = new ArrayList<>();
        Object mapper = Mockito.mock(mapperType, invocation -> {
            if (invocation.getMethod().getName().equals("selectByKid")) {
                queriedTenants.add((Long) invocation.getArgument(0));
            }
            return Answers.RETURNS_DEFAULTS.answer(invocation);
        });
        Object converter = mock(converterType);
        Object repository = repositoryType.getConstructor(mapperType, EgonColaMybatisPlusProperties.class,
                        converterType, EgonColaUserIdProvider.class, Clock.class)
                .newInstance(mapper, scopedProperties(), converter,
                        mock(EgonColaUserIdProvider.class), Clock.systemUTC());
        var findByKid = repositoryType.getMethod("findByKid", TrustedRealmBO.class, String.class);
        findByKid.invoke(repository, trustedTenant(), "shared-kid");
        findByKid.invoke(repository, trustedTenant(1_002L), "shared-kid");
        assertEquals(List.of(TENANT_PARTITION_ID, 1_002L), queriedTenants);
    }

    @Test
    void staleExpectedVersionCannotUpdateSecurityGeneration() throws Exception {
        String repositoryName =
                "top.egon.cola.platform.tianquan.shoubing.admin.token.repository.impl.OAuthSecurityStateRepositoryImpl";
        assertTrue(classExists(repositoryName), "Missing OAuth security-state repository");
        Class<?> repositoryType = Class.forName(repositoryName);
        Class<?> mapperType = Class.forName(
                "top.egon.cola.platform.tianquan.shoubing.admin.token.dao.OAuthSecurityStateMapper");
        Class<?> converterType = Class.forName(
                "top.egon.cola.platform.tianquan.shoubing.admin.token.domain.converter.OAuthSecurityStateConverter");
        OAuthSecurityStatePO current = new OAuthSecurityStatePO();
        current.setId(511L);
        current.setTenantId(TENANT_PARTITION_ID);
        current.setVersion(3L);
        current.setTargetType(OAuthSecurityTargetEnum.SUBJECT);
        current.setTargetKey("legacy-user-sub");
        current.setGeneration(4L);
        current.setEnabled(true);
        current.setRevision(6L);
        current.setReasonCode("ADMIN_REVOKE");
        AtomicBoolean updateAttempted = new AtomicBoolean();
        Object mapper = Mockito.mock(mapperType, invocation -> {
            if (invocation.getMethod().getName().equals("selectByTargetForUpdate")) {
                return current;
            }
            if (invocation.getMethod().getName().equals("updateExpectedVersion")) {
                updateAttempted.set(true);
            }
            return Answers.RETURNS_DEFAULTS.answer(invocation);
        });
        Object converter = mock(converterType);
        Object repository = repositoryType.getConstructor(mapperType, EgonColaMybatisPlusProperties.class,
                        converterType, EgonColaUserIdProvider.class, Clock.class)
                .newInstance(mapper, scopedProperties(), converter,
                        mock(EgonColaUserIdProvider.class), Clock.systemUTC());
        OAuthSecurityStateBO stale = OAuthSecurityStateBO.builder()
                .id(511L)
                .version(2L)
                .targetType(OAuthSecurityTargetEnum.SUBJECT)
                .targetKey("legacy-user-sub")
                .generation(4L)
                .enabled(true)
                .revision(6L)
                .reasonCode("ADMIN_REVOKE")
                .build();
        var save = repositoryType.getMethod(
                "save", TrustedRealmBO.class, OAuthSecurityStateBO.class, long.class);
        InvocationTargetException failure = assertThrows(InvocationTargetException.class,
                () -> save.invoke(repository, trustedTenant(), stale, 1L));
        assertInstanceOf(org.springframework.dao.OptimisticLockingFailureException.class, failure.getCause());
        assertFalse(updateAttempted.get());
    }

    @Test
    void signingKeyRepositoryRejectsPrivateJwkMembersBeforeMapperAccess() throws Exception {
        String repositoryName =
                "top.egon.cola.platform.tianquan.shoubing.admin.token.repository.impl.OAuthSigningKeyRepositoryImpl";
        assertTrue(classExists(repositoryName), "Missing OAuth signing-key repository");
        Class<?> repositoryType = Class.forName(repositoryName);
        Class<?> mapperType = Class.forName(
                "top.egon.cola.platform.tianquan.shoubing.admin.token.dao.OAuthSigningKeyMapper");
        Class<?> converterType = Class.forName(
                "top.egon.cola.platform.tianquan.shoubing.admin.token.domain.converter.OAuthSigningKeyConverter");
        Object mapper = Mockito.mock(mapperType, Answers.RETURNS_DEFAULTS);
        Object converter = mock(converterType);
        Object repository = repositoryType.getConstructor(mapperType, EgonColaMybatisPlusProperties.class,
                        converterType, EgonColaUserIdProvider.class, Clock.class)
                .newInstance(mapper, scopedProperties(), converter,
                        mock(EgonColaUserIdProvider.class), Clock.systemUTC());
        var privateJwk = JsonNodeFactory.instance.objectNode()
                .put("kty", "RSA")
                .put("kid", "tenant-1001-k1")
                .put("alg", "RS256")
                .put("use", "sig")
                .put("n", "synthetic-modulus")
                .put("e", "AQAB")
                .put("d", "synthetic-private-member");
        OAuthSigningKeyBO invalid = OAuthSigningKeyBO.builder()
                .kid("tenant-1001-k1")
                .algorithm("RS256")
                .publicJwk(privateJwk)
                .privateKeyCiphertext("ENC:v1:test-kek-v1:AAAAAAAAAAAAAAAA:AAAAAAAAAAAAAAAAAAAAAA")
                .status(OAuthKeyStatusEnum.PUBLISHED)
                .publishedAt(Instant.parse("2026-10-08T00:00:00Z"))
                .build();
        var save = repositoryType.getMethod("save", TrustedRealmBO.class, OAuthSigningKeyBO.class, long.class);
        InvocationTargetException failure = assertThrows(InvocationTargetException.class,
                () -> save.invoke(repository, trustedTenant(), invalid, 0L));
        assertInstanceOf(IllegalArgumentException.class, failure.getCause());
        Mockito.verifyNoInteractions(mapper);
    }

    @Test
    void auditRepositoryRejectsCredentialPayloadBeforeMapperAccess() throws Exception {
        String repositoryName =
                "top.egon.cola.platform.tianquan.shoubing.admin.audit.repository.impl.OAuthAuditRepositoryImpl";
        assertTrue(classExists(repositoryName), "Missing OAuth audit repository");
        Class<?> repositoryType = Class.forName(repositoryName);
        Class<?> mapperType = Class.forName(
                "top.egon.cola.platform.tianquan.shoubing.admin.audit.dao.OAuthAuditMapper");
        Class<?> converterType = Class.forName(
                "top.egon.cola.platform.tianquan.shoubing.admin.audit.domain.converter.OAuthAuditConverter");
        Object mapper = Mockito.mock(mapperType, Answers.RETURNS_DEFAULTS);
        Object repository = repositoryType.getConstructor(mapperType, EgonColaMybatisPlusProperties.class,
                        converterType)
                .newInstance(mapper, scopedProperties(), mock(converterType));
        OAuthAuditBO audit = OAuthAuditBO.builder()
                .eventId("event-1")
                .eventType("TOKEN_ISSUED")
                .result("SUCCESS")
                .reasonCode("TOKEN_ISSUED")
                .occurredAt(Instant.parse("2026-10-08T00:00:00Z"))
                .auditKind("OAUTH")
                .publicId("public-event-1")
                .payload(JsonNodeFactory.instance.objectNode()
                        .put("schemaVersion", 1)
                        .put("accessToken", "synthetic-sensitive-value"))
                .build();
        var save = repositoryType.getMethod("save", TrustedRealmBO.class, OAuthAuditBO.class, long.class);
        InvocationTargetException failure = assertThrows(InvocationTargetException.class,
                () -> save.invoke(repository, trustedTenant(), audit, 0L));
        assertInstanceOf(IllegalArgumentException.class, failure.getCause());
        assertFalse(failure.getCause().getMessage().contains("synthetic-sensitive-value"));
        Mockito.verifyNoInteractions(mapper);
    }

    private void assertMapperContract(
            String resource,
            String table,
            String namespace,
            List<String> statements) throws IOException {
        assertMapperContract(resource, table, namespace, statements, true);
    }

    private void assertMapperContract(
            String resource,
            String table,
            String namespace,
            List<String> statements,
            boolean requireVersionedUpdate) throws IOException {
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
        if (requireVersionedUpdate) {
            assertTrue(configuration.hasStatement(namespace + ".updateExpectedVersion", false));
        }
        assertTrue(configuration.hasStatement(namespace + ".deleteVersionedById", false));
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
            String mapperMethod) throws Exception {
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
        Class<?> repositoryType = Class.forName(repositoryName);
        Object repository;
        if (repositoryName.endsWith("OAuthAuditRepositoryImpl")) {
            repository = repositoryType.getConstructor(mapperType, EgonColaMybatisPlusProperties.class,
                    converterType).newInstance(mapper, properties, converter);
        } else {
            EgonColaUserIdProvider userIdProvider = Mockito.mock(EgonColaUserIdProvider.class);
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

    private static TrustedRealmBO trustedTenant() {
        return trustedTenant(TENANT_PARTITION_ID);
    }

    private static TrustedRealmBO trustedTenant(long tenantId) {
        return TrustedRealmBO.builder()
                .issuer(URI.create("https://auth.example/tenant/" + tenantId))
                .realmKey("tenant:" + tenantId)
                .realmType(RealmTypeEnum.TENANT)
                .storagePartitionId(tenantId)
                .businessTenantId(tenantId)
                .trustVersion(3L)
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

    private static void restoreMdc(String key, String value) {
        if (value == null) {
            MDC.remove(key);
        } else {
            MDC.put(key, value);
        }
    }
}
