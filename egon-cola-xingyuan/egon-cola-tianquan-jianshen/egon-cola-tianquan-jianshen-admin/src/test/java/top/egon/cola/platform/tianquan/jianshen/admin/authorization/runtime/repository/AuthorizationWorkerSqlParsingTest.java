package top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.apache.shardingsphere.database.connector.core.type.DatabaseType;
import org.apache.shardingsphere.infra.spi.type.typed.TypedSPILoader;
import org.apache.shardingsphere.infra.parser.ShardingSphereSQLParserEngine;
import org.apache.shardingsphere.sql.parser.engine.api.CacheOption;
import org.junit.jupiter.api.Test;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.grant.userrole.repository.jdbc.PostgresqlAssignmentLifecycleRepository;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.repository.jpa.JpaAuthorizationMutationRepository;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthorizationWorkerSqlParsingTest {

    @Test
    void boundedSkipLockedWorkerQueriesParseWithTheActualPostgresqlDialect() {
        EntityManager entityManager = mock(EntityManager.class);
        Query query = mock(Query.class, RETURNS_SELF);
        when(query.getResultList()).thenReturn(List.of());
        var parser = new ShardingSphereSQLParserEngine(TypedSPILoader.getService(DatabaseType.class, "PostgreSQL"),
                new CacheOption(16, 64), new CacheOption(16, 64));
        when(entityManager.createNativeQuery(anyString())).thenAnswer(invocation -> {
            String sql = invocation.getArgument(0);
            assertThat(sql).contains("rbac3_claim_", ":batchSize");
            assertThat(parser.parse(sql.replaceAll(":\\w+", "?"), false)).isNotNull();
            return query;
        });

        assertThat(new PostgresqlAssignmentLifecycleRepository(entityManager)
                .processDue(Instant.parse("2026-10-18T00:00:00Z"), 5, mock(ChangePublisher.class))).isZero();
        assertThat(new JpaAuthorizationMutationRepository(entityManager).claimRecoverable(5)).isEmpty();

        verify(entityManager, times(4)).createNativeQuery(anyString());
        verify(query, times(4)).setParameter("batchSize", 5);
    }
}
