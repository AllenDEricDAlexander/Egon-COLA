package top.egon.cola.platform.rbac3.admin.runtime.application;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Commits authorization facts first, then projects them under a fail-closed fence.
 */
public final class AuthorizationMutationCoordinator {

    private final MutationStore mutationStore;
    private final AuthorizationFenceService fenceService;
    private final RuntimeProjector projector;
    private final TransactionExecutor transactionExecutor;
    private final MutationIdGenerator idGenerator;
    private final Clock clock;

    public AuthorizationMutationCoordinator(
            MutationStore mutationStore,
            AuthorizationFenceService fenceService,
            RuntimeProjector projector,
            TransactionExecutor transactionExecutor,
            MutationIdGenerator idGenerator,
            Clock clock
    ) {
        this.mutationStore = Objects.requireNonNull(mutationStore, "mutationStore");
        this.fenceService = Objects.requireNonNull(fenceService, "fenceService");
        this.projector = Objects.requireNonNull(projector, "projector");
        this.transactionExecutor = Objects.requireNonNull(
                transactionExecutor, "transactionExecutor");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public <T> MutationResult<T> execute(
            MutationScope scope,
            String subjectId,
            ExpectedVersions versions,
            Supplier<T> databaseMutation
    ) {
        Objects.requireNonNull(scope, "scope");
        Objects.requireNonNull(versions, "versions");
        Objects.requireNonNull(databaseMutation, "databaseMutation");
        String mutationId = idGenerator.next();
        MutationRecord record = new MutationRecord(
                mutationId, scope, subjectId, versions, clock.instant());

        @SuppressWarnings("unchecked")
        T value = (T) transactionExecutor.execute(() -> {
            mutationStore.prepare(record);
            T changed = databaseMutation.get();
            mutationStore.transition(
                    mutationId, MutationStatus.COMMITTED, null, clock.instant());
            return changed;
        });

        try {
            fenceService.create(
                    scope.tenantId(), scope.scopeType(), scope.scopeId(), mutationId);
            mutationStore.transition(
                    mutationId, MutationStatus.FENCED, null, clock.instant());
            projector.project(record);
            mutationStore.transition(
                    mutationId, MutationStatus.PROJECTED, null, clock.instant());
            fenceService.release(
                    scope.tenantId(), scope.scopeType(), scope.scopeId());
            mutationStore.transition(
                    mutationId, MutationStatus.COMPLETED, null, clock.instant());
            return new MutationResult<>(
                    mutationId, true, "ALLOW", value, versions);
        } catch (RuntimeException exception) {
            mutationStore.transition(
                    mutationId, MutationStatus.RECOVERY_REQUIRED,
                    "AUTH_PROPAGATION_PENDING", clock.instant());
            return new MutationResult<>(
                    mutationId, false, "AUTH_PROPAGATION_PENDING", value, versions);
        }
    }

    public interface MutationStore {
        void prepare(MutationRecord record);

        void transition(
                String mutationId,
                MutationStatus status,
                String errorCode,
                Instant now);
    }

    @FunctionalInterface
    public interface RuntimeProjector {
        void project(MutationRecord mutation);
    }

    @FunctionalInterface
    public interface TransactionExecutor {
        Object execute(Supplier<?> work);
    }

    @FunctionalInterface
    public interface MutationIdGenerator {
        String next();
    }

    public record MutationScope(
            String tenantId,
            String scopeType,
            String scopeId,
            String commandId,
            String actorId
    ) {
    }

    public record ExpectedVersions(
            Long oldSessionVersion,
            Long newSessionVersion,
            Long oldAuthVersion,
            Long newAuthVersion,
            Long oldPolicyVersion,
            Long newPolicyVersion
    ) {
    }

    public record MutationRecord(
            String mutationId,
            MutationScope scope,
            String subjectId,
            ExpectedVersions versions,
            Instant createdAt
    ) {
    }

    public record MutationResult<T>(
            String mutationId,
            boolean completed,
            String reasonCode,
            T value,
            ExpectedVersions versions
    ) {
    }

    public enum MutationStatus {
        COMMITTED,
        FENCED,
        PROJECTED,
        COMPLETED,
        RECOVERY_REQUIRED
    }
}
