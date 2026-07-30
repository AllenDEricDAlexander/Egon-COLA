package top.egon.cola.platform.rbac3.admin.runtime.application;

import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Objects;

/**
 * Claims idempotency keys without persisting request bodies or sensitive responses.
 */
public final class IdempotencyService {

    private final IdempotencyStore store;

    public IdempotencyService(IdempotencyStore store) {
        this.store = Objects.requireNonNull(store, "store");
    }

    public Claim claim(Command command) {
        String keyHash = sha256(command.idempotencyKey());
        String requestHash = sha256(command.canonicalRequest());
        Claim claim = store.claim(new StoredCommand(
                command.tenantId(), command.actorType(), command.actorId(),
                command.operationCode(), keyHash, requestHash,
                command.expiresAt(), command.now()));
        if (claim.outcome() == Outcome.CONFLICT) {
            throw new Rbac3RuleViolation("IDEMPOTENCY_CONFLICT");
        }
        if (claim.outcome() == Outcome.IN_PROGRESS) {
            throw new Rbac3RuleViolation("AUTH_MUTATION_CONFLICT");
        }
        return claim;
    }

    public void complete(
            String recordId,
            String resourceType,
            String resourceId,
            int responseStatus,
            String safeResponseDigest,
            Instant now
    ) {
        store.complete(
                recordId, resourceType, resourceId, responseStatus,
                sha256(safeResponseDigest), now);
    }

    public interface IdempotencyStore {
        Claim claim(StoredCommand command);

        void complete(
                String recordId,
                String resourceType,
                String resourceId,
                int responseStatus,
                String responseDigest,
                Instant now);
    }

    public record Command(
            String tenantId,
            String actorType,
            String actorId,
            String operationCode,
            String idempotencyKey,
            String canonicalRequest,
            Instant expiresAt,
            Instant now
    ) {
    }

    public record StoredCommand(
            String tenantId,
            String actorType,
            String actorId,
            String operationCode,
            String keyHash,
            String requestHash,
            Instant expiresAt,
            Instant now
    ) {
    }

    public record Claim(
            String recordId,
            Outcome outcome,
            String resourceId,
            Integer responseStatus,
            String responseDigest
    ) {
    }

    public enum Outcome {
        CLAIMED,
        REPLAY,
        IN_PROGRESS,
        CONFLICT
    }

    private String sha256(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("idempotency value is required");
        }
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
