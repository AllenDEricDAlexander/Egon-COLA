package top.egon.cola.component.ddc.admin.service;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import top.egon.cola.component.common.id.uuid.UuidV7;
import top.egon.cola.component.ddc.admin.model.entity.DdcPublishTaskEntity;
import top.egon.cola.component.ddc.admin.model.enums.PublishMode;
import top.egon.cola.component.ddc.admin.model.enums.PublishStatus;
import top.egon.cola.component.ddc.admin.repository.DdcPublishAckRepository;
import top.egon.cola.component.ddc.admin.repository.DdcPublishTaskRepository;
import top.egon.cola.component.ddc.admin.repository.DdcRedisRepository;
import top.egon.cola.component.ddc.admin.service.policy.PublishConsistencyPolicyFactory;
import top.egon.cola.component.ddc.model.dto.DdcAckRequest;
import top.egon.cola.component.ddc.model.enums.DdcAckStatus;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({
        DdcPublishService.class,
        PublishFailureRecorder.class,
        PublishConsistencyPolicyFactory.class,
        DdcAckServiceTest.RedisTestConfig.class
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:sqlite:file:ddc_ack_service_test?mode=memory&cache=shared",
        "spring.datasource.driver-class-name=org.sqlite.JDBC",
        "spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.hikari.maximum-pool-size=2",
        "spring.flyway.enabled=false",
        "egon.cola.component.ddc.enabled=false"
})
class DdcAckServiceTest {

    @Autowired
    private DdcPublishService publishService;

    @Autowired
    private DdcPublishTaskRepository publishTaskRepository;

    @Autowired
    private DdcPublishAckRepository publishAckRepository;

    @Test
    void duplicateAckUpdatesSameRowAndCompletesTask() {
        DdcPublishTaskEntity task = publishTask();
        publishTaskRepository.save(task);
        DdcAckRequest request = ackRequest(task.getChangeId());

        publishService.ack(request);
        publishService.ack(request);

        assertThat(publishAckRepository.findByChangeId(task.getChangeId())).hasSize(1);
        assertThat(publishTaskRepository.findByChangeId(task.getChangeId()))
                .get()
                .satisfies(updated -> {
                    assertThat(updated.getAckCount()).isEqualTo(1);
                    assertThat(updated.getStatus()).isEqualTo(PublishStatus.SUCCESS.name());
                });
    }

    private DdcPublishTaskEntity publishTask() {
        LocalDateTime now = LocalDateTime.now();
        DdcPublishTaskEntity task = new DdcPublishTaskEntity();
        task.setId(UuidV7.simpleString());
        task.setChangeId("c1");
        task.setAppCode("demo");
        task.setEnv("dev");
        task.setNamespace("default");
        task.setConfigKey("switch");
        task.setTargetVersion(2L);
        task.setPublishMode(PublishMode.STRONG_ALL_ACK.name());
        task.setStatus(PublishStatus.PUBLISHING.name());
        task.setTargetCount(1);
        task.setAckCount(0);
        task.setFailedCount(0);
        task.setIgnoredCount(0);
        task.setTimeoutCount(0);
        task.setCreatedAt(now);
        task.setUpdatedAt(now);
        return task;
    }

    private DdcAckRequest ackRequest(String changeId) {
        DdcAckRequest request = new DdcAckRequest();
        request.setChangeId(changeId);
        request.setInstanceId("i1");
        request.setAppCode("demo");
        request.setEnv("dev");
        request.setNamespace("default");
        request.setConfigKey("switch");
        request.setTargetVersion(2L);
        request.setCurrentVersion(2L);
        request.setStatus(DdcAckStatus.SUCCESS);
        return request;
    }

    @TestConfiguration
    static class RedisTestConfig {

        @Bean
        DdcRedisRepository ddcRedisRepository() {
            return Mockito.mock(DdcRedisRepository.class);
        }
    }
}
