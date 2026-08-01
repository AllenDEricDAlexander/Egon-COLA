package top.egon.cola.component.gateway.admin.application.release;

import top.egon.cola.component.common.id.uuid.UuidV7;
import top.egon.cola.component.ddc.management.DdcManagementClient;
import top.egon.cola.component.ddc.management.client.DdcManagementClientException;
import top.egon.cola.component.ddc.management.client.DdcManagementErrorCode;
import top.egon.cola.component.ddc.management.model.DdcManagementConfig;
import top.egon.cola.component.ddc.management.model.DdcManagementConfigQuery;
import top.egon.cola.component.ddc.management.model.DdcManagementConfigUpsertRequest;
import top.egon.cola.component.ddc.management.model.DdcManagementPublishResult;
import top.egon.cola.component.ddc.management.model.DdcManagementPublishStatus;
import top.egon.cola.component.ddc.management.model.DdcManagementPublishTask;
import top.egon.cola.component.gateway.admin.rule.CompiledGatewayRelease;
import top.egon.cola.component.gateway.admin.rule.GatewayDdcPublicationCommand;
import top.egon.cola.component.gateway.admin.rule.GatewayDdcRulePublisher;
import top.egon.cola.component.gateway.admin.rule.GatewayRuleCanonicalizer;
import top.egon.cola.component.gateway.contract.rule.GatewayRuleChunkRef;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import static top.egon.cola.component.gateway.admin.application.release
        .GatewayReleasePublicationStore.PhaseType.ACTIVATION;
import static top.egon.cola.component.gateway.admin.application.release
        .GatewayReleasePublicationStore.PhaseType.CHUNK;
import static top.egon.cola.component.gateway.admin.application.release
        .GatewayReleasePublicationStore.PublicationStatus.FAILED;
import static top.egon.cola.component.gateway.admin.application.release
        .GatewayReleasePublicationStore.PublicationStatus.PARTIAL_SUCCESS;
import static top.egon.cola.component.gateway.admin.application.release
        .GatewayReleasePublicationStore.PublicationStatus.PLANNED;
import static top.egon.cola.component.gateway.admin.application.release
        .GatewayReleasePublicationStore.PublicationStatus.RESOLVED;
import static top.egon.cola.component.gateway.admin.application.release
        .GatewayReleasePublicationStore.PublicationStatus.SUBMITTED;
import static top.egon.cola.component.gateway.admin.application.release
        .GatewayReleasePublicationStore.PublicationStatus.SUCCESS;
import static top.egon.cola.component.gateway.admin.application.release
        .GatewayReleasePublicationStore.PublicationStatus.TIMEOUT;
import static top.egon.cola.component.gateway.admin.application.release
        .GatewayReleasePublicationStore.PublicationStatus.UNKNOWN;

public final class GatewayReleasePublicationCoordinator {

    private final GatewayReleasePublicationStore journal;

    private final GatewayReleaseStore releases;

    private final DdcManagementClient client;

    private final GatewayDdcRulePublisher publisher;

    private final String targetBizCode;

    private final String targetAppCode;

    private final Clock clock;

    private final Duration timeout;

    public GatewayReleasePublicationCoordinator(
            GatewayReleasePublicationStore journal,
            GatewayReleaseStore releases,
            DdcManagementClient client,
            GatewayDdcRulePublisher publisher,
            Clock clock,
            Duration timeout,
            String targetBizCode,
            String targetAppCode) {
        this.journal = Objects.requireNonNull(journal, "journal");
        this.releases = Objects.requireNonNull(releases, "releases");
        this.client = Objects.requireNonNull(client, "client");
        this.publisher = Objects.requireNonNull(publisher, "publisher");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.timeout = positive(timeout);
        this.targetBizCode = required(targetBizCode, "targetBizCode");
        this.targetAppCode = required(targetAppCode, "targetAppCode");
    }

    public PublicationOutcome resume(String releaseId, int attemptNo) {
        return execute(
                releaseId,
                attemptNo,
                releases.loadCompiled(releaseId),
                "gateway_release_reconciler"
        );
    }

    public PublicationOutcome execute(
            String releaseId,
            int attemptNo,
            CompiledGatewayRelease compiled,
            String actorId) {
        Objects.requireNonNull(compiled, "compiled");
        String operator = required(actorId, "actorId");
        Scope scope = scope(compiled);
        List<GatewayReleasePublicationStore.PublicationRecord> operations =
                initialize(releaseId, attemptNo, compiled);
        if (operations.stream().anyMatch(operation ->
                operation.status() != SUCCESS)) {
            publisher.ensureReadyTarget(
                    scope.bizCode(),
                    scope.env(),
                    scope.appCode()
            );
        }
        int successfulPhases = 0;
        DdcManagementPublishResult latestResult = null;
        for (GatewayReleasePublicationStore.PublicationRecord original
                : operations) {
            GatewayReleasePublicationStore.PublicationRecord operation =
                    current(releaseId, attemptNo, original.changeId());
            if (operation.status() == SUCCESS) {
                successfulPhases++;
                continue;
            }
            DdcManagementPublishResult result = execute(
                    scope,
                    operation,
                    operator
            );
            latestResult = result;
            GatewayReleasePublicationStore.PublicationStatus status =
                    status(result.status());
            recordResult(operation.changeId(), result, status);
            if (status != SUCCESS) {
                return new PublicationOutcome(
                        status,
                        operation.changeId(),
                        result,
                        successfulPhases > 0
                );
            }
            successfulPhases++;
        }
        GatewayReleasePublicationStore.PublicationRecord activation =
                journal.findAttempt(releaseId, attemptNo).getLast();
        DdcManagementPublishResult result = latestResult != null
                && activation.changeId().equals(latestResult.changeId())
                ? latestResult
                : publishedResult(activation);
        return new PublicationOutcome(
                SUCCESS,
                activation.changeId(),
                result,
                false
        );
    }

    private DdcManagementPublishResult execute(
            Scope scope,
            GatewayReleasePublicationStore.PublicationRecord operation,
            String operator) {
        GatewayReleasePublicationStore.PublicationRecord current = operation;
        if (current.status() == PLANNED) {
            long expectedVersion = resolve(scope, current, operator);
            current = withStatus(
                    current,
                    expectedVersion,
                    RESOLVED
            );
        }
        if (current.status() == RESOLVED) {
            journal.markSubmitted(current.changeId(), clock.instant());
            return publish(scope, current, operator);
        }
        if (current.status() == SUBMITTED) {
            return resumeSubmitted(scope, current, operator);
        }
        if (current.status() == FAILED
                || current.status() == PARTIAL_SUCCESS
                || current.status() == TIMEOUT
                || current.status() == UNKNOWN) {
            return retry(scope, current, operator);
        }
        throw new IllegalStateException(
                "publication phase cannot execute from " + current.status()
        );
    }

    private DdcManagementPublishResult publish(
            Scope scope,
            GatewayReleasePublicationStore.PublicationRecord operation,
            String operator) {
        try {
            return publisher.publish(command(scope, operation, operator));
        } catch (RuntimeException failure) {
            return recover(operation, failure);
        }
    }

    private DdcManagementPublishResult resumeSubmitted(
            Scope scope,
            GatewayReleasePublicationStore.PublicationRecord operation,
            String operator) {
        try {
            return result(client.getPublishTask(operation.changeId()));
        } catch (DdcManagementClientException exception) {
            if (exception.code()
                    == DdcManagementErrorCode.PUBLISH_TASK_NOT_FOUND
                    .getCode()) {
                return publish(scope, operation, operator);
            }
            return unknown(operation, exception);
        } catch (RuntimeException failure) {
            return unknown(operation, failure);
        }
    }

    private DdcManagementPublishResult retry(
            Scope scope,
            GatewayReleasePublicationStore.PublicationRecord operation,
            String operator) {
        try {
            DdcManagementPublishTask task =
                    client.getPublishTask(operation.changeId());
            if (task.status() == DdcManagementPublishStatus.PENDING
                    || task.status()
                    == DdcManagementPublishStatus.PUBLISHING
                    || task.status() == DdcManagementPublishStatus.SUCCESS) {
                return result(task);
            }
            return client.retry(operation.changeId());
        } catch (DdcManagementClientException exception) {
            if (exception.code()
                    == DdcManagementErrorCode.PUBLISH_TASK_NOT_FOUND
                    .getCode()) {
                return publish(scope, operation, operator);
            }
            return unknown(operation, exception);
        } catch (RuntimeException failure) {
            return recover(operation, failure);
        }
    }

    private DdcManagementPublishResult recover(
            GatewayReleasePublicationStore.PublicationRecord operation,
            RuntimeException publishFailure) {
        try {
            return result(client.getPublishTask(operation.changeId()));
        } catch (RuntimeException queryFailure) {
            publishFailure.addSuppressed(queryFailure);
            return unknown(operation, publishFailure);
        }
    }

    private long resolve(
            Scope scope,
            GatewayReleasePublicationStore.PublicationRecord operation,
            String operator) {
        DdcManagementConfigQuery query = new DdcManagementConfigQuery(
                scope.bizCode(),
                scope.env(),
                scope.appCode(),
                operation.configKey()
        );
        DdcManagementConfig config = client.findConfig(query).orElse(null);
        if (config == null) {
            config = create(scope, operation, operator, query);
        }
        validateConfig(config, operation.configKey());
        journal.resolveVersion(
                operation.changeId(),
                config.version(),
                clock.instant()
        );
        return config.version();
    }

    private DdcManagementConfig create(
            Scope scope,
            GatewayReleasePublicationStore.PublicationRecord operation,
            String operator,
            DdcManagementConfigQuery query) {
        try {
            client.upsert(new DdcManagementConfigUpsertRequest(
                    scope.bizCode(),
                    scope.env(),
                    scope.appCode(),
                    operation.configKey(),
                    operation.contentValue(),
                    operation.phaseType() == ACTIVATION ? "JSON" : "STRING",
                    "Gateway release " + operation.releaseId(),
                    0L,
                    operator
            ));
        } catch (RuntimeException failure) {
            DdcManagementConfig recovered = client.findConfig(query)
                    .filter(config -> Objects.equals(
                            config.configValue(),
                            operation.contentValue()
                    ))
                    .orElseThrow(() -> failure);
            validateConfig(recovered, operation.configKey());
            return recovered;
        }
        return client.findConfig(query).orElseThrow(() ->
                new IllegalStateException(
                        "created DDC config cannot be queried"
                ));
    }

    private void validateConfig(
            DdcManagementConfig config,
            String configKey) {
        if (config.version() == null || config.version() < 0) {
            throw new IllegalStateException(
                    "DDC config has no usable version: " + configKey
            );
        }
        if (config.deleted()) {
            throw new IllegalStateException(
                    "DDC config is deleted: " + configKey
            );
        }
        if (!config.enabled()) {
            throw new IllegalStateException(
                    "DDC config is disabled: " + configKey
            );
        }
    }

    private GatewayDdcPublicationCommand command(
            Scope scope,
            GatewayReleasePublicationStore.PublicationRecord operation,
            String operator) {
        return new GatewayDdcPublicationCommand(
                scope.bizCode(),
                scope.env(),
                scope.appCode(),
                operation.configKey(),
                operation.contentValue(),
                operation.expectedVersion(),
                operation.changeId(),
                operator,
                timeout
        );
    }

    private List<GatewayReleasePublicationStore.PublicationRecord> initialize(
            String releaseId,
            int attemptNo,
            CompiledGatewayRelease compiled) {
        List<GatewayReleasePublicationStore.PublicationRecord> existing =
                journal.findAttempt(releaseId, attemptNo);
        List<Artifact> artifacts = artifacts(compiled);
        if (!existing.isEmpty()) {
            validateExisting(existing, artifacts);
            return existing;
        }
        Instant now = clock.instant();
        List<GatewayReleasePublicationStore.PublicationRecord> created =
                new ArrayList<>();
        for (int index = 0; index < artifacts.size(); index++) {
            Artifact artifact = artifacts.get(index);
            created.add(new GatewayReleasePublicationStore.PublicationRecord(
                    required(releaseId, "releaseId"),
                    attemptNo,
                    index,
                    artifact.phaseType(),
                    artifact.configKey(),
                    artifact.value(),
                    checksum(artifact.value()),
                    null,
                    UuidV7.simpleString(),
                    null,
                    PLANNED,
                    null,
                    null,
                    now,
                    now
            ));
        }
        journal.insertAll(created);
        return List.copyOf(created);
    }

    private List<Artifact> artifacts(CompiledGatewayRelease compiled) {
        List<Artifact> artifacts = new ArrayList<>();
        compiled.activation().chunks().stream()
                .sorted(Comparator.comparingInt(GatewayRuleChunkRef::index))
                .forEach(chunk -> artifacts.add(new Artifact(
                        CHUNK,
                        chunk.configKey(),
                        required(
                                compiled.chunkValues().get(chunk.configKey()),
                                "chunk value"
                        )
                )));
        artifacts.add(new Artifact(
                ACTIVATION,
                GatewayDdcRulePublisher.ACTIVE_CONFIG_KEY,
                compiled.activationJson()
        ));
        return List.copyOf(artifacts);
    }

    private void validateExisting(
            List<GatewayReleasePublicationStore.PublicationRecord> existing,
            List<Artifact> artifacts) {
        if (existing.size() != artifacts.size()) {
            throw new IllegalStateException(
                    "publication journal does not match compiled release"
            );
        }
        for (int index = 0; index < artifacts.size(); index++) {
            Artifact artifact = artifacts.get(index);
            GatewayReleasePublicationStore.PublicationRecord operation =
                    existing.get(index);
            if (operation.phaseOrder() != index
                    || operation.phaseType() != artifact.phaseType()
                    || !operation.configKey().equals(artifact.configKey())
                    || !operation.contentSha256().equals(
                    checksum(artifact.value()))) {
                throw new IllegalStateException(
                        "publication journal content conflict"
                );
            }
        }
    }

    private GatewayReleasePublicationStore.PublicationRecord current(
            String releaseId,
            int attemptNo,
            String changeId) {
        return journal.findAttempt(releaseId, attemptNo).stream()
                .filter(operation -> operation.changeId().equals(changeId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "publication operation disappeared"
                ));
    }

    private GatewayReleasePublicationStore.PublicationRecord withStatus(
            GatewayReleasePublicationStore.PublicationRecord operation,
            long expectedVersion,
            GatewayReleasePublicationStore.PublicationStatus status) {
        return new GatewayReleasePublicationStore.PublicationRecord(
                operation.releaseId(),
                operation.attemptNo(),
                operation.phaseOrder(),
                operation.phaseType(),
                operation.configKey(),
                operation.contentValue(),
                operation.contentSha256(),
                expectedVersion,
                operation.changeId(),
                operation.ddcTargetVersion(),
                status,
                operation.errorCode(),
                operation.errorMessage(),
                operation.createdAt(),
                clock.instant()
        );
    }

    private void recordResult(
            String changeId,
            DdcManagementPublishResult result,
            GatewayReleasePublicationStore.PublicationStatus status) {
        journal.markResult(
                changeId,
                result.targetVersion(),
                status,
                status == SUCCESS ? null : "DDC_PUBLISH_" + status,
                result.errorMessage(),
                clock.instant()
        );
    }

    private DdcManagementPublishResult result(
            GatewayReleasePublicationStore.PublicationRecord operation) {
        return new DdcManagementPublishResult(
                operation.changeId(),
                DdcManagementPublishStatus.SUCCESS,
                operation.ddcTargetVersion(),
                operation.contentSha256(),
                0,
                List.of(),
                null,
                operation.createdAt(),
                operation.updatedAt(),
                operation.updatedAt()
        );
    }

    private DdcManagementPublishResult publishedResult(
            GatewayReleasePublicationStore.PublicationRecord operation) {
        try {
            return result(client.getPublishTask(operation.changeId()));
        } catch (RuntimeException unavailable) {
            return result(operation);
        }
    }

    private DdcManagementPublishResult result(DdcManagementPublishTask task) {
        return new DdcManagementPublishResult(
                task.changeId(),
                task.status(),
                task.targetVersion(),
                task.contentChecksum(),
                task.targetCount(),
                task.targets(),
                task.errorMessage(),
                task.createdAt(),
                task.dispatchedAt(),
                task.completedAt()
        );
    }

    private DdcManagementPublishResult unknown(
            GatewayReleasePublicationStore.PublicationRecord operation,
            RuntimeException failure) {
        return new DdcManagementPublishResult(
                operation.changeId(),
                DdcManagementPublishStatus.UNKNOWN,
                operation.ddcTargetVersion(),
                operation.contentSha256(),
                0,
                List.of(),
                failure.getMessage(),
                operation.createdAt(),
                null,
                clock.instant()
        );
    }

    private GatewayReleasePublicationStore.PublicationStatus status(
            DdcManagementPublishStatus status) {
        return switch (status) {
            case SUCCESS -> SUCCESS;
            case PARTIAL_SUCCESS -> PARTIAL_SUCCESS;
            case FAILED -> FAILED;
            case TIMEOUT -> TIMEOUT;
            case PENDING, PUBLISHING, UNKNOWN -> UNKNOWN;
        };
    }

    private Scope scope(CompiledGatewayRelease compiled) {
        return new Scope(
                targetBizCode,
                compiled.snapshot().content().env(),
                targetAppCode
        );
    }

    private String checksum(String value) {
        return GatewayRuleCanonicalizer.sha256(
                value.getBytes(StandardCharsets.UTF_8)
        );
    }

    private Duration positive(Duration value) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException("timeout must be positive");
        }
        return value;
    }

    private String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }

    public record PublicationOutcome(
            GatewayReleasePublicationStore.PublicationStatus status,
            String changeId,
            DdcManagementPublishResult result,
            boolean partialApplied
    ) {

        public boolean successful() {
            return status == SUCCESS;
        }
    }

    private record Scope(String bizCode, String env, String appCode) {
    }

    private record Artifact(
            GatewayReleasePublicationStore.PhaseType phaseType,
            String configKey,
            String value
    ) {
    }
}
