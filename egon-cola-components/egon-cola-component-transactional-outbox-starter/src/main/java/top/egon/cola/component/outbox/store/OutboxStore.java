package top.egon.cola.component.outbox.store;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import top.egon.cola.component.outbox.api.OutboxReceipt;

import java.time.Duration;
import java.util.Collection;
import java.util.List;

public interface OutboxStore {

    OutboxReceipt enqueue(@NotNull @Valid NewOutboxRecord record);

    List<OutboxRecord> claimDue(
            @Positive @Max(10_000) int limit,
            @NotNull String leaseOwner,
            @NotNull Duration leaseDuration
    );

    List<OutboxRecord> claimByMessageIds(
            Collection<String> messageIds,
            @Positive @Max(10_000) int limit,
            @NotNull String leaseOwner,
            @NotNull Duration leaseDuration
    );

    boolean markSucceeded(@Positive long id, @NotNull String leaseOwner);

    boolean markRetry(
            @Positive long id,
            @NotNull String leaseOwner,
            @NotNull Duration delay,
            String errorCode,
            String errorMessage
    );

    boolean markDead(@Positive long id, @NotNull String leaseOwner, String errorCode, String errorMessage);

    int deleteSucceeded(@NotNull Duration retention, @Positive @Max(10_000) int limit);

    long countBacklog();

    void validateSchema();
}
