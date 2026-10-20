package top.egon.cola.component.tianshu.admin.repository;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;
import top.egon.cola.component.common.core.converter.BaseConverter;
import top.egon.cola.component.common.core.converter.BaseForwardConverter;
import top.egon.cola.component.common.core.pojo.PageResultRecord;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;
import top.egon.cola.component.common.mybatis.extension.EgonColaMapper;
import top.egon.cola.component.common.mybatis.extension.EgonColaRepository;
import top.egon.cola.component.common.mybatis.model.EgonModel;
import top.egon.cola.component.common.mybatis.sharding.EgonColaShardingProperties;
import top.egon.cola.component.common.mybatis.sharding.strategy.EgonColaStandardTenantIdShardingStrategy;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class DdcMetadataRepositoryContractTest {

    private static final String MODEL_PACKAGE = "top.egon.cola.component.tianshu.admin.model.";
    private static final String REPOSITORY_PACKAGE = "top.egon.cola.component.tianshu.admin.repository.";
    private static final String MAPPER_PACKAGE = "top.egon.cola.component.tianshu.admin.dao.";
    private static final String CONVERTER_PACKAGE = "top.egon.cola.component.tianshu.admin.model.convertor.";

    private static final List<MetadataTableBO> TABLES = List.of(
            new MetadataTableBO("DdcBiz", "ddc_biz", "ddc_biz_t0"),
            new MetadataTableBO("DdcEnv", "ddc_env", "ddc_env_t0"),
            new MetadataTableBO("DdcApp", "ddc_app", "ddc_app_t0"),
            new MetadataTableBO("DdcNamespace", "ddc_namespace", "ddc_namespace_t0"),
            new MetadataTableBO("DdcNamespaceEnvAppBinding", "ddc_namespace_env_app", "ddc_namespace_env_app_t0"));

    private static final Map<String, Set<String>> LEGACY_RESULT_FIELDS = Map.of(
            "DdcBiz", Set.of("publicId", "bizCode", "bizName", "description", "enabled", "createdAt", "updatedAt"),
            "DdcEnv", Set.of("publicId", "envCode", "description", "sortOrder", "enabled", "createdAt", "updatedAt"),
            "DdcApp", Set.of("publicId", "appCode", "bizCode", "appName", "owner", "description", "enabled",
                    "createdAt", "updatedAt"),
            "DdcNamespace", Set.of("publicId", "bizCode", "namespace", "description", "enabled", "createdAt",
                    "updatedAt", "namespaceCode"),
            "DdcNamespaceEnvAppBinding", Set.of("id", "bizCode", "namespaceId", "namespaceCode", "env",
                    "appId", "appCode", "appName", "enabled"));

    @Test
    void metadataPersistenceTypesUseTheGuardedMpRepositoryBoundary() throws Exception {
        for (MetadataTableBO table : TABLES) {
            Class<?> po = requiredType(MODEL_PACKAGE + "po." + table.name() + "PO");
            Class<?> bo = requiredType(MODEL_PACKAGE + "bo." + table.name() + "BO");
            Class<?> mapper = requiredType(MAPPER_PACKAGE + table.name() + "Mapper");
            Class<?> repository = requiredType(REPOSITORY_PACKAGE + table.name() + "Repository");
            Class<?> persistenceRepository = requiredType(
                    REPOSITORY_PACKAGE + "impl." + table.name() + "RepositoryImpl");

            assertThat(po.getSuperclass()).as(table.name() + " PO inherits EgonModel").isEqualTo(EgonModel.class);
            assertThat(po.getAnnotation(TableName.class).value()).isEqualTo(table.logicalTable());
            assertThat(EgonModel.class.isAssignableFrom(bo)).as(table.name() + " BO is not a persistence model").isFalse();
            assertThat(mapper.isInterface()).isTrue();
            assertThat(EgonColaMapper.class.isAssignableFrom(mapper)).isTrue();
            assertThat(repository.isInterface()).isTrue();
            assertThat(EgonColaRepository.class.isAssignableFrom(persistenceRepository)).isTrue();

            Class<?> forwardConverter = requiredType(CONVERTER_PACKAGE + table.name() + "ResultConverter");
            assertThat(BaseForwardConverter.class.isAssignableFrom(forwardConverter)).isTrue();
            Class<?> converter = requiredType(CONVERTER_PACKAGE + table.name() + "Converter");
            assertThat(BaseConverter.class.isAssignableFrom(converter)).isTrue();
        }
    }

    @Test
    void metadataMapperXmlKeepsTenantActiveVersionAndNamedPagePredicates() throws IOException {
        ClassLoader classLoader = DdcMetadataRepositoryContractTest.class.getClassLoader();
        for (MetadataTableBO table : TABLES) {
            String mapperName = table.name() + "Mapper";
            try (InputStream input = classLoader.getResourceAsStream(
                    "mybatis/mapper/tianshu/" + mapperName + ".xml")) {
                assertThat(input).as(mapperName + " XML resource").isNotNull();
                String xml = new String(input.readAllBytes(), StandardCharsets.UTF_8);
                String normalizedXml = xml.toLowerCase(java.util.Locale.ROOT);
                assertThat(normalizedXml).contains("namespace=\"" + MAPPER_PACKAGE.toLowerCase(java.util.Locale.ROOT)
                        + mapperName.toLowerCase(java.util.Locale.ROOT) + "\"")
                        .contains("selectactivebyid", "selectactivebyids",
                                table.name().equals("DdcNamespaceEnvAppBinding") ? "selectjoinedpage" : "selectpage",
                                "deleteversionedbyid")
                        .contains("tenant_id", "deleted_at", "is null", "version",
                                "from " + table.logicalTable(), "update " + table.logicalTable())
                        .doesNotContain("from " + table.physicalTable(), "update " + table.physicalTable(), "<delete");
            }
        }
    }

    @Test
    void metadataMapperXmlParsesAndRegistersTheDeclaredStatements() throws IOException {
        Configuration configuration = new Configuration();
        ClassLoader classLoader = DdcMetadataRepositoryContractTest.class.getClassLoader();
        for (MetadataTableBO table : TABLES) {
            String mapperName = table.name() + "Mapper";
            String resource = "mybatis/mapper/tianshu/" + mapperName + ".xml";
            try (InputStream input = classLoader.getResourceAsStream(resource)) {
                assertThat(input).as(resource).isNotNull();
                new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
            }
            String pageStatement = table.name().equals("DdcNamespaceEnvAppBinding") ? "selectJoinedPage" : "selectPage";
            String mapperType = MAPPER_PACKAGE + mapperName;
            assertThat(configuration.hasStatement(mapperType + ".selectActiveById")).isTrue();
            assertThat(configuration.hasStatement(mapperType + ".selectActiveByIds")).isTrue();
            assertThat(configuration.hasStatement(mapperType + "." + pageStatement)).isTrue();
            assertThat(configuration.hasStatement(mapperType + ".deleteVersionedById")).isTrue();
        }
    }

    @Test
    void mainConfigurationLoadsThePersistenceRoutesAndTianshuMapperDirectory() throws IOException {
        String application = resource("application.yml");
        String persistence = resource("egon-tianshu-persistence.yml");
        assertThat(application).contains("classpath:egon-tianshu-persistence.yml",
                "classpath*:mybatis/mapper/tianshu/**/*.xml", "classpath*:mapper/outbox/**/*.xml");
        assertThat(persistence).contains("${TIANSHU_JDBC_URL:${spring.datasource.url}}",
                "${TIANSHU_DB_USERNAME:${spring.datasource.username}}",
                "${TIANSHU_DB_PASSWORD:${spring.datasource.password}}");
    }

    @Test
    void mapperLogicalNamesResolveToConfiguredPhysicalTargetsWithDynamicRenameDisabled() throws Exception {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("tianshu-route-test", Map.of(
                "TIANSHU_JDBC_URL", "jdbc:postgresql://localhost:5432/tianshu",
                "TIANSHU_DB_USERNAME", "test-user",
                "TIANSHU_DB_PASSWORD", "test-password")));
        new YamlPropertySourceLoader().load("tianshu-persistence",
                new ClassPathResource("egon-tianshu-persistence.yml"))
                .forEach(environment.getPropertySources()::addLast);
        EgonColaShardingProperties sharding = Binder.get(environment)
                .bind(EgonColaShardingProperties.PREFIX, Bindable.of(EgonColaShardingProperties.class))
                .orElseThrow(() -> new IllegalStateException("Tianshu sharding configuration is missing"));
        EgonColaMybatisPlusProperties mybatis = Binder.get(environment)
                .bind(EgonColaMybatisPlusProperties.PREFIX, Bindable.of(EgonColaMybatisPlusProperties.class))
                .orElseThrow(() -> new IllegalStateException("Tianshu MP configuration is missing"));
        EgonColaStandardTenantIdShardingStrategy strategy = new EgonColaStandardTenantIdShardingStrategy();

        assertThat(mybatis.getDynamicTableName().isEnabled()).isFalse();
        for (MetadataTableBO table : TABLES) {
            var profile = strategy.toRoutingProfile(table.logicalTable(), sharding.getTables().get(table.logicalTable()),
                    sharding.getDataSources());
            assertThat(profile.logicalTable()).isEqualTo(table.logicalTable());
            assertThat(profile.actualNodes().values().stream().flatMap(List::stream)
                    .map(target -> target.table()).toList()).containsExactly(table.physicalTable());
        }
    }

    @Test
    void inheritedMapperIdReadsDelegateUsingOnlyTheTrustedMdcTenant() throws Exception {
        MDC.put("tenantId", "401");
        try {
            for (MetadataTableBO table : TABLES) {
                Class<?> mapperType = requiredType(MAPPER_PACKAGE + table.name() + "Mapper");
                List<Object[]> idCalls = new ArrayList<>();
                List<Object[]> idsCalls = new ArrayList<>();
                Object mapper = Proxy.newProxyInstance(mapperType.getClassLoader(), new Class<?>[]{mapperType},
                        (proxy, method, arguments) -> {
                            if (method.isDefault()) return InvocationHandler.invokeDefault(proxy, method, arguments);
                            if (method.getName().equals("selectActiveById") && arguments.length == 2) {
                                idCalls.add(arguments);
                                return null;
                            }
                            if (method.getName().equals("selectActiveByIds") && arguments.length == 2) {
                                idsCalls.add(arguments);
                                return List.of();
                            }
                            return null;
                        });

                Method idRead = mapperType.getMethod("selectActiveById", Serializable.class);
                Method idsRead = mapperType.getMethod("selectActiveByIds", Collection.class);
                assertThat(idRead.isDefault()).as(table.name() + " base selectActiveById is tenant-aware").isTrue();
                assertThat(idsRead.isDefault()).as(table.name() + " base selectActiveByIds is tenant-aware").isTrue();
                idRead.invoke(mapper, 810001L);
                idsRead.invoke(mapper, List.of(810001L, 810002L));

                assertThat(idCalls).singleElement().satisfies(call -> {
                    assertThat(call[0]).isEqualTo(401L);
                    assertThat(call[1]).isEqualTo(810001L);
                });
                assertThat(idsCalls).singleElement().satisfies(call -> {
                    assertThat(call[0]).isEqualTo(401L);
                    assertThat(call[1]).isEqualTo(List.of(810001L, 810002L));
                });
            }
        } finally {
            MDC.clear();
        }
    }

    @Test
    void pageResultAndSupportPreserveTheExistingWireEnvelope() throws Exception {
        Class<?> pageResult = requiredType(MODEL_PACKAGE + "vo.DdcPageResult");
        assertThat(pageResult.isRecord()).isFalse();
        assertThat(fieldNames(pageResult)).contains("records", "total", "pageNo", "pageSize");
        Method result = requiredType("top.egon.cola.component.tianshu.admin.support.DdcAdminPageSupport")
                .getMethod("result", pageResult);
        assertThat(result.getReturnType()).isEqualTo(PageResultRecord.class);
    }

    @Test
    void legacyResultConvertersUsePublicIdAndExposeOnlyTheOriginalFields() throws Exception {
        for (MetadataTableBO table : TABLES) {
            Class<?> resultType = requiredType(MODEL_PACKAGE + "vo." + table.name() + "Result");
            assertThat(fieldNames(resultType)).containsExactlyInAnyOrderElementsOf(LEGACY_RESULT_FIELDS.get(table.name()))
                    .doesNotContain("tenantId", "version", "deletedAt", "createUserId", "updateUserId");
            if (!table.name().equals("DdcNamespaceEnvAppBinding")) {
                assertThat(resultType.getDeclaredField("publicId").getAnnotation(JsonProperty.class).value())
                        .isEqualTo("id");
            }

            Class<?> poType = requiredType(MODEL_PACKAGE + "po." + table.name() + "PO");
            Class<?> boType = requiredType(MODEL_PACKAGE + "bo." + table.name() + "BO");
            Object po = poType.getConstructor().newInstance();
            setProperty(po, "id", 810001L);
            setProperty(po, "publicId", "legacy-public-id");

            Object bo = invokeConverter(CONVERTER_PACKAGE + table.name() + "ConverterImpl", poType, po);
            Object result = invokeConverter(CONVERTER_PACKAGE + table.name() + "ResultConverterImpl", boType, bo);
            assertThat(getProperty(result,
                    table.name().equals("DdcNamespaceEnvAppBinding") ? "id" : "publicId"))
                    .isEqualTo("legacy-public-id");
        }
    }

    private static Class<?> requiredType(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException failure) {
            throw new AssertionError("Missing planned metadata contract: " + className, failure);
        }
    }

    private String resource(String name) throws IOException {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(name)) {
            assertThat(input).as(name).isNotNull();
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static Set<String> fieldNames(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields()).map(Field::getName).collect(Collectors.toSet());
    }

    private static Object invokeConverter(String converterClassName, Class<?> sourceType, Object source)
            throws Exception {
        Object converter = requiredType(converterClassName).getConstructor().newInstance();
        return converter.getClass().getMethod("toTarget", sourceType).invoke(converter, source);
    }

    private static void setProperty(Object target, String property, Object value) throws Exception {
        target.getClass().getMethod("set" + Character.toUpperCase(property.charAt(0)) + property.substring(1),
                value.getClass()).invoke(target, value);
    }

    private static Object getProperty(Object target, String property) throws Exception {
        return target.getClass().getMethod("get" + Character.toUpperCase(property.charAt(0)) + property.substring(1))
                .invoke(target);
    }

    private record MetadataTableBO(String name, String logicalTable, String physicalTable) {
    }
}
