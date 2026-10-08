package top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.JdbcType;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.beans.factory.annotation.AnnotatedGenericBeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.slf4j.MDC;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;
import top.egon.cola.component.common.mybatis.business.EgonColaTenantIdProvider;
import top.egon.cola.component.common.mybatis.business.EgonColaUserIdProvider;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.dao.OAuthStringListJsonbTypeHandler;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.dao.OAuthClientMapper;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.dao.OAuthRealmMapper;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthClientBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.TrustedRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.converter.OAuthClientConverter;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.converter.OAuthRealmConverter;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.enums.OAuthClientPurposeEnum;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.enums.RealmTypeEnum;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.po.OAuthClientPO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.po.OAuthRealmPO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository.impl.OAuthClientRepositoryImpl;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository.impl.OAuthRealmRepositoryImpl;
import top.egon.cola.platform.tianquan.shoubing.core.oauth.OAuthClient;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OAuthRealmPersistenceContractTest {

    private static final String ADMIN_PREFIX =
            "egon-cola-xingyuan/egon-cola-tianquan-shoubing/"
                    + "egon-cola-tianquan-shoubing-admin/src/main/";
    private static final long TENANT_PARTITION_ID = 1_001L;

    @Test
    void realmAndClientSchemaExposeExactTrustedPartitionsAndBusinessColumns() throws IOException {
        String ddl = Files.readString(repositoryRoot().resolve(
                ADMIN_PREFIX + "resources/db/egon-mp/"
                        + "V20261008_001__initialize_oauth_tenant_schema.sql"));

        assertTrue(ddl.contains("CREATE TABLE oauth_realm"));
        assertTrue(ddl.contains("business_tenant_id BIGINT"));
        assertTrue(ddl.contains("storage_partition_id BIGINT NOT NULL"));
        assertTrue(ddl.contains("tenant_id = storage_partition_id"));
        assertTrue(ddl.contains("CREATE TABLE oauth_client"));
        assertTrue(ddl.contains("client_id VARCHAR(128) NOT NULL"));
        assertTrue(ddl.contains("purpose VARCHAR(16) NOT NULL"));
        assertTrue(ddl.contains("redirect_uris JSONB NOT NULL"));
        assertTrue(ddl.contains("resource_uris JSONB NOT NULL"));
        assertTrue(ddl.contains("allowed_scopes JSONB NOT NULL"));
    }

    @Test
    void realmAndClientMappersScopeEveryReadAndVersionedWrite() throws IOException {
        assertMapperContract(
                "mapper/oauth/OAuthRealmMapper.xml",
                "oauth_realm",
                "top.egon.cola.platform.tianquan.shoubing.admin.oauth.dao.OAuthRealmMapper",
                List.of("selectByIssuer", "selectByRealmKey", "selectByRealmKeyForUpdate", "selectDirectory")
        );
        assertMapperContract(
                "mapper/oauth/OAuthClientMapper.xml",
                "oauth_client",
                "top.egon.cola.platform.tianquan.shoubing.admin.oauth.dao.OAuthClientMapper",
                List.of("selectByClientId", "selectByClientIdForUpdate", "selectDirectory")
        );
    }

    @Test
    void namedMappersConvertersAndRepositoryImplementationsArePresent() {
        List<String> types = List.of(
                "top.egon.cola.platform.tianquan.shoubing.admin.oauth.dao.OAuthRealmMapper",
                "top.egon.cola.platform.tianquan.shoubing.admin.oauth.dao.OAuthClientMapper",
                "top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.converter.OAuthRealmConverterImpl",
                "top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.converter.OAuthClientConverterImpl",
                "top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository.impl.OAuthRealmRepositoryImpl",
                "top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository.impl.OAuthClientRepositoryImpl"
        );
        for (String type : types) {
            assertTrue(classExists(type), "Missing typed persistence implementation: " + type);
        }
    }

    @Test
    void repositoryQualifiersMatchSpringGeneratedMapperAndConverterBeanNames() throws Exception {
        String realmMapperName =
                "top.egon.cola.platform.tianquan.shoubing.admin.oauth.dao.OAuthRealmMapper";
        String realmConverterName =
                "top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.converter.OAuthRealmConverterImpl";
        String clientMapperName =
                "top.egon.cola.platform.tianquan.shoubing.admin.oauth.dao.OAuthClientMapper";
        String clientConverterName =
                "top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.converter.OAuthClientConverterImpl";

        assertEquals("OAuthRealmMapper", generatedSpringBeanName(Class.forName(realmMapperName)));
        assertEquals("OAuthRealmConverterImpl", generatedSpringBeanName(Class.forName(realmConverterName)));
        assertEquals("OAuthClientMapper", generatedSpringBeanName(Class.forName(clientMapperName)));
        assertEquals("OAuthClientConverterImpl", generatedSpringBeanName(Class.forName(clientConverterName)));

        String realmRepository = Files.readString(repositoryRoot().resolve(
                ADMIN_PREFIX + "java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/"
                        + "repository/impl/OAuthRealmRepositoryImpl.java"));
        String clientRepository = Files.readString(repositoryRoot().resolve(
                ADMIN_PREFIX + "java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/"
                        + "repository/impl/OAuthClientRepositoryImpl.java"));
        assertTrue(realmRepository.contains("@Qualifier(\"OAuthRealmMapper\")"));
        assertTrue(realmRepository.contains("@Qualifier(\"OAuthRealmConverterImpl\")"));
        assertTrue(clientRepository.contains("@Qualifier(\"OAuthClientMapper\")"));
        assertTrue(clientRepository.contains("@Qualifier(\"OAuthClientConverterImpl\")"));
    }

    @Test
    void converterGenericDirectionsMatchBaseConverterSourceContract() throws Exception {
        String realmConverterName =
                "top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.converter.OAuthRealmConverter";
        assertConverterDirection(
                realmConverterName,
                "top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.po.OAuthRealmPO",
                "top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthRealmBO"
        );
        Object realmConverter = Mappers.getMapper((Class) Class.forName(realmConverterName));
        OAuthRealmPO realmSource = new OAuthRealmPO();
        realmSource.setId(31L);
        realmSource.setVersion(2L);
        realmSource.setIssuer("https://auth.example/tenants/1001");
        realmSource.setRealmKey("tenant:1001");
        realmSource.setRealmType(RealmTypeEnum.TENANT);
        realmSource.setBusinessTenantId(1_001L);
        realmSource.setStoragePartitionId(1_001L);
        realmSource.setStatus("ACTIVE");
        realmSource.setActiveKid("kid-1");
        realmSource.setTrustVersion(4L);
        OAuthRealmBO realm = (OAuthRealmBO) realmConverter.getClass()
                .getMethod("toTarget", OAuthRealmPO.class)
                .invoke(realmConverter, realmSource);
        assertEquals(URI.create("https://auth.example/tenants/1001"), realm.getIssuer());
        assertEquals(31L, realm.getId());
        assertEquals(4L, realm.getTrustVersion());
        OAuthRealmPO realmCopy = (OAuthRealmPO) realmConverter.getClass()
                .getMethod("toSource", OAuthRealmBO.class)
                .invoke(realmConverter, realm);
        assertEquals(realmSource.getIssuer(), realmCopy.getIssuer());
        assertEquals(realmSource.getRealmKey(), realmCopy.getRealmKey());

        String clientConverterName =
                "top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.converter.OAuthClientConverter";
        assertConverterDirection(
                clientConverterName,
                "top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.po.OAuthClientPO",
                "top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthClientBO"
        );
        Object clientConverter = Mappers.getMapper((Class) Class.forName(clientConverterName));
        OAuthClientPO clientSource = new OAuthClientPO();
        clientSource.setId(41L);
        clientSource.setVersion(7L);
        clientSource.setClientId("client-1");
        clientSource.setAppId("application-1");
        clientSource.setClientName("Client One");
        clientSource.setClientType("CONFIDENTIAL");
        clientSource.setPurpose(OAuthClientPurposeEnum.USER);
        clientSource.setStatus("ACTIVE");
        clientSource.setSecretHash("{bcrypt}encoded-secret");
        clientSource.setSecretHint("aB12");
        clientSource.setCredentialVersion(3L);
        clientSource.setRedirectUris(List.of("https://client.example/callback"));
        clientSource.setResourceUris(List.of("https://api.example"));
        clientSource.setAllowedScopes(List.of("orders:read"));
        clientSource.setAccessTtlSeconds(300);
        clientSource.setRefreshTtlSeconds(86_400);
        OAuthClientBO client = (OAuthClientBO) clientConverter.getClass()
                .getMethod("toTarget", OAuthClientPO.class)
                .invoke(clientConverter, clientSource);
        assertEquals(OAuthClient.ClientType.CONFIDENTIAL, client.getClientType());
        assertEquals(OAuthClient.Status.ACTIVE, client.getStatus());
        assertEquals(List.of("orders:read"), client.getAllowedScopes());
        OAuthClientPO clientCopy = (OAuthClientPO) clientConverter.getClass()
                .getMethod("toSource", OAuthClientBO.class)
                .invoke(clientConverter, client);
        assertEquals(clientSource.getClientId(), clientCopy.getClientId());
        assertEquals(clientSource.getSecretHash(), clientCopy.getSecretHash());
        assertEquals(clientSource.getRedirectUris(), clientCopy.getRedirectUris());
    }

    @Test
    void trustedRealmStorageScopeRestoresTenantAndActorMdcAfterSuccessAndFailure() throws Exception {
        TrustedRealmBO realm = trustedTenant();
        assertTrustedMdcScope(
                "top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository.impl.OAuthRealmRepositoryImpl",
                "top.egon.cola.platform.tianquan.shoubing.admin.oauth.dao.OAuthRealmMapper",
                "top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.converter.OAuthRealmConverter",
                "findByRealmKey",
                new Class<?>[]{TrustedRealmBO.class},
                new Object[]{realm},
                "selectByRealmKey"
        );
        assertTrustedMdcScope(
                "top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository.impl.OAuthClientRepositoryImpl",
                "top.egon.cola.platform.tianquan.shoubing.admin.oauth.dao.OAuthClientMapper",
                "top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.converter.OAuthClientConverter",
                "findByClientId",
                new Class<?>[]{TrustedRealmBO.class, String.class},
                new Object[]{realm, "client-1"},
                "selectByClientId"
        );
    }

    @Test
    void staleExpectedVersionDoesNotUpdateRealmOrClientRows() {
        TrustedRealmBO trustedRealm = trustedTenant();
        EgonColaMybatisPlusProperties properties = new EgonColaMybatisPlusProperties();
        OAuthRealmMapper realmMapper = mock(OAuthRealmMapper.class);
        OAuthRealmConverter realmConverter = Mappers.getMapper(OAuthRealmConverter.class);
        EgonColaUserIdProvider userIdProvider = mock(EgonColaUserIdProvider.class);
        OAuthRealmRepositoryImpl realms = new OAuthRealmRepositoryImpl(
                realmMapper, properties, realmConverter, userIdProvider, Clock.systemUTC());

        OAuthRealmPO storedRealm = new OAuthRealmPO();
        storedRealm.setId(31L);
        storedRealm.setTenantId(TENANT_PARTITION_ID);
        storedRealm.setVersion(3L);
        storedRealm.setIssuer(trustedRealm.getIssuer().toASCIIString());
        storedRealm.setRealmKey(trustedRealm.getRealmKey());
        storedRealm.setRealmType(trustedRealm.getRealmType());
        storedRealm.setBusinessTenantId(trustedRealm.getBusinessTenantId());
        storedRealm.setStoragePartitionId(trustedRealm.getStoragePartitionId());
        storedRealm.setStatus("ACTIVE");
        storedRealm.setTrustVersion(4L);
        when(realmMapper.selectByRealmKeyForUpdate(TENANT_PARTITION_ID, trustedRealm.getRealmKey()))
                .thenReturn(storedRealm);
        OAuthRealmBO staleRealm = realmConverter.toTarget(storedRealm).setVersion(2L);
        assertThrows(OptimisticLockingFailureException.class,
                () -> realms.save(trustedRealm, staleRealm, 1L));
        verify(realmMapper, never()).updateExpectedVersion(anyLong(), any(), anyLong());

        OAuthClientMapper clientMapper = mock(OAuthClientMapper.class);
        OAuthClientConverter clientConverter = Mappers.getMapper(OAuthClientConverter.class);
        OAuthClientRepositoryImpl clients = new OAuthClientRepositoryImpl(
                clientMapper, properties, clientConverter, userIdProvider, Clock.systemUTC());
        OAuthClientPO storedClient = new OAuthClientPO();
        storedClient.setId(41L);
        storedClient.setTenantId(TENANT_PARTITION_ID);
        storedClient.setVersion(3L);
        storedClient.setClientId("client-1");
        storedClient.setAppId("application-1");
        storedClient.setClientName("Client One");
        storedClient.setClientType("CONFIDENTIAL");
        storedClient.setPurpose(OAuthClientPurposeEnum.USER);
        storedClient.setStatus("ACTIVE");
        storedClient.setCredentialVersion(2L);
        storedClient.setRedirectUris(List.of("https://client.example/callback"));
        storedClient.setResourceUris(List.of("https://api.example"));
        storedClient.setAllowedScopes(List.of("orders:read"));
        storedClient.setAccessTtlSeconds(300);
        storedClient.setRefreshTtlSeconds(86_400);
        when(clientMapper.selectByClientIdForUpdate(TENANT_PARTITION_ID, "client-1"))
                .thenReturn(storedClient);
        OAuthClientBO staleClient = clientConverter.toTarget(storedClient).setVersion(2L);
        assertThrows(OptimisticLockingFailureException.class,
                () -> clients.save(trustedRealm, staleClient, 1L));
        verify(clientMapper, never()).updateExpectedVersion(anyLong(), any(), anyLong());
    }

    @Test
    void trustedRealmKeepsBusinessAndStoragePartitionsDistinctByRealmType() {
        TrustedRealmBO tenant = TrustedRealmBO.builder()
                .issuer(URI.create("https://auth.example/tenants/1001"))
                .realmKey("tenant:1001")
                .realmType(RealmTypeEnum.TENANT)
                .storagePartitionId(1_001L)
                .businessTenantId(1_001L)
                .trustVersion(0L)
                .build();
        tenant.validateTrustedPartition();

        TrustedRealmBO platform = TrustedRealmBO.builder()
                .issuer(URI.create("https://auth.example/platform"))
                .realmKey("platform")
                .realmType(RealmTypeEnum.PLATFORM)
                .storagePartitionId(9_001L)
                .businessTenantId(null)
                .trustVersion(0L)
                .build();
        platform.validateTrustedPartition();

        TrustedRealmBO mismatchedTenant = TrustedRealmBO.builder()
                .issuer(URI.create("https://auth.example/tenants/1001"))
                .realmKey("tenant:1001")
                .realmType(RealmTypeEnum.TENANT)
                .storagePartitionId(1_001L)
                .businessTenantId(1_002L)
                .trustVersion(0L)
                .build();
        assertThrows(IllegalArgumentException.class, mismatchedTenant::validateTrustedPartition);
    }

    @Test
    void realmAndClientEnumsKeepStableJsonAndPersistentCodes() throws Exception {
        assertEquals("\"TENANT\"", new ObjectMapper().writeValueAsString(RealmTypeEnum.TENANT));
        assertEquals(RealmTypeEnum.PLATFORM,
                new ObjectMapper().readValue("\"PLATFORM\"", RealmTypeEnum.class));
        assertEquals("\"USER\"", new ObjectMapper().writeValueAsString(OAuthClientPurposeEnum.USER));
        assertEquals(OAuthClientPurposeEnum.SERVICE,
                new ObjectMapper().readValue("\"SERVICE\"", OAuthClientPurposeEnum.class));
        assertTrue(RealmTypeEnum.class.getDeclaredField("code").isAnnotationPresent(EnumValue.class));
        assertTrue(OAuthClientPurposeEnum.class.getDeclaredField("code").isAnnotationPresent(EnumValue.class));
        assertThrows(IllegalArgumentException.class, () -> RealmTypeEnum.fromCode("TENANTS"));
        assertThrows(IllegalArgumentException.class, () -> OAuthClientPurposeEnum.fromCode("ADMIN"));
    }

    @Test
    void clientJsonbArraysBindTypedStringsUsingJdbcOther() throws Exception {
        OAuthStringListJsonbTypeHandler handler = new OAuthStringListJsonbTypeHandler();
        PreparedStatement statement = mock(PreparedStatement.class);
        handler.setNonNullParameter(statement, 1, List.of("https://client.example/callback"), JdbcType.OTHER);
        verify(statement).setObject(1, "[\"https://client.example/callback\"]", Types.OTHER);

        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getString("redirect_uris"))
                .thenReturn("[\"https://client.example/callback\"]");
        assertEquals(List.of("https://client.example/callback"),
                handler.getNullableResult(resultSet, "redirect_uris"));

        when(resultSet.getString("redirect_uris")).thenReturn("{\"bad\":true}");
        assertThrows(SQLException.class, () -> handler.getNullableResult(resultSet, "redirect_uris"));
        when(resultSet.getString("redirect_uris")).thenReturn("[1]");
        assertThrows(SQLException.class, () -> handler.getNullableResult(resultSet, "redirect_uris"));
        assertThrows(IllegalArgumentException.class,
                () -> handler.setNonNullParameter(statement, 1, List.of(" "), JdbcType.OTHER));
    }

    @Test
    void clientJsonbPOFieldsRemainTypedListsAndSecretsStayInternal() throws Exception {
        for (String name : List.of("redirectUris", "resourceUris", "allowedScopes")) {
            Field field = OAuthClientPO.class.getDeclaredField(name);
            assertEquals(List.class, field.getType());
            Type generic = field.getGenericType();
            assertInstanceOf(ParameterizedType.class, generic);
            ParameterizedType listType = (ParameterizedType) generic;
            assertEquals(String.class, listType.getActualTypeArguments()[0]);
            assertEquals(OAuthStringListJsonbTypeHandler.class,
                    field.getAnnotation(TableField.class).typeHandler());
        }
        OAuthClientPO client = new OAuthClientPO()
                .setSecretHash("{bcrypt}encoded-secret")
                .setSecretHint("aB12");
        assertFalse(client.toString().contains("{bcrypt}encoded-secret"));
        assertFalse(client.toString().contains("aB12"));
        String json = new ObjectMapper().findAndRegisterModules().writeValueAsString(client);
        assertFalse(json.contains("{bcrypt}encoded-secret"));
        assertFalse(json.contains("aB12"));
    }

    private static void assertMapperContract(
            String mapper,
            String table,
            String namespace,
            List<String> reads) throws IOException {
        Path path = repositoryRoot().resolve(ADMIN_PREFIX + "resources/" + mapper);
        assertTrue(Files.isRegularFile(path), "Missing mapper XML: " + mapper);
        String xml = Files.readString(path).toLowerCase();
        assertTrue(xml.contains("from " + table), "Wrong table mapping: " + mapper);
        assertTrue(xml.contains("tenant_id = #{tenantid}"), "Missing trusted tenant predicate: " + mapper);
        assertTrue(xml.contains("deleted_at is null"), "Missing active-row predicate: " + mapper);
        assertTrue(xml.contains("version = #{expectedversion}"), "Missing optimistic update: " + mapper);
        assertTrue(xml.contains("for update"), "Missing command row lock: " + mapper);
        Configuration configuration = new Configuration();
        try (InputStream input = Files.newInputStream(path)) {
            new XMLMapperBuilder(input, configuration, path.toString(), configuration.getSqlFragments())
                    .parse();
        }
        for (String read : reads) {
            assertTrue(configuration.hasStatement(namespace + "." + read, false),
                    "Missing named SQL statement: " + namespace + "." + read);
        }
        assertTrue(configuration.hasStatement(namespace + ".updateExpectedVersion", false));
        assertTrue(configuration.hasStatement(namespace + ".deleteVersionedById", false));
    }

    private static boolean classExists(String name) {
        try {
            Class.forName(name);
            return true;
        } catch (ClassNotFoundException missing) {
            return false;
        }
    }

    private static String generatedSpringBeanName(Class<?> type) {
        return new AnnotationBeanNameGenerator().generateBeanName(
                new AnnotatedGenericBeanDefinition(type), new DefaultListableBeanFactory());
    }

    private static void assertConverterDirection(
            String converterName,
            String sourceName,
            String targetName) throws Exception {
        assertTrue(classExists(converterName), "Missing MapStruct converter: " + converterName);
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
        throw new AssertionError("Converter does not extend BaseConverter<S,T>: " + converterName);
    }

    private static void assertTrustedMdcScope(
            String repositoryName,
            String mapperName,
            String converterName,
            String operationName,
            Class<?>[] operationParameterTypes,
            Object[] operationArguments,
            String mapperMethodName) throws Exception {
        assertTrue(classExists(repositoryName), "Missing repository implementation: " + repositoryName);
        Class<?> mapperType = Class.forName(mapperName);
        Class<?> converterType = Class.forName(converterName);
        List<String> observed = new ArrayList<>();
        AtomicBoolean fail = new AtomicBoolean();
        Answer<Object> answer = invocation -> {
            if (mapperMethodName.equals(invocation.getMethod().getName())) {
                observed.add(MDC.get(EgonColaTenantIdProvider.DEFAULT_MDC_KEY)
                        + "|" + MDC.get("userId"));
                if (fail.get()) {
                    throw new IllegalStateException("expected mapper failure");
                }
            }
            return null;
        };
        Object mapper = mockMapper(mapperType, answer);
        Object converter = Mappers.getMapper((Class) converterType);
        EgonColaMybatisPlusProperties properties = new EgonColaMybatisPlusProperties();
        properties.getTenantId().setMdcKey(EgonColaTenantIdProvider.DEFAULT_MDC_KEY);
        properties.getAudit().setUserIdMdcKey("userId");
        Object userIdProvider = Mockito.mock(EgonColaUserIdProvider.class);
        Class<?> repositoryType = Class.forName(repositoryName);
        var constructor = repositoryType.getConstructor(
                mapperType,
                EgonColaMybatisPlusProperties.class,
                converterType,
                EgonColaUserIdProvider.class,
                Clock.class
        );
        Object repository = constructor.newInstance(
                mapper, properties, converter, userIdProvider, Clock.systemUTC());
        var operation = repositoryType.getMethod(operationName, operationParameterTypes);

        String priorTenant = MDC.get(EgonColaTenantIdProvider.DEFAULT_MDC_KEY);
        String priorUser = MDC.get("userId");
        MDC.put(EgonColaTenantIdProvider.DEFAULT_MDC_KEY, "7001");
        MDC.put("userId", "operator-1");
        try {
            operation.invoke(repository, operationArguments);
            assertEquals(Long.toString(TENANT_PARTITION_ID) + "|operator-1", observed.get(0));
            assertEquals("7001", MDC.get(EgonColaTenantIdProvider.DEFAULT_MDC_KEY));
            assertEquals("operator-1", MDC.get("userId"));

            MDC.put(EgonColaTenantIdProvider.DEFAULT_MDC_KEY, "7002");
            MDC.remove("userId");
            operation.invoke(repository, operationArguments);
            assertEquals(Long.toString(TENANT_PARTITION_ID) + "|service:tianquan-shoubing", observed.get(1));
            assertEquals("7002", MDC.get(EgonColaTenantIdProvider.DEFAULT_MDC_KEY));
            assertNull(MDC.get("userId"));

            MDC.put(EgonColaTenantIdProvider.DEFAULT_MDC_KEY, "7003");
            MDC.put("userId", "operator-3");
            fail.set(true);
            InvocationTargetException failure = assertThrows(
                    InvocationTargetException.class,
                    () -> operation.invoke(repository, operationArguments));
            assertInstanceOf(IllegalStateException.class, failure.getCause());
            assertEquals(Long.toString(TENANT_PARTITION_ID) + "|operator-3", observed.get(2));
            assertEquals("7003", MDC.get(EgonColaTenantIdProvider.DEFAULT_MDC_KEY));
            assertEquals("operator-3", MDC.get("userId"));
        } finally {
            restoreMdc(EgonColaTenantIdProvider.DEFAULT_MDC_KEY, priorTenant);
            restoreMdc("userId", priorUser);
        }
    }

    private static void restoreMdc(String key, String value) {
        if (value == null) {
            MDC.remove(key);
        } else {
            MDC.put(key, value);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Object mockMapper(Class<?> mapperType, Answer<Object> answer) {
        return Mockito.mock((Class) mapperType, answer);
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

    private static Path repositoryRoot() {
        Path cursor = Path.of(System.getProperty("user.dir"))
                .toAbsolutePath()
                .normalize();
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
