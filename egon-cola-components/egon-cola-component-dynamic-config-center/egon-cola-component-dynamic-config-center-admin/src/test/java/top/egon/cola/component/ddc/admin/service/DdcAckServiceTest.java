package top.egon.cola.component.ddc.admin.service;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import top.egon.cola.component.common.id.uuid.UuidV7;
import top.egon.cola.component.ddc.admin.common.DdcAdminException;
import top.egon.cola.component.ddc.admin.config.DdcAdminProperties;
import top.egon.cola.component.ddc.admin.model.entity.DdcPublishAckEntity;
import top.egon.cola.component.ddc.admin.model.entity.DdcPublishTaskEntity;
import top.egon.cola.component.ddc.admin.model.enums.PublishMode;
import top.egon.cola.component.ddc.admin.model.enums.PublishStatus;
import top.egon.cola.component.ddc.admin.repository.DdcPublishAckRepository;
import top.egon.cola.component.ddc.admin.repository.DdcPublishTaskRepository;
import top.egon.cola.component.ddc.admin.repository.DdcRedisRepository;
import top.egon.cola.component.ddc.common.DdcChecksum;
import top.egon.cola.component.ddc.model.dto.DdcAckRequest;
import top.egon.cola.component.ddc.model.enums.DdcAckStatus;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import({
        DdcPublishService.class,
        DdcPublishStateTransitionService.class,
        PublishFailureRecorder.class,
        PublishResourceLockRegistry.class,
        PublishCompletionWaiterRegistry.class,
        DdcAckServiceTest.Dependencies.class
})
@EnableConfigurationProperties(DdcAdminProperties.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:sqlite:file:ddc_ack_service_test?mode=memory&cache=shared",
        "spring.datasource.driver-class-name=org.sqlite.JDBC",
        "spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.hikari.maximum-pool-size=1",
        "spring.flyway.enabled=false"
})
class DdcAckServiceTest {

    private static final String CHECKSUM = DdcChecksum.content("true");

    @Autowired
    private DdcPublishService publishService;

    @Autowired
    private DdcPublishTaskRepository taskRepository;

    @Autowired
    private DdcPublishAckRepository ackRepository;

    @Autowired
    private DdcPublishStateTransitionService stateTransitions;

    @Test
    void exactDuplicateAckIsIdempotentAndCompletesTask() {
        DdcPublishTaskEntity task = savePublishingTask("ack-success");
        saveTarget(task, "instance-1", "lease-1");
        DdcAckRequest request =
                ackRequest(task.getChangeId(), "instance-1", "lease-1", CHECKSUM);

        publishService.ack(request);
        publishService.ack(request);

        assertThat(ackRepository.findByChangeId(task.getChangeId())).hasSize(1);
        assertThat(taskRepository.findByChangeId(task.getChangeId()))
                .get()
                .satisfies(updated -> {
                    assertThat(updated.getAckCount()).isEqualTo(1);
                    assertThat(updated.getStatus()).isEqualTo(PublishStatus.SUCCESS.name());
                });
    }

    @Test
    void rejectsUnknownIdentityVersionAndChecksumWithoutCreatingTarget() {
        DdcPublishTaskEntity task = savePublishingTask("ack-reject");
        saveTarget(task, "instance-1", "lease-1");

        assertThatThrownBy(() -> publishService.ack(
                ackRequest(task.getChangeId(), "instance-1", "stale-lease", CHECKSUM)
        )).isInstanceOf(DdcAdminException.class);
        DdcAckRequest wrongVersion =
                ackRequest(task.getChangeId(), "instance-1", "lease-1", CHECKSUM);
        wrongVersion.setTargetVersion(3L);
        assertThatThrownBy(() -> publishService.ack(wrongVersion))
                .isInstanceOf(DdcAdminException.class);
        assertThatThrownBy(() -> publishService.ack(
                ackRequest(task.getChangeId(), "instance-1", "lease-1", "wrong")
        )).isInstanceOf(DdcAdminException.class);

        assertThat(ackRepository.findByChangeId(task.getChangeId()))
                .singleElement()
                .satisfies(target -> assertThat(target.getAckStatus()).isNull());
        assertThat(taskRepository.findByChangeId(task.getChangeId()))
                .get()
                .satisfies(updated -> {
                    assertThat(updated.getAckCount()).isZero();
                    assertThat(updated.getStatus()).isEqualTo(PublishStatus.PUBLISHING.name());
                });
    }

    @Test
    void failedAckWinsAndTerminalTaskIsReadOnly() {
        DdcPublishTaskEntity task = savePublishingTask("ack-failed");
        saveTarget(task, "instance-1", "lease-1");
        DdcAckRequest failed =
                ackRequest(task.getChangeId(), "instance-1", "lease-1", CHECKSUM);
        failed.setStatus(DdcAckStatus.FAILED);

        publishService.ack(failed);
        publishService.ack(
                ackRequest(task.getChangeId(), "instance-1", "lease-1", CHECKSUM)
        );

        assertThat(taskRepository.findByChangeId(task.getChangeId()))
                .get()
                .extracting(DdcPublishTaskEntity::getStatus)
                .isEqualTo(PublishStatus.FAILED.name());
        assertThat(ackRepository.findByChangeId(task.getChangeId()))
                .singleElement()
                .extracting(DdcPublishAckEntity::getAckStatus)
                .isEqualTo(DdcAckStatus.FAILED.name());
    }

    @Test
    void ackAndTimeoutEachPreserveTheFirstTerminalWinner() {
        DdcPublishTaskEntity ackWinner = savePublishingTask("ack-wins");
        saveTarget(ackWinner, "instance-1", "lease-1");
        publishService.ack(ackRequest(
                ackWinner.getChangeId(),
                "instance-1",
                "lease-1",
                CHECKSUM
        ));
        stateTransitions.timeout(ackWinner.getChangeId(), "late timeout");
        assertThat(taskRepository.findByChangeId(ackWinner.getChangeId()))
                .get()
                .extracting(DdcPublishTaskEntity::getStatus)
                .isEqualTo(PublishStatus.SUCCESS.name());

        DdcPublishTaskEntity timeoutWinner = savePublishingTask("timeout-wins");
        saveTarget(timeoutWinner, "instance-1", "lease-1");
        stateTransitions.timeout(timeoutWinner.getChangeId(), "timeout");
        publishService.ack(ackRequest(
                timeoutWinner.getChangeId(),
                "instance-1",
                "lease-1",
                CHECKSUM
        ));
        assertThat(taskRepository.findByChangeId(timeoutWinner.getChangeId()))
                .get()
                .extracting(DdcPublishTaskEntity::getStatus)
                .isEqualTo(PublishStatus.TIMEOUT.name());
        assertThat(ackRepository.findByChangeId(timeoutWinner.getChangeId()))
                .singleElement()
                .extracting(DdcPublishAckEntity::getAckStatus)
                .isEqualTo(DdcAckStatus.TIMEOUT.name());
    }

    private DdcPublishTaskEntity savePublishingTask(String configKey) {
        LocalDateTime now = LocalDateTime.now();
        DdcPublishTaskEntity task = new DdcPublishTaskEntity();
        task.setId(UuidV7.simpleString());
        task.setChangeId(UuidV7.simpleString());
        task.setConfigId(UuidV7.simpleString());
        task.setAppCode("demo");
        task.setEnv("dev");
        task.setNamespace("default");
        task.setConfigKey(configKey);
        task.setTargetVersion(2L);
        task.setContentChecksum(CHECKSUM);
        task.setPublishMode(PublishMode.SYNC_ALL_ACK.name());
        task.setStatus(PublishStatus.PUBLISHING.name());
        task.setTargetCount(1);
        task.setAckCount(0);
        task.setFailedCount(0);
        task.setIgnoredCount(0);
        task.setTimeoutCount(0);
        task.setTimeoutMs(2000L);
        task.setCreatedAt(now);
        task.setDispatchedAt(now);
        task.setUpdatedAt(now);
        return taskRepository.saveAndFlush(task);
    }

    private void saveTarget(DdcPublishTaskEntity task,
                            String instanceId,
                            String leaseId) {
        DdcPublishAckEntity target = new DdcPublishAckEntity();
        target.setId(UuidV7.simpleString());
        target.setChangeId(task.getChangeId());
        target.setInstanceId(instanceId);
        target.setLeaseId(leaseId);
        target.setAppCode(task.getAppCode());
        target.setEnv(task.getEnv());
        target.setNamespace(task.getNamespace());
        target.setConfigKey(task.getConfigKey());
        target.setTargetVersion(task.getTargetVersion());
        target.setContentChecksum(task.getContentChecksum());
        ackRepository.saveAndFlush(target);
    }

    private DdcAckRequest ackRequest(String changeId,
                                     String instanceId,
                                     String leaseId,
                                     String checksum) {
        DdcAckRequest request = new DdcAckRequest();
        request.setChangeId(changeId);
        request.setInstanceId(instanceId);
        request.setLeaseId(leaseId);
        request.setTargetVersion(2L);
        request.setCurrentVersion(2L);
        request.setContentChecksum(checksum);
        request.setStatus(DdcAckStatus.SUCCESS);
        return request;
    }

    @TestConfiguration
    static class Dependencies {

        @Bean
        DdcRedisRepository ddcRedisRepository() {
            return Mockito.mock(DdcRedisRepository.class);
        }

        @Bean
        DdcConfigLeaseService ddcConfigLeaseService() {
            return Mockito.mock(DdcConfigLeaseService.class);
        }
    }
}
