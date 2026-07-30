package top.egon.cola.platform.rbac3.admin.infrastructure.persistence;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;
import top.egon.cola.platform.rbac3.admin.application.port.DatabaseClock;

import java.time.Instant;
import java.time.OffsetDateTime;

@Component
public final class JpaDatabaseClock implements DatabaseClock {

    private final EntityManager entityManager;

    public JpaDatabaseClock(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Instant transactionNow() {
        Object value = entityManager.createNativeQuery("select transaction_timestamp()")
                .getSingleResult();
        if (value instanceof OffsetDateTime offsetDateTime) {
            return offsetDateTime.toInstant();
        }
        if (value instanceof java.sql.Timestamp timestamp) {
            return timestamp.toInstant();
        }
        throw new IllegalStateException("unsupported PostgreSQL timestamp type");
    }
}
