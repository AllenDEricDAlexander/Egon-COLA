package top.egon.cola.component.gateway.admin.infrastructure.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import top.egon.cola.component.gateway.admin.application.reporting
        .GatewayDefinitionLifecycleStore;

import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JdbcGatewayDefinitionLifecycleStoreTest {

    @Test
    void scansActiveDefinitionsWhenNoProviderDefinitionRemains() {
        NamedParameterJdbcTemplate jdbc =
                mock(NamedParameterJdbcTemplate.class);
        doAnswer(invocation -> {
            RowCallbackHandler handler = invocation.getArgument(2);
            java.sql.ResultSet row = mock(java.sql.ResultSet.class);
            when(row.getString("application_id"))
                    .thenReturn("application-1");
            when(row.getString("id")).thenReturn("definition-1");
            handler.processRow(row);
            return null;
        }).when(jdbc).query(
                anyString(),
                any(MapSqlParameterSource.class),
                any(RowCallbackHandler.class)
        );
        when(jdbc.update(
                contains("status = 'RETIRED'"),
                any(MapSqlParameterSource.class)
        )).thenReturn(1);
        when(jdbc.update(
                contains("lifecycle_status = 'OFFLINE'"),
                any(MapSqlParameterSource.class)
        )).thenReturn(2);
        JdbcGatewayDefinitionLifecycleStore store =
                new JdbcGatewayDefinitionLifecycleStore(jdbc);

        GatewayDefinitionLifecycleStore.ReconcileResult result =
                store.reconcile(
                Set.of(),
                Instant.parse("2026-07-25T08:00:00Z")
        );

        assertThat(result.retiredDefinitionSets()).isEqualTo(1);
        assertThat(result.offlinedOperations()).isEqualTo(2);
        verify(jdbc).query(
                contains("status = 'ACTIVE'"),
                any(MapSqlParameterSource.class),
                any(RowCallbackHandler.class)
        );
        verify(jdbc).update(
                contains(
                        "THEN CAST(:now AS TIMESTAMP WITH TIME ZONE)"
                ),
                any(MapSqlParameterSource.class)
        );
    }
}
