package top.egon.cola.platform.idp.admin.support.outbox.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import top.egon.cola.platform.idp.admin.support.outbox.domain.pojo.IdentityOutboxEventEntity;

import java.time.Instant;
import java.util.List;

public interface IdentityOutboxEventRepository
        extends JpaRepository<IdentityOutboxEventEntity, String> {

    List<IdentityOutboxEventEntity> findTop100ByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAt(
            IdentityOutboxEventEntity.Status status,
            Instant nextAttemptAt
    );
}
