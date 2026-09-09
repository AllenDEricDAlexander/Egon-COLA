package top.egon.cola.component.yuheng.admin.openapi.repository.jdbc;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JdbcGatewayOpenApiSnapshotRepositoryTest {

    @Test
    void linksAllGroupsToOneDefinitionSetWithAffectedRowChecks() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.update(anyString(), any(Object[].class))).thenReturn(1);
        JdbcGatewayOpenApiSnapshotRepository repository =
                new JdbcGatewayOpenApiSnapshotRepository(
                        jdbc,
                        new ObjectMapper()
                );

        assertThat(repository.linkAllToDefinitionSet(
                List.of("snapshot-orders", "snapshot-inventory"),
                "set-http-1"
        )).isEqualTo(2);

        verify(jdbc, org.mockito.Mockito.times(2)).update(
                org.mockito.ArgumentMatchers.contains(
                        "definition_set_id = ?"
                ),
                any(Object[].class)
        );
    }

    @Test
    void rejectsLinkWhenSnapshotAlreadyPointsToAnotherSet() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.update(anyString(), any(Object[].class))).thenReturn(0);
        when(jdbc.query(
                anyString(),
                any(RowMapper.class),
                any(Object[].class)
        )).thenReturn(List.of("set-other"));
        JdbcGatewayOpenApiSnapshotRepository repository =
                new JdbcGatewayOpenApiSnapshotRepository(
                        jdbc,
                        new ObjectMapper()
                );

        assertThatThrownBy(() -> repository.linkAllToDefinitionSet(
                List.of("snapshot-1"),
                "set-http-1"
        )).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("YUHENG_OPENAPI_SNAPSHOT_CONFLICT");
    }

    @Test
    void emptyGroupLookupDoesNotIssueUnboundedSql() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        JdbcGatewayOpenApiSnapshotRepository repository =
                new JdbcGatewayOpenApiSnapshotRepository(
                        jdbc,
                        new ObjectMapper()
                );

        assertThat(repository.findByBuildGroups(
                "app-1",
                "build-1",
                List.of()
        )).isEmpty();
        org.mockito.Mockito.verifyNoInteractions(jdbc);
    }
}
