package top.egon.cola.platform.rbac3.admin.infrastructure.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JpaDatabaseClockTest {

    @Test
    void acceptsTheInstantReturnedForPostgresqlTimestampWithTimeZone() {
        EntityManager entityManager = mock(EntityManager.class);
        Query query = mock(Query.class);
        Instant databaseNow = Instant.parse("2026-08-01T14:22:00Z");
        when(entityManager.createNativeQuery("select transaction_timestamp()"))
                .thenReturn(query);
        when(query.getSingleResult()).thenReturn(databaseNow);

        assertThat(new JpaDatabaseClock(entityManager).transactionNow())
                .isEqualTo(databaseNow);
    }
}
