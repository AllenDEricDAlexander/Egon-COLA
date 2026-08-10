package top.egon.cola.component.ddc.admin.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;
import top.egon.cola.component.ddc.admin.model.entity.DdcConfigItemEntity;
import top.egon.cola.component.ddc.admin.model.entity.DdcConfigVersionEntity;

import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:sqlite:file:ddc_config_paging_test?mode=memory&cache=shared",
        "spring.datasource.driver-class-name=org.sqlite.JDBC",
        "spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.hikari.maximum-pool-size=1",
        "spring.flyway.enabled=false"
})
class DdcConfigPagingRepositoryTest {

    @Autowired
    private DdcConfigItemRepository configItemRepository;

    @Autowired
    private DdcConfigVersionRepository versionRepository;

    @Test
    void pagesConfigContentCountAndVersionHistoryInDatabase() {
        IntStream.rangeClosed(1, 11)
                .mapToObj(index -> config("config-%02d".formatted(index), "infra"))
                .forEach(configItemRepository::save);
        configItemRepository.save(config("config-retail", "retail"));

        versionRepository.save(version("version-1", 1L));
        versionRepository.save(version("version-2", 2L));
        versionRepository.save(version("version-3", 3L));

        Page<DdcConfigItemEntity> page = configItemRepository.search(
                "infra",
                null,
                null,
                null,
                null,
                false,
                PageRequest.of(1, 5));
        Page<DdcConfigVersionEntity> versions = versionRepository
                .findByConfigIdOrderByVersionDescIdDesc(
                        "config-1", PageRequest.of(0, 2));

        assertThat(page.getContent()).hasSize(5);
        assertThat(page.getTotalElements()).isEqualTo(11);
        assertThat(page.getNumber()).isEqualTo(1);
        assertThat(versions.getContent())
                .extracting(DdcConfigVersionEntity::getVersion)
                .containsExactly(3L, 2L);
        assertThat(versions.getTotalElements()).isEqualTo(3);
    }

    private DdcConfigItemEntity config(String id, String bizCode) {
        DdcConfigItemEntity entity = new DdcConfigItemEntity();
        entity.setId(id);
        entity.setBizCode(bizCode);
        entity.setAppCode("gateway");
        entity.setEnv("prod");
        entity.setResourceName(id + ".yml");
        entity.setContent("feature:\n  enabled: true\n");
        entity.setFormat("YAML");
        entity.setCurrentVersion(1L);
        entity.setEnabled(true);
        entity.setDeleted(false);
        return entity;
    }

    private DdcConfigVersionEntity version(String id, long version) {
        DdcConfigVersionEntity entity = new DdcConfigVersionEntity();
        entity.setId(id);
        entity.setConfigId("config-1");
        entity.setBizCode("infra");
        entity.setAppCode("gateway");
        entity.setEnv("prod");
        entity.setResourceName("application.yml");
        entity.setVersion(version);
        entity.setFormat("YAML");
        entity.setChangeType("UPDATE");
        return entity;
    }
}
