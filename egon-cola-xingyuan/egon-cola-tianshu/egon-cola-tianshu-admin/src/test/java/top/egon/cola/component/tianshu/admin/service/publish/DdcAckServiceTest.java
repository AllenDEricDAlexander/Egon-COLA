package top.egon.cola.component.tianshu.admin.service.publish;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import top.egon.cola.component.common.id.snowflake.SnowflakeIdGenerator;
import top.egon.cola.component.tianshu.admin.common.DdcAdminException;
import top.egon.cola.component.tianshu.admin.config.DdcAdminProperties;
import top.egon.cola.component.tianshu.admin.model.entity.DdcPublishAckEntity;
import top.egon.cola.component.tianshu.admin.model.bo.DdcConfigItemBO;
import top.egon.cola.component.tianshu.admin.model.entity.DdcPublishTaskEntity;
import top.egon.cola.component.tianshu.admin.model.enums.PublishMode;
import top.egon.cola.component.tianshu.admin.model.enums.PublishStatus;
import top.egon.cola.component.tianshu.admin.repository.DdcPublishAckRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcConfigItemRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcConfigVersionRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcPublishTaskRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcRedisRepository;
import top.egon.cola.component.tianshu.admin.service.lease.DdcConfigLeaseService;
import top.egon.cola.component.tianshu.format.DdcChecksum;
import top.egon.cola.component.tianshu.model.config.DdcAckRequest;
import top.egon.cola.component.tianshu.model.config.DdcAckStatus;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import({
        DdcPublishService.class,
        DdcPendingPublishDispatcher.class,
        DdcPublishStateTransitionService.class,
        PublishFailureRecorder.class,
        PublishResourceLockRegistry.class,
        PublishCompletionWaiterRegistry.class,
        DdcAckServiceTest.Dependencies.class
})
@EnableConfigurationProperties(DdcAdminProperties.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "egon.cola.component.id.machine-id=0",
        "spring.datasource.url=jdbc:sqlite:file:ddc_ack_service_test?mode=memory&cache=shared",
        "spring.datasource.driver-class-name=org.sqlite.JDBC",
        "spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.hikari.maximum-pool-size=1",
        "spring.flyway.enabled=false"
})
class DdcAckServiceTest {

    private static final String CONFIG_VALUE =
            "feature:\n  enabled: true\n";

    private static final String CHECKSUM = DdcChecksum.resource(
            "application.yml",
            "YAML",
            CONFIG_VALUE
    );

    @Autowired
    private DdcPublishService publishService;

    @Autowired
    private DdcPublishTaskRepository taskRepository;

    @MockBean(name = "ddcConfigItemRepository")
    private DdcConfigItemRepository configItemRepository;

    @MockBean(name = "ddcConfigVersionRepository")
    private DdcConfigVersionRepository versionRepository;

    @Autowired
    private DdcPublishAckRepository ackRepository;

    @Autowired
    private DdcPublishStateTransitionService stateTransitions;

    private final Map<String, DdcConfigItemBO> configItemsByPublicId = new HashMap<>();
    private long nextInternalConfigId = 100L;

    @BeforeEach
    void setUp() {
        configItemsByPublicId.clear();
        nextInternalConfigId = 100L;
        Mockito.reset(configItemRepository, versionRepository);
        Mockito.when(configItemRepository.findByPublicId(Mockito.any()))
                .thenAnswer(invocation -> Optional.ofNullable(configItemsByPublicId.get(invocation.getArgument(0))));
    }

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
    void duplicateAckCompletesAfterPublishedPointerCatchesUp() {
        DdcPublishTaskEntity task = savePublishingTask("ack-pointer-catch-up");
        DdcConfigItemBO config = configItemRepository.findByPublicId(task.getConfigId())
                .orElseThrow();
        config.setPublishedVersion(1L);
        saveTarget(task, "instance-1", "lease-1");
        DdcAckRequest request =
                ackRequest(task.getChangeId(), "instance-1", "lease-1", CHECKSUM);

        assertThat(publishService.ack(request).getStatus())
                .isEqualTo(PublishStatus.PUBLISHING.name());
        config = configItemRepository.findByPublicId(task.getConfigId()).orElseThrow();
        config.setPublishedVersion(2L);

        assertThat(publishService.ack(request).getStatus())
                .isEqualTo(PublishStatus.SUCCESS.name());
        assertThat(ackRepository.findByChangeId(task.getChangeId())).hasSize(1);
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

    private DdcPublishTaskEntity savePublishingTask(String label) {
        LocalDateTime now = LocalDateTime.now();
        DdcPublishTaskEntity task = new DdcPublishTaskEntity();
        String configId = SnowflakeIdGenerator.nextId();
        long internalConfigId = nextInternalConfigId++;
        DdcConfigItemBO config = DdcConfigItemBO.builder().id(internalConfigId).tenantId(401L)
                .publicId(configId).bizCode("default").appCode(label).env("dev")
                .resourceName("application.yml").content(CONFIG_VALUE).format("YAML")
                .currentVersion(2L).publishedVersion(2L).enabled(true).deleted(false)
                .createdAt(now).updatedAt(now).version(0L).build();
        configItemsByPublicId.put(configId, config);

        task.setId(SnowflakeIdGenerator.nextId());
        task.setChangeId(SnowflakeIdGenerator.nextId());
        task.setConfigId(configId);
        task.setBizCode("default");
        task.setAppCode(label);
        task.setEnv("dev");
        task.setNamespace("default");
        task.setResourceName("application.yml");
        task.setTargetVersion(2L);
        task.setResourceChecksum(CHECKSUM);
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
        target.setId(SnowflakeIdGenerator.nextId());
        target.setChangeId(task.getChangeId());
        target.setInstanceId(instanceId);
        target.setLeaseId(leaseId);
        target.setBizCode(task.getBizCode());
        target.setAppCode(task.getAppCode());
        target.setEnv(task.getEnv());
        target.setNamespace(task.getNamespace());
        target.setResourceName(task.getResourceName());
        target.setTargetVersion(task.getTargetVersion());
        target.setResourceChecksum(task.getResourceChecksum());
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
        request.setResourceChecksum(checksum);
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
