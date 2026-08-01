package top.egon.cola.platform.rbac3.admin.runtime;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.rbac3.admin.runtime.application.IdempotencyService;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IdempotencyServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-30T08:00:00Z");

    @Test
    void storesOnlyHashesAndReplaysTheCompletedSafeResult() {
        InMemoryStore store = new InMemoryStore();
        IdempotencyService service = new IdempotencyService(store);

        IdempotencyService.Claim first = service.claim(command("request-a"));
        service.complete(first.recordId(), "ROLE_ASSIGNMENT", "60001", 200,
                "60001|ACTIVE", NOW);
        IdempotencyService.Claim replay = service.claim(command("request-a"));

        assertThat(store.command.keyHash()).doesNotContain("plain-key");
        assertThat(store.command.requestHash()).doesNotContain("request-a");
        assertThat(replay.outcome()).isEqualTo(IdempotencyService.Outcome.REPLAY);
        assertThat(replay.resourceId()).isEqualTo("60001");
        assertThat(store.responseDigest).doesNotContain("ACTIVE");
    }

    @Test
    void rejectsTheSameKeyWhenCanonicalRequestChanges() {
        InMemoryStore store = new InMemoryStore();
        IdempotencyService service = new IdempotencyService(store);
        service.claim(command("request-a"));

        assertThatThrownBy(() -> service.claim(command("request-b")))
                .isInstanceOf(Rbac3RuleViolation.class)
                .hasMessage("IDEMPOTENCY_CONFLICT");
    }

    private IdempotencyService.Command command(String request) {
        return new IdempotencyService.Command(
                "10001", "USER", "20001", "ASSIGN_ROLE", "plain-key",
                request, NOW.plusSeconds(86400), NOW);
    }

    private static final class InMemoryStore
            implements IdempotencyService.IdempotencyStore {

        private IdempotencyService.StoredCommand command;
        private boolean completed;
        private String resourceId;
        private Integer responseStatus;
        private String responseDigest;

        @Override
        public IdempotencyService.Claim claim(
                IdempotencyService.StoredCommand next
        ) {
            if (command == null) {
                command = next;
                return new IdempotencyService.Claim(
                        "record-1", IdempotencyService.Outcome.CLAIMED,
                        null, null, null);
            }
            if (!command.requestHash().equals(next.requestHash())) {
                return new IdempotencyService.Claim(
                        "record-1", IdempotencyService.Outcome.CONFLICT,
                        null, null, null);
            }
            return new IdempotencyService.Claim(
                    "record-1",
                    completed ? IdempotencyService.Outcome.REPLAY
                            : IdempotencyService.Outcome.IN_PROGRESS,
                    resourceId, responseStatus, responseDigest);
        }

        @Override
        public void complete(
                String recordId,
                String resourceType,
                String completedResourceId,
                int completedStatus,
                String completedDigest,
                Instant now
        ) {
            completed = true;
            resourceId = completedResourceId;
            responseStatus = completedStatus;
            responseDigest = completedDigest;
        }
    }
}
