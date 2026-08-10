package top.egon.cola.component.ddc.admin.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.TestPropertySource;
import top.egon.cola.component.ddc.admin.model.entity.DdcConfigItemEntity;
import top.egon.cola.component.ddc.admin.model.entity.DdcConfigVersionEntity;
import top.egon.cola.component.ddc.admin.model.entity.DdcInstanceEntity;
import top.egon.cola.component.ddc.admin.model.entity.DdcPublishTaskEntity;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:sqlite:file:ddc_operational_paging_test?mode=memory&cache=shared",
        "spring.datasource.driver-class-name=org.sqlite.JDBC",
        "spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.hikari.maximum-pool-size=1",
        "spring.flyway.enabled=false"
})
class DdcOperationalPagingRepositoryTest {

    @Autowired
    private DdcPublishTaskRepository publishTaskRepository;

    @Autowired
    private DdcInstanceRepository instanceRepository;

    @Autowired
    private DdcConfigItemRepository configItemRepository;

    @Autowired
    private DdcConfigVersionRepository versionRepository;

    @Test
    void pagesPublishTasksInstancesAndPublishedCacheSeeds() {
        publishTaskRepository.save(task(
                "task-failed", "019-failed", "infra", "FAILED"));
        publishTaskRepository.save(task(
                "task-success", "020-success", "infra", "SUCCESS"));
        publishTaskRepository.save(task(
                "task-retail", "019-retail", "retail", "FAILED"));

        instanceRepository.save(instance("instance-1", "infra", "gateway"));
        instanceRepository.save(instance("instance-2", "infra", "gateway"));
        instanceRepository.save(instance("instance-other", "infra", "worker"));

        savePublishedConfig("cache-config-1", "cache-version-1", "application.yml");
        savePublishedConfig("cache-config-2", "cache-version-2", "application.yaml");

        Page<DdcPublishTaskEntity> tasks = publishTaskRepository.search(
                "infra",
                "prod",
                "gateway",
                "FAILED",
                "019",
                PageRequest.of(0, 10));
        Page<DdcInstanceEntity> instances = instanceRepository
                .findByBizCodeAndEnvAndAppCode(
                        "infra",
                        "prod",
                        "gateway",
                        PageRequest.of(0, 10,
                                Sort.by(Sort.Direction.DESC, "updatedAt", "id")));
        Page<DdcConfigVersionEntity> versions = versionRepository
                .findPublishedRuntimeVersions(
                        "infra",
                        "prod",
                        "gateway",
                        "DELETE",
                        PageRequest.of(0, 1));

        assertThat(tasks.getContent())
                .extracting(DdcPublishTaskEntity::getStatus)
                .containsOnly("FAILED");
        assertThat(instances.getTotalElements()).isEqualTo(2);
        assertThat(versions.getTotalElements()).isEqualTo(2);
        assertThat(versions.getContent()).hasSize(1);
    }

    private DdcPublishTaskEntity task(
            String id,
            String changeId,
            String bizCode,
            String status) {
        DdcPublishTaskEntity entity = new DdcPublishTaskEntity();
        entity.setId(id);
        entity.setChangeId(changeId);
        entity.setBizCode(bizCode);
        entity.setEnv("prod");
        entity.setAppCode("gateway");
        entity.setStatus(status);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        return entity;
    }

    private DdcInstanceEntity instance(
            String id,
            String bizCode,
            String appCode) {
        DdcInstanceEntity entity = new DdcInstanceEntity();
        entity.setId(id);
        entity.setInstanceId(id);
        entity.setBizCode(bizCode);
        entity.setEnv("prod");
        entity.setAppCode(appCode);
        entity.setHost("127.0.0.1");
        entity.setPort(9000);
        entity.setStatus("ONLINE");
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setRuntimeMetadata(Map.of());
        return entity;
    }

    private void savePublishedConfig(
            String configId,
            String versionId,
            String resourceName) {
        DdcConfigItemEntity item = new DdcConfigItemEntity();
        item.setId(configId);
        item.setBizCode("infra");
        item.setEnv("prod");
        item.setAppCode("gateway");
        item.setResourceName(resourceName);
        item.setContent("feature:\n  enabled: true\n");
        item.setFormat("YAML");
        item.setCurrentVersion(1L);
        item.setPublishedVersion(1L);
        item.setEnabled(true);
        item.setDeleted(false);
        configItemRepository.save(item);

        DdcConfigVersionEntity version = new DdcConfigVersionEntity();
        version.setId(versionId);
        version.setConfigId(configId);
        version.setBizCode("infra");
        version.setEnv("prod");
        version.setAppCode("gateway");
        version.setResourceName(resourceName);
        version.setVersion(1L);
        version.setNewContent(item.getContent());
        version.setFormat("YAML");
        version.setChangeType("UPDATE");
        versionRepository.save(version);
    }
}
