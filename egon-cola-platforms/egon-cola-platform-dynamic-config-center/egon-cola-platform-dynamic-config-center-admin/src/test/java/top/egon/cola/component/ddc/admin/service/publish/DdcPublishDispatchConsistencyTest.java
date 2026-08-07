package top.egon.cola.component.ddc.admin.service.publish;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.id.uuid.UuidV7;
import top.egon.cola.component.ddc.admin.common.DdcAdminException;
import top.egon.cola.component.ddc.admin.config.DdcAdminProperties;
import top.egon.cola.component.ddc.admin.model.entity.DdcConfigItemEntity;
import top.egon.cola.component.ddc.admin.model.entity.DdcConfigVersionEntity;
import top.egon.cola.component.ddc.admin.model.entity.DdcPublishAckEntity;
import top.egon.cola.component.ddc.admin.model.entity.DdcPublishTaskEntity;
import top.egon.cola.component.ddc.admin.model.enums.PublishMode;
import top.egon.cola.component.ddc.admin.model.enums.PublishStatus;
import top.egon.cola.component.ddc.admin.model.vo.DdcAtomicPublishCommand;
import top.egon.cola.component.ddc.admin.repository.DdcConfigItemRepository;
import top.egon.cola.component.ddc.admin.repository.DdcConfigVersionRepository;
import top.egon.cola.component.ddc.admin.repository.DdcPublishAckRepository;
import top.egon.cola.component.ddc.admin.repository.DdcPublishTaskRepository;
import top.egon.cola.component.ddc.admin.repository.DdcRedisRepository;
import top.egon.cola.component.ddc.admin.service.config.DdcConfigService;
import top.egon.cola.component.ddc.common.DdcChecksum;
import top.egon.cola.component.ddc.common.DdcErrorStatus;
import top.egon.cola.component.ddc.model.vo.DdcConfigValue;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@DataJpaTest
@Import({
        DdcPendingPublishDispatcher.class,
        DdcPublishStateTransitionService.class,
        DdcConfigService.class,
        PublishResourceLockRegistry.class,
        PublishCompletionWaiterRegistry.class,
        DdcPublishDispatchConsistencyTest.Dependencies.class
})
@EnableConfigurationProperties(DdcAdminProperties.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:sqlite:file:ddc_publish_dispatch_test?mode=memory&cache=shared",
        "spring.datasource.driver-class-name=org.sqlite.JDBC",
        "spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.hikari.maximum-pool-size=2",
        "spring.flyway.enabled=false"
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class DdcPublishDispatchConsistencyTest {

    @Autowired
    private DdcPendingPublishDispatcher dispatcher;

    @Autowired
    private DdcConfigService configService;

    @Autowired
    private DdcPublishStateTransitionService stateTransitions;

    @SpyBean
    private DdcConfigItemRepository configItemRepository;

    @Autowired
    private DdcConfigVersionRepository versionRepository;

    @Autowired
    private DdcPublishTaskRepository taskRepository;

    @Autowired
    private DdcPublishAckRepository ackRepository;

    @Autowired
    private DdcRedisRepository redisRepository;

    @BeforeEach
    void setUp() {
        reset(redisRepository);
    }

    @Test
    void redisFailureLeavesPublishedPointerAndRuntimePullOnPriorVersion() {
        DdcPublishTaskEntity task = savePreparedPublish("redis-failure");
        doThrow(new IllegalStateException("redis response lost"))
                .when(redisRepository)
                .dispatch(any());

        DdcPublishTaskEntity result = dispatcher.dispatch(task.getChangeId());

        assertThat(result.getStatus()).isEqualTo(PublishStatus.UNKNOWN.name());
        assertThat(configItemRepository.findById(task.getConfigId()))
                .get()
                .extracting(DdcConfigItemEntity::getPublishedVersion)
                .isEqualTo(1L);
        assertThat(configService.value(
                "default", "dev", "demo", task.getConfigKey()
        )).extracting(DdcConfigValue::getVersion, DdcConfigValue::getConfigValue)
                .containsExactly(1L, "old");
    }

    @Test
    void databaseFailureReplaysTheSameEventWithoutCreatingAnotherVersion() {
        DdcPublishTaskEntity task = savePreparedPublish("db-failure");
        doThrow(new IllegalStateException("database response lost"))
                .doAnswer(invocation -> {
                    DdcConfigItemEntity config = configItemRepository
                            .findById(task.getConfigId())
                            .orElseThrow();
                    config.setPublishedVersion(task.getTargetVersion());
                    configItemRepository.saveAndFlush(config);
                    return 1;
                })
                .when(configItemRepository)
                .advancePublishedVersion(
                        eq(task.getConfigId()),
                        eq(1L),
                        eq(2L),
                        any(LocalDateTime.class)
                );

        assertThat(dispatcher.dispatch(task.getChangeId()).getStatus())
                .isEqualTo(PublishStatus.UNKNOWN.name());
        assertThat(dispatcher.dispatch(task.getChangeId()).getStatus())
                .isEqualTo(PublishStatus.PUBLISHING.name());
        assertThat(taskRepository.findByChangeId(task.getChangeId()))
                .get()
                .extracting(DdcPublishTaskEntity::getAttemptCount)
                .isEqualTo(1);

        ArgumentCaptor<DdcAtomicPublishCommand> commands =
                ArgumentCaptor.forClass(DdcAtomicPublishCommand.class);
        verify(redisRepository, times(2)).dispatch(commands.capture());
        DdcAtomicPublishCommand first = commands.getAllValues().get(0);
        DdcAtomicPublishCommand replay = commands.getAllValues().get(1);
        assertThat(replay.changeId()).isEqualTo(first.changeId());
        assertThat(replay.targetVersion()).isEqualTo(first.targetVersion());
        assertThat(replay.content()).isEqualTo(first.content());
        assertThat(replay.message().getTimestamp())
                .isEqualTo(first.message().getTimestamp());
        assertThat(replay.message().getChecksum())
                .isEqualTo(first.message().getChecksum());
        assertThat(versionRepository.findByConfigIdOrderByVersionDesc(task.getConfigId()))
                .extracting(DdcConfigVersionEntity::getVersion)
                .containsExactly(2L, 1L);
        assertThat(configItemRepository.findById(task.getConfigId()))
                .get()
                .extracting(DdcConfigItemEntity::getPublishedVersion)
                .isEqualTo(2L);
    }

    @Test
    void changeIdFingerprintConflictIsPersistedAndReturnedWithStableError() {
        DdcPublishTaskEntity task = savePreparedPublish("change-id-conflict");
        doThrow(new DdcAdminException(DdcErrorStatus.CHANGE_ID_CONFLICT))
                .when(redisRepository)
                .dispatch(any());

        assertThatThrownBy(() -> dispatcher.dispatch(task.getChangeId()))
                .isInstanceOf(DdcAdminException.class)
                .hasMessageContaining("change id conflict");

        assertThat(taskRepository.findByChangeId(task.getChangeId()))
                .get()
                .satisfies(failed -> {
                    assertThat(failed.getStatus()).isEqualTo(PublishStatus.FAILED.name());
                    assertThat(failed.getFailureStage()).isEqualTo("REDIS_DISPATCH");
                });
        assertThat(configItemRepository.findById(task.getConfigId()))
                .get()
                .extracting(DdcConfigItemEntity::getPublishedVersion)
                .isEqualTo(1L);
    }

    @Test
    void earlyAckCannotCompleteUntilPublishedPointerAdvances() {
        DdcPublishTaskEntity task = savePreparedPublish("early-ack");
        doAnswer(invocation -> {
            DdcPublishAckEntity target = ackRepository
                    .findByChangeId(task.getChangeId())
                    .getFirst();
            target.setAckStatus("SUCCESS");
            target.setCurrentVersion(task.getTargetVersion());
            target.setAckAt(LocalDateTime.now());
            ackRepository.saveAndFlush(target);
            assertThat(stateTransitions.refreshAfterAck(task.getChangeId()).getStatus())
                    .isEqualTo(PublishStatus.PUBLISHING.name());
            return null;
        }).when(redisRepository).dispatch(any());

        DdcPublishTaskEntity completed = dispatcher.dispatch(task.getChangeId());

        assertThat(completed.getStatus()).isEqualTo(PublishStatus.SUCCESS.name());
        assertThat(configItemRepository.findById(task.getConfigId()))
                .get()
                .extracting(DdcConfigItemEntity::getPublishedVersion)
                .isEqualTo(2L);
    }

    private DdcPublishTaskEntity savePreparedPublish(String configKey) {
        LocalDateTime createdAt = LocalDateTime.of(2026, 7, 26, 8, 0);
        String configId = UuidV7.simpleString();
        DdcConfigItemEntity config = new DdcConfigItemEntity();
        config.setId(configId);
        config.setBizCode("default");
        config.setAppCode("demo");
        config.setEnv("dev");
        config.setConfigKey(configKey);
        config.setConfigValue("new");
        config.setDefaultValue("old");
        config.setValueType("STRING");
        config.setCurrentVersion(2L);
        config.setPublishedVersion(1L);
        config.setEnabled(true);
        config.setDeleted(false);
        config.setCreatedAt(createdAt.minusDays(1));
        config.setUpdatedAt(createdAt);
        configItemRepository.saveAndFlush(config);

        saveVersion(config, 1L, null, "old", createdAt.minusDays(1));
        saveVersion(config, 2L, "old", "new", createdAt);

        DdcPublishTaskEntity task = new DdcPublishTaskEntity();
        task.setId(UuidV7.simpleString());
        task.setChangeId(UuidV7.simpleString());
        task.setConfigId(configId);
        task.setBizCode(config.getBizCode());
        task.setAppCode(config.getAppCode());
        task.setEnv(config.getEnv());
        task.setNamespace(null);
        task.setConfigKey(configKey);
        task.setTargetVersion(2L);
        task.setPublishMode(PublishMode.SYNC_ALL_ACK.name());
        task.setContentChecksum(DdcChecksum.content("new"));
        task.setAttemptCount(0);
        task.setStatus(PublishStatus.PENDING.name());
        task.setTargetCount(1);
        task.setAckCount(0);
        task.setFailedCount(0);
        task.setIgnoredCount(0);
        task.setTimeoutCount(0);
        task.setTimeoutMs(2000L);
        task.setOperator("tester");
        task.setCreatedAt(createdAt);
        task.setUpdatedAt(createdAt);
        taskRepository.saveAndFlush(task);

        DdcPublishAckEntity target = new DdcPublishAckEntity();
        target.setId(UuidV7.simpleString());
        target.setChangeId(task.getChangeId());
        target.setInstanceId("instance-1");
        target.setLeaseId("lease-1");
        target.setContentChecksum(task.getContentChecksum());
        target.setBizCode(task.getBizCode());
        target.setAppCode(task.getAppCode());
        target.setEnv(task.getEnv());
        target.setNamespace(task.getNamespace());
        target.setConfigKey(task.getConfigKey());
        target.setTargetVersion(task.getTargetVersion());
        ackRepository.saveAndFlush(target);
        return task;
    }

    private void saveVersion(DdcConfigItemEntity config,
                             long versionNumber,
                             String oldValue,
                             String newValue,
                             LocalDateTime createdAt) {
        DdcConfigVersionEntity version = new DdcConfigVersionEntity();
        version.setId(UuidV7.simpleString());
        version.setConfigId(config.getId());
        version.setBizCode(config.getBizCode());
        version.setAppCode(config.getAppCode());
        version.setEnv(config.getEnv());
        version.setNamespace(null);
        version.setConfigKey(config.getConfigKey());
        version.setVersion(versionNumber);
        version.setOldValue(oldValue);
        version.setNewValue(newValue);
        version.setValueType(config.getValueType());
        version.setChangeType("UPDATE");
        version.setCreatedAt(createdAt);
        versionRepository.saveAndFlush(version);
    }

    @TestConfiguration
    static class Dependencies {

        @Bean
        DdcRedisRepository ddcRedisRepository() {
            return org.mockito.Mockito.mock(DdcRedisRepository.class);
        }
    }
}
