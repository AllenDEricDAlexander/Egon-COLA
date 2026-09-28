package top.egon.cola.component.tianshu.admin.repository;

import com.baomidou.mybatisplus.annotation.TableName;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.common.mybatis.extension.EgonColaMapper;
import top.egon.cola.component.common.mybatis.extension.EgonColaRepository;
import top.egon.cola.component.common.mybatis.model.EgonModel;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class DdcConfigRepositoryContractTest {

    private static final String BASE = "top.egon.cola.component.tianshu.admin.";

    @Test
    void configPersistenceUsesTechnicalVersionDistinctFromLegacyBusinessRevision() throws Exception {
        Class<?> itemPO = requiredType(BASE + "model.po.DdcConfigItemPO");
        Class<?> itemBO = requiredType(BASE + "model.bo.DdcConfigItemBO");
        Class<?> versionPO = requiredType(BASE + "model.po.DdcConfigVersionPO");
        Class<?> versionBO = requiredType(BASE + "model.bo.DdcConfigVersionBO");

        assertThat(itemPO.getSuperclass()).isEqualTo(EgonModel.class);
        assertThat(itemPO.getAnnotation(TableName.class).value()).isEqualTo("ddc_config_item");
        assertThat(versionPO.getSuperclass()).isEqualTo(EgonModel.class);
        assertThat(versionPO.getAnnotation(TableName.class).value()).isEqualTo("ddc_config_version");
        assertThat(EgonModel.class.isAssignableFrom(itemBO)).isFalse();
        assertThat(EgonModel.class.isAssignableFrom(versionBO)).isFalse();
        assertThat(fieldNames(itemPO)).contains("currentVersion", "publishedVersion", "deleted");
        assertThat(fieldNames(itemBO)).contains("currentVersion", "publishedVersion", "deleted");
        assertThat(fieldNames(versionPO)).contains("configId", "configRevision");
        assertThat(fieldNames(versionBO)).contains("configItemId", "configId", "version");
        assertThat(fieldNames(versionPO)).doesNotContain("lockVersion");
    }

    @Test
    void configRepositoriesUseGuardedMpAndKeepVersionHistoryAppendOnly() throws Exception {
        for (String name : new String[]{"DdcConfigItem", "DdcConfigVersion"}) {
            Class<?> mapper = requiredType(BASE + "dao." + name + "Mapper");
            Class<?> repository = requiredType(BASE + "repository." + name + "Repository");
            Class<?> implementation = requiredType(BASE + "repository.impl." + name + "RepositoryImpl");
            assertThat(mapper.isInterface()).isTrue();
            assertThat(EgonColaMapper.class.isAssignableFrom(mapper)).isTrue();
            assertThat(EgonColaRepository.class.isAssignableFrom(implementation)).isTrue();
        }
        Class<?> versionRepository = requiredType(BASE + "repository.DdcConfigVersionRepository");
        assertThat(Arrays.stream(versionRepository.getMethods()).map(java.lang.reflect.Method::getName).toList())
                .doesNotContain("update", "delete");
        Class<?> versionMapper = requiredType(BASE + "dao.DdcConfigVersionMapper");
        assertThat(versionMapper.getMethod("deleteVersionedById",
                requiredType(BASE + "model.po.DdcConfigVersionPO")).isDefault()).isTrue();
        assertThat(versionMapper.getMethod("restoreVersionedById",
                requiredType(BASE + "model.po.DdcConfigVersionPO")).isDefault()).isTrue();
    }

    @Test
    void configMapperXmlRetainsTenantBusinessDeletionAndRevisionContracts() throws IOException {
        String item = mapper("DdcConfigItemMapper.xml").toLowerCase(java.util.Locale.ROOT);
        assertThat(item).contains("namespace=\"" + BASE.toLowerCase(java.util.Locale.ROOT)
                        + "dao.ddcconfigitemmapper\"")
                .contains("from ddc_config_item", "tenant_id", "deleted_at is null", "config_key",
                        "current_version", "published_version", "selectforpublish", "for update",
                        "deleteversionedbyid", "current_timestamp at time zone 'utc'")
                .doesNotContain("from ddc_config_item_t0", "update ddc_config_item_t0", "<delete");

        String history = mapper("DdcConfigVersionMapper.xml").toLowerCase(java.util.Locale.ROOT);
        assertThat(history).contains("namespace=\"" + BASE.toLowerCase(java.util.Locale.ROOT)
                        + "dao.ddcconfigversionmapper\"")
                .contains("from ddc_config_version", "tenant_id", "deleted_at is null", "config_revision",
                        "order by v.config_id, v.config_revision desc, v.id desc", "selectpublishedruntimepage")
                .doesNotContain("update ddc_config_version set", "delete from", "<delete",
                        "from ddc_config_version_t0");

        String service = source("service/config/DdcConfigService.java");
        assertThat(service).contains("setDeleted(true)", "setDeleted(false)")
                .doesNotContain("deleteVersionedById");
    }

    @Test
    void configMapperXmlParsesAndRegistersOnlyMutableItemWrites() throws IOException {
        Configuration configuration = new Configuration();
        for (String mapperName : List.of("DdcConfigItemMapper", "DdcConfigVersionMapper")) {
            String resource = "mybatis/mapper/tianshu/" + mapperName + ".xml";
            try (InputStream input = getClass().getClassLoader().getResourceAsStream(resource)) {
                assertThat(input).as(resource).isNotNull();
                new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
            }
        }
        String itemMapper = BASE + "dao.DdcConfigItemMapper";
        assertThat(configuration.hasStatement(itemMapper + ".selectActiveById")).isTrue();
        assertThat(configuration.hasStatement(itemMapper + ".selectActiveByIds")).isTrue();
        assertThat(configuration.hasStatement(itemMapper + ".selectForPublishByScopeAndResource")).isTrue();
        assertThat(configuration.hasStatement(itemMapper + ".selectPage")).isTrue();
        assertThat(configuration.hasStatement(itemMapper + ".deleteVersionedById")).isTrue();

        String historyMapper = BASE + "dao.DdcConfigVersionMapper";
        assertThat(configuration.hasStatement(historyMapper + ".selectByConfigPublicIdAndRevision")).isTrue();
        assertThat(configuration.hasStatement(historyMapper + ".selectPageByConfigPublicId")).isTrue();
        assertThat(configuration.hasStatement(historyMapper + ".selectPublishedRuntimePage")).isTrue();
        assertThat(configuration.hasStatement(historyMapper + ".deleteVersionedById")).isFalse();
    }

    @Test
    void configMapperXmlParsesAndRegistersTenantScopedStatements() throws IOException {
        Configuration configuration = new Configuration();
        for (String mapperName : List.of("DdcConfigItemMapper", "DdcConfigVersionMapper")) {
            String resource = "mybatis/mapper/tianshu/" + mapperName + ".xml";
            try (InputStream input = getClass().getClassLoader().getResourceAsStream(resource)) {
                assertThat(input).as(resource).isNotNull();
                new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
            }
        }
        String itemMapper = BASE + "dao.DdcConfigItemMapper";
        assertThat(configuration.hasStatement(itemMapper + ".selectActiveById")).isTrue();
        assertThat(configuration.hasStatement(itemMapper + ".selectActiveByIds")).isTrue();
        assertThat(configuration.hasStatement(itemMapper + ".selectForPublishByScopeAndResource")).isTrue();
        assertThat(configuration.hasStatement(itemMapper + ".selectPage")).isTrue();
        assertThat(configuration.hasStatement(itemMapper + ".deleteVersionedById")).isTrue();

        String versionMapper = BASE + "dao.DdcConfigVersionMapper";
        assertThat(configuration.hasStatement(versionMapper + ".selectByConfigPublicIdAndRevision")).isTrue();
        assertThat(configuration.hasStatement(versionMapper + ".selectPageByConfigPublicId")).isTrue();
        assertThat(configuration.hasStatement(versionMapper + ".selectPublishedRuntimePage")).isTrue();
        assertThat(configuration.hasStatement(versionMapper + ".deleteVersionedById")).isFalse();
    }

    @Test
    void serviceAndLegacyViewsRetainExternalIdsAndExcludeJpaEntities() throws Exception {
        String service = source("service/config/DdcConfigService.java");
        assertThat(service).doesNotContain("DdcConfigItemEntity", "DdcConfigVersionEntity", "JpaRepository");
        Class<?> configView = requiredType(BASE + "model.vo.DdcConfigVO");
        Class<?> versionView = requiredType(BASE + "model.vo.DdcConfigVersionVO");
        assertThat(fieldNames(configView)).containsExactlyInAnyOrder("id", "bizCode", "env", "appCode",
                "visibleNamespaces", "resourceName", "content", "format", "currentVersion", "description",
                "enabled", "deleted", "updatedAt");
        assertThat(fieldNames(versionView)).containsExactlyInAnyOrder("id", "configId", "version", "resourceName",
                "oldContent", "newContent", "format", "changeType", "changeReason", "operator", "createdAt");
    }

    private Class<?> requiredType(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException failure) {
            throw new AssertionError("Missing config persistence contract: " + name, failure);
        }
    }

    private Set<String> fieldNames(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields()).map(Field::getName).collect(Collectors.toSet());
    }

    private String mapper(String name) throws IOException {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("mybatis/mapper/tianshu/" + name)) {
            assertThat(input).as(name).isNotNull();
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private String source(String relativeName) throws IOException {
        return Files.readString(Path.of("src/main/java/top/egon/cola/component/tianshu/admin")
                .resolve(relativeName), StandardCharsets.UTF_8);
    }
}
