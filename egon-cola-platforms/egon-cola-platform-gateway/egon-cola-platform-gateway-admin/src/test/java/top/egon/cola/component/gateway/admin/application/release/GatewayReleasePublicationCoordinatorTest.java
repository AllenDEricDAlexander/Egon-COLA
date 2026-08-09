package top.egon.cola.component.gateway.admin.application.release;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import top.egon.cola.component.ddc.api.client.DdcManagementClient;
import top.egon.cola.component.ddc.error.management.DdcManagementClientException;
import top.egon.cola.component.ddc.error.management.DdcManagementErrorCode;
import top.egon.cola.component.ddc.model.management.DdcManagementConfig;
import top.egon.cola.component.ddc.model.management.DdcManagementConfigClientInstance;
import top.egon.cola.component.ddc.model.management.DdcManagementConfigDeleteRequest;
import top.egon.cola.component.ddc.model.management.DdcManagementConfigQuery;
import top.egon.cola.component.ddc.model.management.DdcManagementConfigUpsertRequest;
import top.egon.cola.component.ddc.model.management.DdcManagementInstanceQuery;
import top.egon.cola.component.ddc.model.management.DdcManagementPublishRequest;
import top.egon.cola.component.ddc.model.management.DdcManagementPublishResult;
import top.egon.cola.component.ddc.model.management.DdcManagementPublishStatus;
import top.egon.cola.component.ddc.model.management.DdcManagementPublishTask;
import top.egon.cola.component.ddc.model.management.DdcManagementScopeBinding;
import top.egon.cola.component.ddc.model.management.DdcManagementScopeQuery;
import top.egon.cola.component.ddc.model.management.DdcManagementServiceCatalog;
import top.egon.cola.component.ddc.model.management.DdcManagementServiceQuery;
import top.egon.cola.component.ddc.model.management.DdcManagementServiceSnapshot;
import top.egon.cola.component.gateway.admin.rule.CompiledGatewayRelease;
import top.egon.cola.component.gateway.admin.rule.GatewayDdcRulePublisher;
import top.egon.cola.component.gateway.admin.rule.GatewayDdcYamlDocument;
import top.egon.cola.component.gateway.contract.rule.GatewayRuleActivation;
import top.egon.cola.component.gateway.contract.rule.GatewayRuleActivationMode;
import top.egon.cola.component.gateway.contract.rule.GatewayRuleChunkRef;
import top.egon.cola.component.gateway.contract.rule.GatewayRuleContent;
import top.egon.cola.component.gateway.contract.rule.GatewayRuleSnapshot;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static top.egon.cola.component.gateway.admin.application.release
        .GatewayReleasePublicationStore.PublicationStatus.FAILED;
import static top.egon.cola.component.gateway.admin.application.release
        .GatewayReleasePublicationStore.PublicationStatus.PLANNED;
import static top.egon.cola.component.gateway.admin.application.release
        .GatewayReleasePublicationStore.PublicationStatus.SUCCESS;
import static top.egon.cola.component.gateway.admin.application.release
        .GatewayReleasePublicationStore.PublicationStatus.TIMEOUT;

class GatewayReleasePublicationCoordinatorTest {

    private static final Instant NOW =
            Instant.parse("2026-07-26T08:00:00Z");

    @Test
    void persistsAllPhasesAndStopsBeforeActivationOnChunkFailure() {
        InMemoryPublicationStore journal = new InMemoryPublicationStore();
        RecordingClient client = new RecordingClient(journal);
        client.statuses.put("gateway.rules.chunk.release-1.0", SUCCESS);
        client.statuses.put("gateway.rules.chunk.release-1.1", FAILED);
        GatewayReleasePublicationCoordinator coordinator = coordinator(
                journal,
                client
        );

        GatewayReleasePublicationCoordinator.PublicationOutcome outcome =
                coordinator.execute(
                        "release-1",
                        1,
                        compiledWithChunks(2),
                        "admin"
                );

        assertThat(outcome.status()).isEqualTo(FAILED);
        assertThat(client.publishedKeys).containsExactly(
                "gateway.rules.chunk.release-1.0",
                "gateway.rules.chunk.release-1.1"
        );
        assertThat(client.upsertRequests).singleElement()
                .satisfies(request -> {
                    assertThat(request.expectedVersion()).isZero();
                    assertThat(yaml().leafValue(
                            request.content(),
                            "gateway.rules.chunk.release-1.0"
                    )).contains("chunk-0");
                });
        assertThat(client.upsertRequests).allSatisfy(request -> {
            assertThat(request.bizCode()).isEqualTo("infra");
            assertThat(request.env()).isEqualTo("test");
            assertThat(request.appCode()).isEqualTo("ge");
        });
        assertThat(client.publishRequests).allSatisfy(request -> {
            assertThat(request.bizCode()).isEqualTo("infra");
            assertThat(request.env()).isEqualTo("test");
            assertThat(request.appCode()).isEqualTo("ge");
        });
        assertThat(client.publishRequests)
                .extracting(DdcManagementPublishRequest::expectedVersion)
                .containsExactly(1L, 2L);
        assertThat(yaml().leafValue(
                client.publishRequests.get(1).content(),
                "gateway.rules.chunk.release-1.0"
        )).contains("chunk-0");
        assertThat(yaml().leafValue(
                client.publishRequests.get(1).content(),
                "gateway.rules.chunk.release-1.1"
        )).contains("chunk-1");
        assertThat(journal.findAttempt("release-1", 1))
                .hasSize(3)
                .extracting(
                        GatewayReleasePublicationStore.PublicationRecord
                                ::status
                ).containsExactly(SUCCESS, FAILED, PLANNED);
        assertThat(journal.insertCount).isEqualTo(1);
        assertThat(journal.findAttempt("release-1", 1))
                .allSatisfy(operation -> assertUuidV7(
                        operation.changeId()
                ));
    }

    @Test
    void reconcilesLostPublishResponseByTheSameChangeId() {
        InMemoryPublicationStore journal = new InMemoryPublicationStore();
        RecordingClient client = new RecordingClient(journal);
        client.loseResponse = true;
        GatewayReleasePublicationCoordinator coordinator = coordinator(
                journal,
                client
        );

        GatewayReleasePublicationCoordinator.PublicationOutcome outcome =
                coordinator.execute(
                        "release-inline",
                        1,
                        compiledInline(),
                        "admin"
                );

        assertThat(outcome.successful()).isTrue();
        assertThat(client.publishRequests).hasSize(1);
        assertThat(client.taskQueries).containsExactly(
                client.publishRequests.getFirst().changeId()
        );
        assertThat(journal.findAttempt("release-inline", 1))
                .singleElement()
                .extracting(
                        GatewayReleasePublicationStore.PublicationRecord
                                ::status
                ).isEqualTo(SUCCESS);
    }

    @Test
    void restartReusesJournalIdentityVersionAndRetriesTheSameTask() {
        InMemoryPublicationStore journal = new InMemoryPublicationStore();
        RecordingClient client = new RecordingClient(journal);
        client.statuses.put("gateway.rules.active", TIMEOUT);
        CompiledGatewayRelease compiled = compiledInline();

        GatewayReleasePublicationCoordinator.PublicationOutcome first =
                coordinator(journal, client).execute(
                        "release-inline",
                        1,
                        compiled,
                        "admin"
                );
        GatewayReleasePublicationStore.PublicationRecord persisted =
                journal.findAttempt("release-inline", 1).getFirst();
        client.statuses.put("gateway.rules.active", SUCCESS);

        GatewayReleasePublicationCoordinator.PublicationOutcome resumed =
                coordinator(journal, client).execute(
                        "release-inline",
                        1,
                        compiled,
                        "admin"
                );

        assertThat(first.status()).isEqualTo(TIMEOUT);
        assertThat(resumed.successful()).isTrue();
        assertThat(journal.insertCount).isEqualTo(1);
        assertThat(journal.findAttempt("release-inline", 1).getFirst())
                .satisfies(operation -> {
                    assertThat(operation.changeId())
                            .isEqualTo(persisted.changeId());
                    assertThat(operation.expectedVersion())
                            .isEqualTo(persisted.expectedVersion());
                });
        assertThat(client.retryChangeIds)
                .containsExactly(persisted.changeId());
    }

    @Test
    void taskMissingRecoveryRebasesTheLeafOnTheLatestYamlDocument() {
        InMemoryPublicationStore journal = new InMemoryPublicationStore();
        RecordingClient client = new RecordingClient(journal);
        client.failBeforeTaskAt = 0;
        CompiledGatewayRelease compiled = compiledInline();

        GatewayReleasePublicationCoordinator.PublicationOutcome first =
                coordinator(journal, client).execute(
                        "release-inline",
                        1,
                        compiled,
                        "admin"
                );
        String changeId = journal.findAttempt("release-inline", 1)
                .getFirst()
                .changeId();
        client.configs.put(
                "infra/test/ge",
                new DdcManagementConfig(
                        "infra",
                        "test",
                        "ge",
                        "application.yml",
                        "feature:\n  external: true\n"
                                + "gateway:\n  rules:\n    active: old\n",
                        "YAML",
                        2L,
                        true,
                        false,
                        NOW
                )
        );

        GatewayReleasePublicationCoordinator.PublicationOutcome resumed =
                coordinator(journal, client).execute(
                        "release-inline",
                        1,
                        compiled,
                        "admin"
                );

        assertThat(first.status())
                .isEqualTo(GatewayReleasePublicationStore
                        .PublicationStatus.UNKNOWN);
        assertThat(resumed.successful()).isTrue();
        assertThat(client.publishRequests).hasSize(2)
                .allSatisfy(request -> assertThat(request.changeId())
                        .isEqualTo(changeId));
        DdcManagementPublishRequest recovered =
                client.publishRequests.getLast();
        assertThat(recovered.expectedVersion()).isEqualTo(2L);
        assertThat(recovered.content()).contains("external: true");
        assertThat(yaml().leafValue(
                recovered.content(),
                GatewayDdcYamlDocument.ACTIVE_CONFIG_KEY
        )).contains(compiled.activationJson());
    }

    @Test
    void resumeCompletesReleaseAfterJournalSuccessWithoutAReadyTarget() {
        InMemoryPublicationStore journal = new InMemoryPublicationStore();
        RecordingClient client = new RecordingClient(journal);
        CompiledGatewayRelease compiled = compiledInline();
        coordinator(journal, client).execute(
                "release-inline",
                1,
                compiled,
                "admin"
        );
        GatewayReleaseStore releases = mock(GatewayReleaseStore.class);
        when(releases.loadCompiled("release-inline")).thenReturn(compiled);
        client.ready = false;
        GatewayReleasePublicationCoordinator resumedCoordinator =
                new GatewayReleasePublicationCoordinator(
                        journal,
                        releases,
                        client,
                        new GatewayDdcRulePublisher(client),
                        Clock.fixed(NOW, ZoneOffset.UTC),
                        Duration.ofSeconds(30),
                        "infra",
                        "ge"
                );

        GatewayReleasePublicationCoordinator.PublicationOutcome outcome =
                resumedCoordinator.resume("release-inline", 1);

        assertThat(outcome.successful()).isTrue();
        assertThat(client.publishRequests).hasSize(1);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3})
    void crashAtEveryPhaseReusesIdentityAndSkipsCompletedPhases(
            int crashPhase) {
        InMemoryPublicationStore journal = new InMemoryPublicationStore();
        RecordingClient client = new RecordingClient(journal);
        client.failBeforeTaskAt = crashPhase;
        CompiledGatewayRelease compiled = compiledWithChunks(3);

        GatewayReleasePublicationCoordinator.PublicationOutcome first =
                coordinator(journal, client).execute(
                        "release-1",
                        1,
                        compiled,
                        "admin"
                );
        List<String> identities = journal.findAttempt("release-1", 1)
                .stream()
                .map(GatewayReleasePublicationStore.PublicationRecord
                        ::changeId)
                .toList();
        GatewayReleasePublicationCoordinator.PublicationOutcome resumed =
                coordinator(journal, client).execute(
                        "release-1",
                        1,
                        compiled,
                        "admin"
                );

        assertThat(first.status())
                .isEqualTo(GatewayReleasePublicationStore
                        .PublicationStatus.UNKNOWN);
        assertThat(resumed.successful()).isTrue();
        assertThat(journal.insertCount).isEqualTo(1);
        assertThat(journal.findAttempt("release-1", 1))
                .extracting(GatewayReleasePublicationStore.PublicationRecord
                        ::changeId)
                .containsExactlyElementsOf(identities);
        assertThat(client.publishRequests)
                .filteredOn(request -> request.changeId().equals(
                        identities.get(crashPhase)
                )).hasSize(2);
        List<String> retriedDocuments = client.publishRequests.stream()
                .filter(request -> request.changeId().equals(
                        identities.get(crashPhase)
                ))
                .map(DdcManagementPublishRequest::content)
                .toList();
        assertThat(retriedDocuments)
                .allMatch(retriedDocuments.getFirst()::equals);
        for (int phase = 0; phase < identities.size(); phase++) {
            if (phase != crashPhase) {
                String changeId = identities.get(phase);
                assertThat(client.publishRequests)
                        .filteredOn(request -> request.changeId().equals(
                                changeId
                        )).hasSize(1);
            }
        }
    }

    private GatewayReleasePublicationCoordinator coordinator(
            InMemoryPublicationStore journal,
            RecordingClient client) {
        return new GatewayReleasePublicationCoordinator(
                journal,
                mock(GatewayReleaseStore.class),
                client,
                new GatewayDdcRulePublisher(client),
                Clock.fixed(NOW, ZoneOffset.UTC),
                Duration.ofSeconds(30),
                "infra",
                "ge"
        );
    }

    private GatewayDdcYamlDocument yaml() {
        return new GatewayDdcYamlDocument();
    }

    private CompiledGatewayRelease compiledWithChunks(int chunkCount) {
        GatewayRuleContent content = content();
        GatewayRuleSnapshot snapshot = snapshot("release-1", content);
        List<GatewayRuleChunkRef> chunks = new ArrayList<>();
        Map<String, String> chunkValues = new LinkedHashMap<>();
        for (int index = 0; index < chunkCount; index++) {
            String configKey = "gateway.rules.chunk.release-1." + index;
            chunks.add(new GatewayRuleChunkRef(
                    configKey,
                    index,
                    7,
                    Integer.toString(index + 1).repeat(64)
            ));
            chunkValues.put(configKey, "chunk-" + index);
        }
        GatewayRuleActivation activation = new GatewayRuleActivation(
                "v1",
                "release-1",
                GatewayRuleActivationMode.CHUNKED,
                "v1",
                chunkCount * 7,
                "a".repeat(64),
                "b".repeat(64),
                null,
                chunks
        );
        return new CompiledGatewayRelease(
                snapshot,
                "{}",
                activation,
                "{\"releaseId\":\"release-1\"}",
                chunkValues
        );
    }

    private CompiledGatewayRelease compiledInline() {
        GatewayRuleContent content = content();
        GatewayRuleSnapshot snapshot = snapshot("release-inline", content);
        GatewayRuleActivation activation = new GatewayRuleActivation(
                "v1",
                "release-inline",
                GatewayRuleActivationMode.INLINE,
                "v1",
                2,
                "a".repeat(64),
                "b".repeat(64),
                "{}",
                List.of()
        );
        return new CompiledGatewayRelease(
                snapshot,
                "{}",
                activation,
                "{\"releaseId\":\"release-inline\"}",
                Map.of()
        );
    }

    private GatewayRuleContent content() {
        return new GatewayRuleContent(
                "group-1",
                "default",
                "test",
                "default",
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
    }

    private GatewayRuleSnapshot snapshot(
            String releaseId,
            GatewayRuleContent content) {
        return new GatewayRuleSnapshot(
                "v1",
                releaseId,
                NOW,
                "a".repeat(64),
                "b".repeat(64),
                content
        );
    }

    private void assertUuidV7(String value) {
        String canonical = value.length() == 32
                ? value.substring(0, 8)
                + "-" + value.substring(8, 12)
                + "-" + value.substring(12, 16)
                + "-" + value.substring(16, 20)
                + "-" + value.substring(20)
                : value;
        assertThat(UUID.fromString(canonical).version()).isEqualTo(7);
    }

    private static final class InMemoryPublicationStore
            implements GatewayReleasePublicationStore {

        private final Map<String, PublicationRecord> records =
                new LinkedHashMap<>();

        private int insertCount;

        @Override
        public void insertAll(List<PublicationRecord> operations) {
            insertCount++;
            operations.forEach(operation -> records.put(
                    operation.changeId(),
                    operation
            ));
        }

        @Override
        public List<PublicationRecord> findAttempt(
                String releaseId,
                int attemptNo) {
            return records.values().stream()
                    .filter(record -> record.releaseId().equals(releaseId))
                    .filter(record -> record.attemptNo() == attemptNo)
                    .sorted(Comparator.comparingInt(
                            PublicationRecord::phaseOrder
                    ))
                    .toList();
        }

        @Override
        public Optional<PublicationRecord> nextIncomplete(
                String releaseId,
                int attemptNo) {
            return findAttempt(releaseId, attemptNo).stream()
                    .filter(record -> record.status() != SUCCESS)
                    .findFirst();
        }

        @Override
        public List<ChunkCleanupCandidate> findChunkCleanupCandidates(
                Instant successorActivatedBefore) {
            return List.of();
        }

        @Override
        public void resolveDocument(
                String changeId,
                long expectedVersion,
                String documentContent,
                Instant now) {
            update(changeId, record -> copy(
                    record,
                    documentContent,
                    expectedVersion,
                    record.ddcTargetVersion(),
                    PublicationStatus.RESOLVED,
                    null,
                    null,
                    now
            ));
        }

        @Override
        public void markSubmitted(String changeId, Instant now) {
            update(changeId, record -> copy(
                    record,
                    record.contentValue(),
                    record.expectedVersion(),
                    record.ddcTargetVersion(),
                    PublicationStatus.SUBMITTED,
                    null,
                    null,
                    now
            ));
        }

        @Override
        public void markResult(
                String changeId,
                Long targetVersion,
                PublicationStatus status,
                String errorCode,
                String errorMessage,
                Instant now) {
            update(changeId, record -> copy(
                    record,
                    record.contentValue(),
                    record.expectedVersion(),
                    targetVersion,
                    status,
                    errorCode,
                    errorMessage,
                    now
            ));
        }

        @Override
        public void markChunkCleaned(String changeId, Instant now) {
            throw new UnsupportedOperationException();
        }

        private void update(
                String changeId,
                java.util.function.UnaryOperator<PublicationRecord> change) {
            records.compute(changeId, (key, value) -> change.apply(value));
        }

        private PublicationRecord copy(
                PublicationRecord source,
                String contentValue,
                Long expectedVersion,
                Long targetVersion,
                PublicationStatus status,
                String errorCode,
                String errorMessage,
                Instant updatedAt) {
            return new PublicationRecord(
                    source.releaseId(),
                    source.attemptNo(),
                    source.phaseOrder(),
                    source.phaseType(),
                    source.configKey(),
                    contentValue,
                    source.contentSha256(),
                    expectedVersion,
                    source.changeId(),
                    targetVersion,
                    status,
                    errorCode,
                    errorMessage,
                    source.createdAt(),
                    updatedAt
            );
        }
    }

    private static final class RecordingClient
            implements DdcManagementClient {

        private final InMemoryPublicationStore journal;

        private final Map<String, DdcManagementConfig> configs =
                new LinkedHashMap<>();

        private final Map<String, GatewayReleasePublicationStore
                .PublicationStatus> statuses = new LinkedHashMap<>();

        private final Map<String, DdcManagementPublishTask> tasks =
                new LinkedHashMap<>();

        private final List<String> publishedKeys = new ArrayList<>();

        private final List<DdcManagementPublishRequest> publishRequests =
                new ArrayList<>();

        private final List<DdcManagementConfigUpsertRequest> upsertRequests =
                new ArrayList<>();

        private final List<String> taskQueries = new ArrayList<>();

        private final List<String> retryChangeIds = new ArrayList<>();

        private boolean loseResponse;

        private boolean ready = true;

        private int failBeforeTaskAt = -1;

        private int publishInvocation;

        private RecordingClient(InMemoryPublicationStore journal) {
            this.journal = journal;
        }

        @Override
        public Optional<DdcManagementConfig> findConfig(
                DdcManagementConfigQuery query) {
            return Optional.ofNullable(configs.get(scope(
                    query.bizCode(),
                    query.env(),
                    query.appCode()
            )));
        }

        @Override
        public DdcManagementConfig upsert(
                DdcManagementConfigUpsertRequest request) {
            upsertRequests.add(request);
            DdcManagementConfig config = new DdcManagementConfig(
                    request.bizCode(),
                    request.env(),
                    request.appCode(),
                    "application.yml",
                    request.content(),
                    "YAML",
                    1L,
                    true,
                    false,
                    NOW
            );
            configs.put(scope(
                    request.bizCode(),
                    request.env(),
                    request.appCode()
            ), config);
            return config;
        }

        @Override
        public void delete(DdcManagementConfigDeleteRequest request) {
        }

        @Override
        public DdcManagementPublishResult publish(
                DdcManagementPublishRequest request) {
            GatewayReleasePublicationStore.PublicationRecord operation =
                    journal.records.get(request.changeId());
            assertThat(operation).isNotNull();
            publishedKeys.add(operation.configKey());
            publishRequests.add(request);
            if (publishInvocation++ == failBeforeTaskAt) {
                throw new IllegalStateException("request not sent");
            }
            DdcManagementPublishStatus status = ddcStatus(
                    statuses.getOrDefault(operation.configKey(), SUCCESS)
            );
            configs.put(scope(
                    request.bizCode(),
                    request.env(),
                    request.appCode()
            ), new DdcManagementConfig(
                    request.bizCode(),
                    request.env(),
                    request.appCode(),
                    "application.yml",
                    request.content(),
                    "YAML",
                    request.expectedVersion() + 1,
                    true,
                    false,
                    NOW
            ));
            DdcManagementPublishResult result = result(request, status);
            tasks.put(request.changeId(), task(result));
            if (loseResponse) {
                loseResponse = false;
                throw new IllegalStateException("response lost");
            }
            return result;
        }

        @Override
        public DdcManagementPublishTask getPublishTask(String changeId) {
            taskQueries.add(changeId);
            DdcManagementPublishTask task = tasks.get(changeId);
            if (task == null) {
                throw new DdcManagementClientException(
                        DdcManagementErrorCode.PUBLISH_TASK_NOT_FOUND
                                .getCode(),
                        DdcManagementErrorCode.PUBLISH_TASK_NOT_FOUND
                                .getStatus(),
                        "task not found"
                );
            }
            return task;
        }

        @Override
        public DdcManagementPublishResult retry(String changeId) {
            retryChangeIds.add(changeId);
            DdcManagementPublishTask current = tasks.get(changeId);
            GatewayReleasePublicationStore.PublicationRecord operation =
                    journal.records.get(changeId);
            DdcManagementPublishStatus status = ddcStatus(
                    statuses.get(operation.configKey())
            );
            DdcManagementPublishResult result = new DdcManagementPublishResult(
                    changeId,
                    status,
                    current.targetVersion(),
                    current.resourceChecksum(),
                    current.targetCount(),
                    current.targets(),
                    null,
                    current.createdAt(),
                    NOW,
                    NOW
            );
            tasks.put(changeId, task(result));
            return result;
        }

        @Override
        public List<DdcManagementConfigClientInstance> getConfigClients(
                DdcManagementInstanceQuery query) {
            if (!ready) {
                return List.of();
            }
            return List.of(new DdcManagementConfigClientInstance(
                    query.bizCode(),
                    query.env(),
                    query.appCode(),
                    "engine-1",
                    "lease-1",
                    "127.0.0.1",
                    18080,
                    "CONFIG_CLIENT",
                    "ONLINE",
                    NOW,
                    NOW,
                    Instant.parse("2099-01-01T00:00:00Z"),
                    Map.of()
            ));
        }

        @Override
        public List<DdcManagementScopeBinding> getScopeBindings(
                DdcManagementScopeQuery query) {
            return List.of();
        }

        @Override
        public DdcManagementServiceCatalog getServiceKeys(
                DdcManagementServiceQuery query) {
            return null;
        }

        @Override
        public DdcManagementServiceSnapshot getInstances(
                DdcManagementServiceQuery query) {
            return null;
        }

        private DdcManagementPublishResult result(
                DdcManagementPublishRequest request,
                DdcManagementPublishStatus status) {
            return new DdcManagementPublishResult(
                    request.changeId(),
                    status,
                    request.expectedVersion() + 1,
                    "checksum",
                    1,
                    List.of(),
                    status == DdcManagementPublishStatus.SUCCESS
                            ? null
                            : "publish failed",
                    NOW,
                    NOW,
                    NOW
            );
        }

        private DdcManagementPublishTask task(
                DdcManagementPublishResult result) {
            return new DdcManagementPublishTask(
                    result.changeId(),
                    result.status(),
                    result.targetVersion(),
                    result.resourceChecksum(),
                    result.targetCount(),
                    1,
                    0,
                    0,
                    0,
                    1,
                    result.targets(),
                    result.errorMessage(),
                    result.createdAt(),
                    result.dispatchedAt(),
                    result.completedAt()
            );
        }

        private DdcManagementPublishStatus ddcStatus(
                GatewayReleasePublicationStore.PublicationStatus status) {
            return DdcManagementPublishStatus.valueOf(status.name());
        }

        private String scope(String bizCode, String env, String appCode) {
            return bizCode + "/" + env + "/" + appCode;
        }
    }
}
