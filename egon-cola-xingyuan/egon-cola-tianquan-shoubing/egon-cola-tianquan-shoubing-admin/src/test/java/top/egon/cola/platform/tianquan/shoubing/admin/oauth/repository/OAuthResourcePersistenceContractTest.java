package top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.JdbcType;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.Answers;
import org.mockito.Mockito;
import org.slf4j.MDC;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;
import top.egon.cola.component.common.mybatis.business.EgonColaTenantIdProvider;
import top.egon.cola.component.common.mybatis.business.EgonColaUserIdProvider;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.common.mybatis.model.EgonColaModelValidationUtils;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.dao.OAuthStringListJsonbTypeHandler;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.TrustedRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.enums.RealmTypeEnum;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.dao.OAuthGrantMapper;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.dao.OAuthResourceMapper;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.bo.OAuthGrantBO;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.bo.OAuthResourceBO;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.converter.OAuthGrantConverter;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.converter.OAuthResourceConverter;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.po.OAuthGrantPO;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.po.OAuthResourcePO;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.repository.impl.OAuthGrantRepositoryImpl;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.repository.impl.OAuthResourceRepositoryImpl;
import top.egon.cola.platform.tianquan.shoubing.core.resource.ClientResourceGrant;
import top.egon.cola.platform.tianquan.shoubing.core.resource.ResourceGrantType;
import top.egon.cola.platform.tianquan.shoubing.core.resource.ResourceServerStatus;

import java.io.IOException;
import java.io.InputStream;
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
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class OAuthResourcePersistenceContractTest {

    private static final String ADMIN_PREFIX =
            "egon-cola-xingyuan/egon-cola-tianquan-shoubing/"
                    + "egon-cola-tianquan-shoubing-admin/src/main/";
    private static final long PLATFORM_PARTITION_ID = 9_001L;
    private static final long TENANT_PARTITION_ID = 1_001L;

    @Test
    void oauthResourceAndGrantDdlExposeExactBusinessColumnsAndTenantKeys() throws IOException {
        String ddl = Files.readString(repositoryRoot().resolve(
                ADMIN_PREFIX + "resources/db/egon-mp/"
                        + "V20261008_001__initialize_oauth_tenant_schema.sql"));

        assertTrue(ddl.contains("CREATE TABLE oauth_resource"));
        for (String column : List.of("resource_server_id", "resource_uri", "biz_code",
                "app_code", "environment", "display_name", "management_client_id",
                "management_client_pk", "rbac_application_code", "entry_permission_code",
                "status", "admission_ticket_ttl_seconds")) {
            assertTrue(ddl.contains(column), "Missing oauth_resource column " + column);
        }
        assertTrue(ddl.contains("CREATE TABLE oauth_grant"));
        for (String column : List.of("client_pk", "resource_server_id", "grant_type",
                "allowed_scopes", "status", "resource_version")) {
            assertTrue(ddl.contains(column), "Missing oauth_grant column " + column);
        }
        assertTrue(ddl.contains("uk_oauth_resource_server_permanent"));
        assertTrue(ddl.contains("uk_oauth_resource_manager_permanent"));
        assertTrue(ddl.contains("uk_grant_1_active"));
        assertTrue(ddl.contains("jsonb_typeof(allowed_scopes) = 'array'"));
    }

    @Test
    void resourceAndGrantPOsMapOnlyTheirOwnColumnsAndReuseTypedScopeHandler() throws Exception {
        assertEquals("oauth_resource", OAuthResourcePO.class.getAnnotation(TableName.class).value());
        assertEquals("oauth_grant", OAuthGrantPO.class.getAnnotation(TableName.class).value());
        for (String field : List.of("resourceServerId", "resourceUri", "bizCode", "appCode",
                "environment", "displayName", "managementClientId", "managementClientPk",
                "rbacApplicationCode", "entryPermissionCode", "status", "admissionTicketTtlSeconds")) {
            OAuthResourcePO.class.getDeclaredField(field);
        }
        for (String field : List.of("clientPk", "resourceServerId", "grantType", "allowedScopes",
                "status", "resourceVersion")) {
            OAuthGrantPO.class.getDeclaredField(field);
        }
        TableField scopes = OAuthGrantPO.class.getDeclaredField("allowedScopes")
                .getAnnotation(TableField.class);
        assertEquals(OAuthStringListJsonbTypeHandler.class, scopes.typeHandler());
        assertFalse(List.of(OAuthResourcePO.class.getDeclaredFields()).stream()
                .anyMatch(field -> List.of("id", "tenantId", "version", "createTime", "updateTime")
                        .contains(field.getName())));
    }

    @Test
    void resourceAndGrantMappersParseAndUseTenantActiveVersionAndCommandLocks() throws IOException {
        assertMapperContract(
                "mapper/resource/OAuthResourceMapper.xml",
                "oauth_resource",
                "top.egon.cola.platform.tianquan.shoubing.admin.resource.dao.OAuthResourceMapper",
                List.of("selectByResourceServerId", "selectByResourceServerIdForUpdate",
                        "selectByResourceUri", "selectBySource", "selectByManagementClientPk",
                        "selectByBizCodeAndEnvironmentAndAppCodeIn", "selectActiveById",
                        "selectActiveByIds", "selectDirectory"));
        assertMapperContract(
                "mapper/resource/OAuthGrantMapper.xml",
                "oauth_grant",
                "top.egon.cola.platform.tianquan.shoubing.admin.resource.dao.OAuthGrantMapper",
                List.of("selectGrant", "selectGrantForUpdate", "listByClient",
                        "selectActiveById", "selectActiveByIds"));
    }

    @Test
    void resourceDirectoryQueryIsPlatformScopedOrderedAndBoundedForOverflowDetection() throws Exception {
        String xml = Files.readString(repositoryRoot().resolve(
                ADMIN_PREFIX + "resources/mapper/resource/OAuthResourceMapper.xml"));
        int statementStart = xml.indexOf("<select id=\"selectDirectory\"");
        assertTrue(statementStart >= 0, "Missing bounded resource directory mapper");
        String statement = xml.substring(statementStart, xml.indexOf("</select>", statementStart));
        assertTrue(statement.contains("tenant_id = #{tenantId}"));
        assertTrue(statement.contains("deleted_at IS NULL"));
        assertTrue(statement.contains("ORDER BY resource_server_id ASC, id ASC"));
        assertTrue(statement.contains("LIMIT #{limit}"));
        assertTrue(java.util.Arrays.stream(OAuthResourceMapper.class.getMethods())
                .anyMatch(method -> method.getName().equals("selectDirectory")));
        assertTrue(java.util.Arrays.stream(top.egon.cola.platform.tianquan.shoubing.admin.resource.repository
                        .OAuthResourceRepository.class.getMethods())
                .anyMatch(method -> method.getName().equals("listDirectory")));
    }

    @Test
    void generatedConvertersUsePOAsBaseConverterSourceAndRoundTripBusinessTypes() throws Exception {
        String resourceConverterName =
                "top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.converter.OAuthResourceConverter";
        assertConverterDirection(resourceConverterName,
                "top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.po.OAuthResourcePO",
                "top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.bo.OAuthResourceBO");
        Object resourceConverter = Mappers.getMapper((Class) Class.forName(resourceConverterName));
        OAuthResourcePO resourceSource = new OAuthResourcePO();
        resourceSource.setId(61L);
        resourceSource.setVersion(4L);
        resourceSource.setResourceServerId("orders-resource")
                .setResourceUri("https://api.example/orders")
                .setBizCode("orders").setAppCode("orders").setEnvironment("prod")
                .setDisplayName("Orders").setManagementClientId("orders-service")
                .setManagementClientPk(901L).setRbacApplicationCode("orders")
                .setEntryPermissionCode("orders:entry").setStatus("ACTIVE")
                .setAdmissionTicketTtlSeconds(300);
        OAuthResourceBO resource = (OAuthResourceBO) resourceConverter.getClass()
                .getMethod("toTarget", OAuthResourcePO.class)
                .invoke(resourceConverter, resourceSource);
        assertEquals(URI.create("https://api.example/orders"), resource.getResourceUri());
        assertEquals(Duration.ofSeconds(300), resource.getAdmissionTicketTtl());
        assertEquals(ResourceServerStatus.ACTIVE, resource.getStatus());
        OAuthResourcePO resourceCopy = (OAuthResourcePO) resourceConverter.getClass()
                .getMethod("toSource", OAuthResourceBO.class)
                .invoke(resourceConverter, resource);
        assertEquals(resourceSource.getResourceServerId(), resourceCopy.getResourceServerId());
        assertEquals(resourceSource.getManagementClientPk(), resourceCopy.getManagementClientPk());

        String grantConverterName =
                "top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.converter.OAuthGrantConverter";
        assertConverterDirection(grantConverterName,
                "top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.po.OAuthGrantPO",
                "top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.bo.OAuthGrantBO");
        Object grantConverter = Mappers.getMapper((Class) Class.forName(grantConverterName));
        OAuthGrantPO grantSource = new OAuthGrantPO();
        grantSource.setId(71L);
        grantSource.setVersion(2L);
        grantSource.setClientPk(901L);
        grantSource.setResourceServerId("orders-resource").setGrantType("CLIENT_CREDENTIALS")
                .setAllowedScopes(List.of("orders:read", "orders:write"))
                .setStatus("ACTIVE").setResourceVersion(4L);
        OAuthGrantBO grant = (OAuthGrantBO) grantConverter.getClass()
                .getMethod("toTarget", OAuthGrantPO.class)
                .invoke(grantConverter, grantSource);
        assertEquals(ResourceGrantType.CLIENT_CREDENTIALS, grant.getGrantType());
        assertEquals(ClientResourceGrant.Status.ACTIVE, grant.getStatus());
        assertEquals(grantSource.getAllowedScopes(), grant.getAllowedScopes());
        OAuthGrantPO grantCopy = (OAuthGrantPO) grantConverter.getClass()
                .getMethod("toSource", OAuthGrantBO.class)
                .invoke(grantConverter, grant);
        assertEquals(grantSource.getGrantType(), grantCopy.getGrantType());
        assertEquals(grantSource.getAllowedScopes(), grantCopy.getAllowedScopes());
    }

    @Test
    void grantScopesReuseTypedJsonbStringArrayHandler() throws Exception {
        OAuthStringListJsonbTypeHandler handler = new OAuthStringListJsonbTypeHandler();
        PreparedStatement statement = mock(PreparedStatement.class);
        handler.setNonNullParameter(statement, 2, List.of("orders:read", "orders:write"), JdbcType.OTHER);
        verify(statement).setObject(2, "[\"orders:read\",\"orders:write\"]", Types.OTHER);

        ResultSet resultSet = mock(ResultSet.class);
        Mockito.when(resultSet.getString("allowed_scopes"))
                .thenReturn("[\"orders:read\",\"orders:write\"]");
        assertEquals(List.of("orders:read", "orders:write"),
                handler.getNullableResult(resultSet, "allowed_scopes"));
        Mockito.when(resultSet.getString("allowed_scopes")).thenReturn("[1]");
        assertThrows(SQLException.class, () -> handler.getNullableResult(resultSet, "allowed_scopes"));
        assertThrows(IllegalArgumentException.class,
                () -> handler.setNonNullParameter(statement, 2, List.of(" "), JdbcType.OTHER));
    }

    @Test
    void namedMappersConvertersAndRepositoriesArePresentAndQualifiersMatchBeanNames() throws Exception {
        String resourceMapper =
                "top.egon.cola.platform.tianquan.shoubing.admin.resource.dao.OAuthResourceMapper";
        String grantMapper =
                "top.egon.cola.platform.tianquan.shoubing.admin.resource.dao.OAuthGrantMapper";
        String resourceConverter =
                "top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.converter.OAuthResourceConverterImpl";
        String grantConverter =
                "top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.converter.OAuthGrantConverterImpl";
        String resourceRepository =
                "top.egon.cola.platform.tianquan.shoubing.admin.resource.repository.impl.OAuthResourceRepositoryImpl";
        String grantRepository =
                "top.egon.cola.platform.tianquan.shoubing.admin.resource.repository.impl.OAuthGrantRepositoryImpl";
        for (String name : List.of(resourceMapper, grantMapper, resourceConverter, grantConverter,
                resourceRepository, grantRepository)) {
            assertTrue(classExists(name), "Missing resource/grant persistence type " + name);
        }
        assertEquals("OAuthResourceMapper", generatedSpringBeanName(Class.forName(resourceMapper)));
        assertEquals("OAuthGrantMapper", generatedSpringBeanName(Class.forName(grantMapper)));
        assertEquals("OAuthResourceConverterImpl", generatedSpringBeanName(Class.forName(resourceConverter)));
        assertEquals("OAuthGrantConverterImpl", generatedSpringBeanName(Class.forName(grantConverter)));
        String resourceSource = Files.readString(repositoryRoot().resolve(
                ADMIN_PREFIX + "java/top/egon/cola/platform/tianquan/shoubing/admin/resource/"
                        + "repository/impl/OAuthResourceRepositoryImpl.java"));
        String grantSource = Files.readString(repositoryRoot().resolve(
                ADMIN_PREFIX + "java/top/egon/cola/platform/tianquan/shoubing/admin/resource/"
                        + "repository/impl/OAuthGrantRepositoryImpl.java"));
        assertTrue(resourceSource.contains("@Qualifier(\"OAuthResourceMapper\")"));
        assertTrue(resourceSource.contains("@Qualifier(\"OAuthResourceConverterImpl\")"));
        assertTrue(grantSource.contains("@Qualifier(\"OAuthGrantMapper\")"));
        assertTrue(grantSource.contains("@Qualifier(\"OAuthGrantConverterImpl\")"));
    }

    @Test
    void resourceRepositoryRequiresPlatformRealmAndRestoresMdcAfterSuccessAndFailure() throws Exception {
        assertTrustedMdcScope(
                "top.egon.cola.platform.tianquan.shoubing.admin.resource.repository.impl.OAuthResourceRepositoryImpl",
                "top.egon.cola.platform.tianquan.shoubing.admin.resource.dao.OAuthResourceMapper",
                "top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.converter.OAuthResourceConverter",
                "findByResourceServerId",
                new Class<?>[]{TrustedRealmBO.class, String.class},
                new Object[]{trustedPlatform(), "orders-resource"},
                "selectByResourceServerId",
                PLATFORM_PARTITION_ID);
    }

    @Test
    void grantRepositoryUsesTenantStoragePartitionAndRestoresMdcAfterSuccessAndFailure() throws Exception {
        assertTrustedMdcScope(
                "top.egon.cola.platform.tianquan.shoubing.admin.resource.repository.impl.OAuthGrantRepositoryImpl",
                "top.egon.cola.platform.tianquan.shoubing.admin.resource.dao.OAuthGrantMapper",
                "top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.converter.OAuthGrantConverter",
                "findGrant",
                new Class<?>[]{TrustedRealmBO.class, Long.class, String.class, ResourceGrantType.class},
                new Object[]{trustedTenant(), 901L, "orders-resource", ResourceGrantType.USER_DELEGATION},
                "selectGrant",
                TENANT_PARTITION_ID);
    }

    @Test
    void platformResourceRealmRejectsTenantRealmBeforeMapperAccess() throws Exception {
        String repositoryName =
                "top.egon.cola.platform.tianquan.shoubing.admin.resource.repository.impl.OAuthResourceRepositoryImpl";
        assertTrue(classExists(repositoryName));
        Class<?> mapperType = Class.forName(
                "top.egon.cola.platform.tianquan.shoubing.admin.resource.dao.OAuthResourceMapper");
        AtomicBoolean mapped = new AtomicBoolean();
        Object mapper = Mockito.mock(mapperType, invocation -> {
            mapped.set(true);
            return Answers.RETURNS_DEFAULTS.answer(invocation);
        });
        Class<?> converterType = Class.forName(
                "top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.converter.OAuthResourceConverter");
        Object converter = Mappers.getMapper((Class) converterType);
        EgonColaMybatisPlusProperties properties = new EgonColaMybatisPlusProperties();
        properties.getTenantId().setMdcKey(EgonColaTenantIdProvider.DEFAULT_MDC_KEY);
        properties.getAudit().setUserIdMdcKey("userId");
        EgonColaUserIdProvider userIdProvider = mock(EgonColaUserIdProvider.class);
        Object repository = Class.forName(repositoryName).getConstructor(mapperType,
                EgonColaMybatisPlusProperties.class, converterType, EgonColaUserIdProvider.class,
                Clock.class).newInstance(mapper, properties, converter, userIdProvider, Clock.systemUTC());
        var method = repository.getClass().getMethod("findByResourceServerId", TrustedRealmBO.class, String.class);
        assertThrows(InvocationTargetException.class,
                () -> method.invoke(repository, trustedTenant(), "orders-resource"));
        assertFalse(mapped.get());
    }

    @Test
    void staleExpectedVersionDoesNotUpdateResourceOrGrantRows() {
        EgonColaMybatisPlusProperties properties = new EgonColaMybatisPlusProperties();
        properties.getTenantId().setMdcKey(EgonColaTenantIdProvider.DEFAULT_MDC_KEY);
        properties.getAudit().setUserIdMdcKey("userId");
        EgonColaUserIdProvider userIdProvider = mock(EgonColaUserIdProvider.class);

        OAuthResourcePO storedResource = resourceRow(81L, 3L);
        AtomicBoolean resourceUpdated = new AtomicBoolean();
        OAuthResourceMapper resourceMapper = Mockito.mock(OAuthResourceMapper.class, invocation -> {
            if (invocation.getMethod().getName().equals("selectByResourceServerIdForUpdate")) {
                return storedResource;
            }
            if (invocation.getMethod().getName().equals("updateExpectedVersion")) {
                resourceUpdated.set(true);
            }
            return Answers.RETURNS_DEFAULTS.answer(invocation);
        });
        OAuthResourceConverter resourceConverter = Mappers.getMapper(OAuthResourceConverter.class);
        OAuthResourceRepositoryImpl resources = new OAuthResourceRepositoryImpl(
                resourceMapper, properties, resourceConverter, userIdProvider, Clock.systemUTC());
        OAuthResourceBO staleResource = resourceConverter.toTarget(storedResource).setVersion(2L);
        org.junit.jupiter.api.Assertions.assertThrows(
                org.springframework.dao.OptimisticLockingFailureException.class,
                () -> resources.save(trustedPlatform(), staleResource, 1L));
        assertFalse(resourceUpdated.get());
        Mockito.verify(resourceMapper, Mockito.never())
                .updateExpectedVersion(Mockito.anyLong(), Mockito.any(), Mockito.anyLong());

        OAuthGrantPO storedGrant = grantRow(91L, 3L);
        AtomicBoolean grantUpdated = new AtomicBoolean();
        OAuthGrantMapper grantMapper = Mockito.mock(OAuthGrantMapper.class, invocation -> {
            if (invocation.getMethod().getName().equals("selectGrantForUpdate")) {
                return storedGrant;
            }
            if (invocation.getMethod().getName().equals("updateExpectedVersion")) {
                grantUpdated.set(true);
            }
            return Answers.RETURNS_DEFAULTS.answer(invocation);
        });
        OAuthGrantConverter grantConverter = Mappers.getMapper(OAuthGrantConverter.class);
        OAuthGrantRepositoryImpl grants = new OAuthGrantRepositoryImpl(
                grantMapper, properties, grantConverter, userIdProvider, Clock.systemUTC());
        OAuthGrantBO staleGrant = grantConverter.toTarget(storedGrant).setVersion(2L);
        org.junit.jupiter.api.Assertions.assertThrows(
                org.springframework.dao.OptimisticLockingFailureException.class,
                () -> grants.save(trustedTenant(), staleGrant, 1L));
        assertFalse(grantUpdated.get());
        Mockito.verify(grantMapper, Mockito.never())
                .updateExpectedVersion(Mockito.anyLong(), Mockito.any(), Mockito.anyLong());
    }

    @Test
    void resourceInsertPersistsCanonicalUriAndGrantInsertPersistsSortedUniqueScopes() {
        EgonColaMybatisPlusProperties properties = new EgonColaMybatisPlusProperties();
        properties.getTenantId().setMdcKey(EgonColaTenantIdProvider.DEFAULT_MDC_KEY);
        properties.getAudit().setUserIdMdcKey("userId");
        EgonColaUserIdProvider userIdProvider = mock(EgonColaUserIdProvider.class);

        AtomicReference<OAuthResourcePO> insertedResource = new AtomicReference<>();
        OAuthResourceMapper resourceMapper = Mockito.mock(OAuthResourceMapper.class, invocation -> {
            if (invocation.getMethod().getName().equals("selectByResourceServerIdForUpdate")) {
                return null;
            }
            if (invocation.getMethod().getName().equals("insert")) {
                insertedResource.set(invocation.getArgument(0));
                return 1;
            }
            return Answers.RETURNS_DEFAULTS.answer(invocation);
        });
        OAuthResourceConverter resourceConverter = Mappers.getMapper(OAuthResourceConverter.class);
        OAuthResourceRepositoryImpl resources = new OAuthResourceRepositoryImpl(
                resourceMapper, properties, resourceConverter, userIdProvider, Clock.systemUTC());
        OAuthResourceBO resource = OAuthResourceBO.builder()
                .version(0L)
                .resourceServerId("orders-resource")
                .resourceUri(URI.create("https://api.example/orders/../orders"))
                .bizCode("orders").appCode("orders").environment("prod")
                .displayName("Orders").managementClientId("orders-service")
                .managementClientPk(901L).rbacApplicationCode("orders")
                .entryPermissionCode("orders:entry").status(ResourceServerStatus.ACTIVE)
                .admissionTicketTtl(Duration.ofSeconds(300))
                .build();
        AtomicReference<OAuthGrantPO> insertedGrant = new AtomicReference<>();
        OAuthGrantMapper grantMapper = Mockito.mock(OAuthGrantMapper.class, invocation -> {
            if (invocation.getMethod().getName().equals("selectGrantForUpdate")) {
                return null;
            }
            if (invocation.getMethod().getName().equals("insert")) {
                insertedGrant.set(invocation.getArgument(0));
                return 1;
            }
            return Answers.RETURNS_DEFAULTS.answer(invocation);
        });
        OAuthGrantConverter grantConverter = Mappers.getMapper(OAuthGrantConverter.class);
        OAuthGrantRepositoryImpl grants = new OAuthGrantRepositoryImpl(
                grantMapper, properties, grantConverter, userIdProvider, Clock.systemUTC());
        OAuthGrantBO grant = OAuthGrantBO.builder()
                .version(0L).clientPk(901L).resourceServerId("orders-resource")
                .grantType(ResourceGrantType.CLIENT_CREDENTIALS)
                .allowedScopes(List.of("orders:write", "orders:read"))
                .status(ClientResourceGrant.Status.ACTIVE).resourceVersion(4L)
                .build();
        try (ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory()) {
            EgonColaModelValidationUtils.Binding binding = EgonColaModelValidationUtils.current() == null
                    ? EgonColaModelValidationUtils.initialize(
                            new ValidationUtils(validatorFactory.getValidator()),
                            "OAuthResourcePersistenceContractTest")
                    : null;
            try {
                OAuthResourceBO savedResource = resources.save(trustedPlatform(), resource, 0L);
                assertEquals("https://api.example/orders", insertedResource.get().getResourceUri());
                assertEquals(URI.create("https://api.example/orders"), savedResource.getResourceUri());

                OAuthGrantBO savedGrant = grants.save(trustedTenant(), grant, 0L);
                assertEquals(List.of("orders:read", "orders:write"), insertedGrant.get().getAllowedScopes());
                assertEquals(insertedGrant.get().getAllowedScopes(), savedGrant.getAllowedScopes());
            } finally {
                if (binding != null) {
                    binding.close();
                }
            }
        }
    }

    private static void assertMapperContract(
            String resource,
            String table,
            String namespace,
            List<String> reads) throws IOException {
        Path path = repositoryRoot().resolve(ADMIN_PREFIX + "resources/" + resource);
        assertTrue(Files.isRegularFile(path), "Missing mapper XML " + resource);
        String xml = Files.readString(path).toLowerCase();
        assertTrue(xml.contains("from " + table), "Wrong table mapping: " + resource);
        assertTrue(xml.contains("tenant_id = #{tenantid}"), "Missing trusted tenant predicate: " + resource);
        assertTrue(xml.contains("deleted_at is null"), "Missing active-row predicate: " + resource);
        assertTrue(xml.contains("version = #{expectedversion}"), "Missing optimistic update: " + resource);
        assertTrue(xml.contains("for update"), "Missing command row lock: " + resource);
        assertFalse(xml.contains("${"), "Unsafe SQL interpolation in " + resource);
        Configuration configuration = new Configuration();
        try (InputStream input = Files.newInputStream(path)) {
            new XMLMapperBuilder(input, configuration, path.toString(), configuration.getSqlFragments())
                    .parse();
        }
        for (String read : reads) {
            assertTrue(configuration.hasStatement(namespace + "." + read, false),
                    "Missing named SQL statement " + namespace + "." + read);
        }
        assertTrue(configuration.hasStatement(namespace + ".updateExpectedVersion", false));
        assertTrue(configuration.hasStatement(namespace + ".deleteVersionedById", false));
    }

    private static void assertConverterDirection(
            String converterName,
            String sourceName,
            String targetName) throws Exception {
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
        throw new AssertionError("Converter does not extend BaseConverter<S,T>: " + converterName);
    }

    private static void assertTrustedMdcScope(
            String repositoryName,
            String mapperName,
            String converterName,
            String operationName,
            Class<?>[] operationParameterTypes,
            Object[] operationArguments,
            String mapperMethodName,
            long expectedPartition) throws Exception {
        assertTrue(classExists(repositoryName), "Missing repository implementation " + repositoryName);
        Class<?> mapperType = Class.forName(mapperName);
        Class<?> converterType = Class.forName(converterName);
        List<String> observed = new ArrayList<>();
        AtomicBoolean fail = new AtomicBoolean();
        Object mapper = Mockito.mock(mapperType, invocation -> {
            if (mapperMethodName.equals(invocation.getMethod().getName())) {
                observed.add(MDC.get(EgonColaTenantIdProvider.DEFAULT_MDC_KEY)
                        + "|" + MDC.get("userId"));
                if (fail.get()) {
                    throw new IllegalStateException("expected mapper failure");
                }
            }
            return Answers.RETURNS_DEFAULTS.answer(invocation);
        });
        Object converter = Mappers.getMapper((Class) converterType);
        EgonColaMybatisPlusProperties properties = new EgonColaMybatisPlusProperties();
        properties.getTenantId().setMdcKey(EgonColaTenantIdProvider.DEFAULT_MDC_KEY);
        properties.getAudit().setUserIdMdcKey("userId");
        EgonColaUserIdProvider userIdProvider = mock(EgonColaUserIdProvider.class);
        Class<?> repositoryType = Class.forName(repositoryName);
        Object repository = repositoryType.getConstructor(mapperType,
                EgonColaMybatisPlusProperties.class, converterType, EgonColaUserIdProvider.class,
                Clock.class).newInstance(mapper, properties, converter, userIdProvider, Clock.systemUTC());
        var operation = repositoryType.getMethod(operationName, operationParameterTypes);

        String oldTenant = MDC.get(EgonColaTenantIdProvider.DEFAULT_MDC_KEY);
        String oldUser = MDC.get("userId");
        MDC.put(EgonColaTenantIdProvider.DEFAULT_MDC_KEY, "7001");
        MDC.put("userId", "operator-1");
        try {
            operation.invoke(repository, operationArguments);
            assertEquals(expectedPartition + "|operator-1", observed.get(0));
            assertEquals("7001", MDC.get(EgonColaTenantIdProvider.DEFAULT_MDC_KEY));
            assertEquals("operator-1", MDC.get("userId"));

            MDC.put(EgonColaTenantIdProvider.DEFAULT_MDC_KEY, "7002");
            MDC.remove("userId");
            operation.invoke(repository, operationArguments);
            assertEquals(expectedPartition + "|service:tianquan-shoubing", observed.get(1));
            assertEquals("7002", MDC.get(EgonColaTenantIdProvider.DEFAULT_MDC_KEY));
            org.junit.jupiter.api.Assertions.assertNull(MDC.get("userId"));

            MDC.put(EgonColaTenantIdProvider.DEFAULT_MDC_KEY, "7003");
            MDC.put("userId", "operator-3");
            fail.set(true);
            InvocationTargetException failure = assertThrows(InvocationTargetException.class,
                    () -> operation.invoke(repository, operationArguments));
            assertInstanceOf(IllegalStateException.class, failure.getCause());
            assertEquals(expectedPartition + "|operator-3", observed.get(2));
            assertEquals("7003", MDC.get(EgonColaTenantIdProvider.DEFAULT_MDC_KEY));
            assertEquals("operator-3", MDC.get("userId"));
        } finally {
            restoreMdc(EgonColaTenantIdProvider.DEFAULT_MDC_KEY, oldTenant);
            restoreMdc("userId", oldUser);
        }
    }

    private static void restoreMdc(String key, String value) {
        if (value == null) {
            MDC.remove(key);
        } else {
            MDC.put(key, value);
        }
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
        return new org.springframework.context.annotation.AnnotationBeanNameGenerator()
                .generateBeanName(new org.springframework.beans.factory.annotation.AnnotatedGenericBeanDefinition(type),
                        new org.springframework.beans.factory.support.DefaultListableBeanFactory());
    }

    private static TrustedRealmBO trustedPlatform() {
        return TrustedRealmBO.builder()
                .issuer(URI.create("https://auth.example/platform"))
                .realmKey("platform")
                .realmType(RealmTypeEnum.PLATFORM)
                .storagePartitionId(PLATFORM_PARTITION_ID)
                .businessTenantId(null)
                .trustVersion(0L)
                .build();
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

    private static OAuthResourcePO resourceRow(long id, long version) {
        OAuthResourcePO po = new OAuthResourcePO();
        po.setId(id);
        po.setTenantId(PLATFORM_PARTITION_ID);
        po.setVersion(version);
        po.setResourceServerId("orders-resource");
        po.setResourceUri("https://api.example/orders");
        po.setBizCode("orders");
        po.setAppCode("orders");
        po.setEnvironment("prod");
        po.setDisplayName("Orders");
        po.setManagementClientId("orders-service");
        po.setManagementClientPk(901L);
        po.setRbacApplicationCode("orders");
        po.setEntryPermissionCode("orders:entry");
        po.setStatus("ACTIVE");
        po.setAdmissionTicketTtlSeconds(300);
        return po;
    }

    private static OAuthGrantPO grantRow(long id, long version) {
        OAuthGrantPO po = new OAuthGrantPO();
        po.setId(id);
        po.setTenantId(TENANT_PARTITION_ID);
        po.setVersion(version);
        po.setClientPk(901L);
        po.setResourceServerId("orders-resource");
        po.setGrantType("CLIENT_CREDENTIALS");
        po.setAllowedScopes(List.of("orders:read"));
        po.setStatus("ACTIVE");
        po.setResourceVersion(4L);
        return po;
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
