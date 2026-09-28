package top.egon.cola.component.tianshu.admin.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.TestPropertySource;
import top.egon.cola.component.tianshu.admin.model.entity.DdcInstanceEntity;
import top.egon.cola.component.tianshu.admin.model.entity.DdcPublishTaskEntity;

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

    @Test
    void pagesPublishTasksAndInstancesWithStableFilters() {
        publishTaskRepository.save(task(
                "task-failed", "019-failed", "infra", "FAILED"));
        publishTaskRepository.save(task(
                "task-success", "020-success", "infra", "SUCCESS"));
        publishTaskRepository.save(task(
                "task-retail", "019-retail", "retail", "FAILED"));

        instanceRepository.save(instance("instance-1", "infra", "yuheng"));
        instanceRepository.save(instance("instance-2", "infra", "yuheng"));
        instanceRepository.save(instance("instance-other", "infra", "worker"));

        Page<DdcPublishTaskEntity> tasks = publishTaskRepository.search(
                "infra",
                "prod",
                "yuheng",
                "FAILED",
                "019",
                PageRequest.of(0, 10));
        Page<DdcInstanceEntity> instances = instanceRepository
                .findByBizCodeAndEnvAndAppCode(
                        "infra",
                        "prod",
                        "yuheng",
                        PageRequest.of(0, 10,
                                Sort.by(Sort.Direction.DESC, "updatedAt", "id")));
        assertThat(tasks.getContent())
                .extracting(DdcPublishTaskEntity::getStatus)
                .containsOnly("FAILED");
        assertThat(instances.getTotalElements()).isEqualTo(2);
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
        entity.setAppCode("yuheng");
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

}
