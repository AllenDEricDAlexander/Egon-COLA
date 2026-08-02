package top.egon.cola.platform.idp.admin.outbox.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import top.egon.cola.platform.idp.admin.outbox.domain.IdentityOutboxEventEntity;

import java.time.Instant;
import java.util.List;

public interface IdentityOutboxEventRepository
        extends JpaRepository<IdentityOutboxEventEntity, String> {

    List<IdentityOutboxEventEntity> findTop100ByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAt(
            IdentityOutboxEventEntity.Status status,
            Instant nextAttemptAt
    );
}
