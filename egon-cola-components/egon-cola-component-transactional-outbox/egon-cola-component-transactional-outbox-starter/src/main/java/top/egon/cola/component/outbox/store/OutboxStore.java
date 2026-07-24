package top.egon.cola.component.outbox.store;

import top.egon.cola.component.outbox.api.OutboxReceipt;

import java.time.Duration;
import java.util.Collection;
import java.util.List;

public interface OutboxStore {

    OutboxReceipt enqueue(NewOutboxRecord record);

    List<OutboxRecord> claimDue(int limit, String leaseOwner, Duration leaseDuration);

    List<OutboxRecord> claimByMessageIds(
            Collection<String> messageIds,
            int limit,
            String leaseOwner,
            Duration leaseDuration
    );

    boolean markSucceeded(long id, String leaseOwner);

    boolean markRetry(
            long id,
            String leaseOwner,
            Duration delay,
            String errorCode,
            String errorMessage
    );

    boolean markDead(long id, String leaseOwner, String errorCode, String errorMessage);

    int deleteSucceeded(Duration retention, int limit);

    long countBacklog();

    void validateSchema();
}
