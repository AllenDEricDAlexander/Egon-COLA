package top.egon.cola.component.tianshu.admin.service.publish;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.id.snowflake.SnowflakeIdGenerator;
import top.egon.cola.component.tianshu.admin.common.DdcAdminException;
import top.egon.cola.component.tianshu.admin.config.DdcAdminProperties;
import top.egon.cola.component.tianshu.admin.model.bo.DdcConfigItemBO;
import top.egon.cola.component.tianshu.admin.model.bo.DdcConfigVersionBO;
import top.egon.cola.component.tianshu.admin.model.convertor.DdcConfigItemConverterImpl;
import top.egon.cola.component.tianshu.admin.model.convertor.DdcConfigVersionConverterImpl;
import top.egon.cola.component.tianshu.admin.model.bo.DdcPublishAckBO;
import top.egon.cola.component.tianshu.admin.model.bo.DdcPublishTaskBO;
import top.egon.cola.component.tianshu.admin.model.enums.PublishMode;
import top.egon.cola.component.tianshu.admin.model.enums.PublishStatus;
import top.egon.cola.component.tianshu.admin.model.vo.DdcAtomicPublishCommand;
import top.egon.cola.component.tianshu.admin.repository.DdcConfigItemRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcConfigVersionRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcNamespaceEnvAppBindingRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcPublishAckRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcPublishTaskRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcRedisRepository;
import top.egon.cola.component.tianshu.admin.service.config.DdcConfigService;
import top.egon.cola.component.tianshu.format.DdcChecksum;
import top.egon.cola.component.tianshu.error.DdcErrorStatus;
import top.egon.cola.component.tianshu.model.config.DdcConfigValue;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DataJpaTest
@Import({
        DdcPendingPublishDispatcher.class,
        DdcPublishStateTransitionService.class,
        DdcConfigService.class,
        DdcConfigItemConverterImpl.class,
        DdcConfigVersionConverterImpl.class,
        PublishResourceLockRegistry.class,
        PublishCompletionWaiterRegistry.class,
        DdcPublishDispatchConsistencyTest.Dependencies.class
})
@EnableConfigurationProperties(DdcAdminProperties.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "egon.cola.component.id.machine-id=0",
        "spring.datasource.url=jdbc:sqlite:file:ddc_publish_dispatch_test?mode=memory&cache=shared",
        "spring.datasource.driver-class-name=org.sqlite.JDBC",
        "spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.hikari.maximum-pool-size=2",
        "spring.flyway.enabled=false",
        "egon.cola.component.tianshu.admin.publish.inline-content-max-bytes=64"
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class DdcPublishDispatchConsistencyTest {

    private static final String OLD_YAML = "feature:\n  value: old\n";

    private static final String NEW_YAML = "feature:\n  value: new\n";

    @Autowired
    private DdcPendingPublishDispatcher dispatcher;

    @Autowired
    private DdcConfigService configService;

    @Autowired
    private DdcPublishStateTransitionService stateTransitions;

    @MockBean(name = "ddcConfigItemRepository")
    private DdcConfigItemRepository configItemRepository;

    @MockBean(name = "ddcConfigVersionRepository")
    private DdcConfigVersionRepository versionRepository;

    @MockBean(name = "ddcNamespaceEnvAppBindingRepository")
    private DdcNamespaceEnvAppBindingRepository bindingRepository;

    @Autowired
    private DdcPublishTaskRepository taskRepository;

    @Autowired
    private DdcPublishAckRepository ackRepository;

    @Autowired
    private DdcRedisRepository redisRepository;

    private final Map<String, DdcConfigItemBO> configItems = new HashMap<>();
    private final Map<String, DdcConfigVersionBO> configVersions = new HashMap<>();
    private long nextConfigId;

    @BeforeEach
    void setUp() {
        reset(configItemRepository, versionRepository, bindingRepository, redisRepository);
        configItems.clear();
        configVersions.clear();
        nextConfigId = 700L;
        when(configItemRepository.findByPublicId(any())).thenAnswer(invocation ->
                Optional.ofNullable(configItems.get(invocation.getArgument(0))));
        when(configItemRepository.findByBizCodeAndEnvAndAppCode(any(), any(), any())).thenAnswer(invocation ->
                configItems.values().stream().filter(item -> java.util.Objects.equals(item.getBizCode(), invocation.getArgument(0))
                        && java.util.Objects.equals(item.getEnv(), invocation.getArgument(1))
                        && java.util.Objects.equals(item.getAppCode(), invocation.getArgument(2))).toList());
        when(configItemRepository.update(any(DdcConfigItemBO.class))).thenAnswer(invocation -> {
            DdcConfigItemBO item = invocation.getArgument(0);
            item.setVersion(item.getVersion() + 1);
            configItems.put(item.getPublicId(), item);
            return item;
        });
        when(configItemRepository.advancePublishedVersion(any(), any(), any(), any())).thenAnswer(invocation -> {
            DdcConfigItemBO item = configItems.get(invocation.getArgument(0));
            if (item == null || !java.util.Objects.equals(item.getPublishedVersion(), invocation.getArgument(1))) return 0;
            item.setPublishedVersion(invocation.getArgument(2)).setVersion(item.getVersion() + 1);
            return 1;
        });
        when(versionRepository.insert(any(DdcConfigVersionBO.class))).thenAnswer(invocation -> {
            DdcConfigVersionBO version = invocation.getArgument(0);
            configVersions.put(versionKey(version.getConfigId(), version.getConfigRevision()), version);
            return version;
        });
        when(versionRepository.findByConfigIdAndVersion(any(), any())).thenAnswer(invocation ->
                Optional.ofNullable(configVersions.get(versionKey(invocation.getArgument(0), invocation.getArgument(1)))));
        when(versionRepository.findByConfigItemIdAndRevision(any(), any())).thenAnswer(invocation ->
                configVersions.values().stream().filter(version -> java.util.Objects.equals(
                        version.getConfigItemId(), invocation.getArgument(0))
                        && java.util.Objects.equals(version.getConfigRevision(), invocation.getArgument(1)))
                        .findFirst());
        when(versionRepository.findByConfigIdOrderByVersionDesc(any())).thenAnswer(invocation ->
                configVersions.values().stream().filter(version -> java.util.Objects.equals(
                        version.getConfigId(), invocation.getArgument(0)))
                        .sorted(java.util.Comparator.comparing(DdcConfigVersionBO::getConfigRevision).reversed()).toList());
    }

    @Test
    void redisFailureLeavesPublishedPointerAndRuntimePullOnPriorVersion() {
        DdcPublishTaskBO task = savePreparedPublish("redis-failure");
        doThrow(new IllegalStateException("redis response lost"))
                .when(redisRepository)
                .dispatch(any());

        DdcPublishTaskBO result = dispatcher.dispatch(task.getChangeId());

        assertThat(result.getStatus()).isEqualTo(PublishStatus.UNKNOWN.name());
        assertThat(configItemRepository.findByPublicId(task.getConfigId()))
                .get()
                .extracting(DdcConfigItemBO::getPublishedVersion)
                .isEqualTo(1L);
        assertThat(configService.pull("default", "dev", task.getAppCode()))
                .singleElement()
                .extracting(
                        DdcConfigValue::getVersion,
                        DdcConfigValue::getContent
                )
                .containsExactly(1L, OLD_YAML);
    }

    @Test
    void databaseFailureReplaysTheSameEventWithoutCreatingAnotherVersion() {
        DdcPublishTaskBO task = savePreparedPublish("db-failure");
        doThrow(new IllegalStateException("database response lost"))
                .doAnswer(invocation -> {
                    DdcConfigItemBO config = configItemRepository
                            .findByPublicId(task.getConfigId())
                            .orElseThrow();
                    config.setPublishedVersion(task.getTargetVersion());
                    config.setVersion(config.getVersion() + 1);
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
                .extracting(DdcPublishTaskBO::getAttemptCount)
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
                .extracting(DdcConfigVersionBO::getConfigRevision)
                .containsExactly(2L, 1L);
        assertThat(configItemRepository.findByPublicId(task.getConfigId()))
                .get()
                .extracting(DdcConfigItemBO::getPublishedVersion)
                .isEqualTo(2L);
    }

    @Test
    void changeIdFingerprintConflictIsPersistedAndReturnedWithStableError() {
        DdcPublishTaskBO task = savePreparedPublish("change-id-conflict");
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
        assertThat(configItemRepository.findByPublicId(task.getConfigId()))
                .get()
                .extracting(DdcConfigItemBO::getPublishedVersion)
                .isEqualTo(1L);
    }

    @Test
    void earlyAckCannotCompleteUntilPublishedPointerAdvances() {
        DdcPublishTaskBO task = savePreparedPublish("early-ack");
        doAnswer(invocation -> {
            DdcPublishAckBO target = ackRepository
                    .findByChangeId(task.getChangeId())
                    .getFirst();
            target.setAckStatus("SUCCESS");
            target.setCurrentVersion(task.getTargetVersion());
            target.setAckAt(LocalDateTime.now());
            ackRepository.update(target);
            assertThat(stateTransitions.refreshAfterAck(task.getChangeId()).getStatus())
                    .isEqualTo(PublishStatus.PUBLISHING.name());
            return null;
        }).when(redisRepository).dispatch(any());

        DdcPublishTaskBO completed = dispatcher.dispatch(task.getChangeId());

        assertThat(completed.getStatus()).isEqualTo(PublishStatus.SUCCESS.name());
        assertThat(configItemRepository.findByPublicId(task.getConfigId()))
                .get()
                .extracting(DdcConfigItemBO::getPublishedVersion)
                .isEqualTo(2L);
    }

    @Test
    void largeContentIsDeferredInRedisNotificationButRetainedInCommand() {
        String largeYaml = "feature:\n  value: "
                + "x".repeat(128)
                + "\n";
        DdcPublishTaskBO task = savePreparedPublish(
                "large-content",
                largeYaml
        );

        DdcPublishTaskBO result = dispatcher.dispatch(task.getChangeId());

        assertThat(result.getStatus()).isEqualTo(PublishStatus.PUBLISHING.name());
        ArgumentCaptor<DdcAtomicPublishCommand> command =
                ArgumentCaptor.forClass(DdcAtomicPublishCommand.class);
        verify(redisRepository).dispatch(command.capture());
        assertThat(command.getValue().content()).isEqualTo(largeYaml);
        assertThat(command.getValue().message().getContent()).isNull();
    }

    private DdcPublishTaskBO savePreparedPublish(String label) {
        return savePreparedPublish(label, NEW_YAML);
    }

    private DdcPublishTaskBO savePreparedPublish(
            String label,
            String newYaml) {
        LocalDateTime createdAt = LocalDateTime.of(2026, 7, 26, 8, 0);
        String configId = SnowflakeIdGenerator.nextId();
        DdcConfigItemBO config = DdcConfigItemBO.builder().id(nextConfigId++).tenantId(401L).publicId(configId)
                .bizCode("default").appCode(label).env("dev").resourceName(DdcConfigService.DEFAULT_RESOURCE_NAME)
                .content(newYaml).format(DdcConfigService.YAML_FORMAT).currentVersion(2L).publishedVersion(1L)
                .enabled(true).deleted(false).createdAt(createdAt.minusDays(1)).updatedAt(createdAt).version(0L).build();
        configItems.put(configId, config);

        saveVersion(config, 1L, null, OLD_YAML, createdAt.minusDays(1));
        saveVersion(config, 2L, OLD_YAML, newYaml, createdAt);

        DdcPublishTaskBO task = new DdcPublishTaskBO();
        task.setChangeId(SnowflakeIdGenerator.nextId());
        task.setConfigId(config.getPublicId());
        task.setConfigItemId(config.getId());
        task.setBizCode(config.getBizCode());
        task.setAppCode(config.getAppCode());
        task.setEnv(config.getEnv());
        task.setNamespace(null);
        task.setResourceName(DdcConfigService.DEFAULT_RESOURCE_NAME);
        task.setTargetVersion(2L);
        task.setPublishMode(PublishMode.SYNC_ALL_ACK.name());
        task.setResourceChecksum(DdcChecksum.resource(
                DdcConfigService.DEFAULT_RESOURCE_NAME,
                DdcConfigService.YAML_FORMAT,
                newYaml
        ));
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
        task = taskRepository.insert(task);

        DdcPublishAckBO target = new DdcPublishAckBO();
        target.setChangeId(task.getChangeId());
        target.setInstanceId("instance-1");
        target.setLeaseId("lease-1");
        target.setResourceChecksum(task.getResourceChecksum());
        target.setBizCode(task.getBizCode());
        target.setAppCode(task.getAppCode());
        target.setEnv(task.getEnv());
        target.setNamespace(task.getNamespace());
        target.setResourceName(task.getResourceName());
        target.setTargetVersion(task.getTargetVersion());
        ackRepository.insert(target);
        return task;
    }

    private void saveVersion(DdcConfigItemBO config,
                             long versionNumber,
                             String oldContent,
                             String newContent,
                             LocalDateTime createdAt) {
        String publicId = SnowflakeIdGenerator.nextId();
        DdcConfigVersionBO version = DdcConfigVersionBO.builder().id(Long.parseLong(publicId))
                .publicId(publicId).configItemId(config.getId()).configId(config.getPublicId())
                .bizCode(config.getBizCode()).appCode(config.getAppCode()).env(config.getEnv()).namespace(null)
                .resourceName(config.getResourceName()).configRevision(versionNumber).oldContent(oldContent)
                .newContent(newContent).format(config.getFormat()).changeType("UPDATE").createdAt(createdAt).build();
        configVersions.put(versionKey(config.getPublicId(), versionNumber), version);
    }

    private String versionKey(String configId, Long revision) { return configId + ":" + revision; }

    @TestConfiguration
    static class Dependencies {

        @Bean
        DdcRedisRepository ddcRedisRepository() {
            return org.mockito.Mockito.mock(DdcRedisRepository.class);
        }
    }
}
