package top.egon.cola.component.gateway.admin.observability.repository.jdbc;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JdbcGatewayObservabilityStoreTest {

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void dashboardUsesRealPostgresqlPercentiles() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(
                anyString(),
                any(Class.class),
                any(Object[].class)
        )).thenReturn(0L);
        when(jdbc.query(
                anyString(),
                any(org.springframework.jdbc.core.RowMapper.class),
                any(Object[].class)
        )).thenReturn(List.of());
        when(jdbc.queryForMap(
                anyString(),
                any(Object[].class)
        )).thenReturn(Map.of("total", 0L, "success", 0L));
        JdbcGatewayObservabilityRepository store =
                new JdbcGatewayObservabilityRepository(jdbc);

        store.dashboard(
                "test",
                "gateway",
                Instant.parse("2026-07-25T08:00:00Z")
        );

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        org.mockito.Mockito.verify(jdbc, org.mockito.Mockito.times(2))
                .query(
                        sql.capture(),
                        any(org.springframework.jdbc.core.RowMapper.class),
                        any(Object[].class)
                );
        assertThat(sql.getAllValues()).anySatisfy(query -> {
            assertThat(query).contains(
                    "percentile_cont(0.50)",
                    "percentile_cont(0.95)",
                    "percentile_cont(0.99)",
                    "gateway_call_event_summary"
            );
            assertThat(query).doesNotContain(
                    "average_ms",
                    "maximum_ms"
            );
        });
    }
}
