package top.egon.cola.component.yuheng.admin.openapi.repository.jdbc;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import top.egon.cola.component.yuheng.admin.openapi.domain.enums.GatewayOpenApiSyncStateEnum;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JdbcGatewayOpenApiSyncRepositoryTest {

    private static final Instant NOW = Instant.parse(
            "2026-08-26T03:00:00Z"
    );

    @Test
    void claimUsesRevisionCasAndReportsLostOwnership() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        JdbcGatewayOpenApiSyncRepository repository =
                new JdbcGatewayOpenApiSyncRepository(jdbc);

        when(jdbc.update(anyString(), any(Object[].class)))
                .thenReturn(1)
                .thenReturn(0);

        assertThat(repository.claim("sync-1", 4, NOW)).isTrue();
        assertThat(repository.claim("sync-1", 4, NOW)).isFalse();
        verify(jdbc, org.mockito.Mockito.times(2)).update(
                org.mockito.ArgumentMatchers.contains(
                        "revision = revision + 1"
                ),
                any(Object[].class)
        );
    }

    @Test
    void transitionRejectsIllegalStateBeforeJdbcCall() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        JdbcGatewayOpenApiSyncRepository repository =
                new JdbcGatewayOpenApiSyncRepository(jdbc);

        assertThatThrownBy(() -> repository.transition(
                "sync-1",
                1,
                GatewayOpenApiSyncStateEnum.FETCHING,
                GatewayOpenApiSyncStateEnum.VALID,
                NOW
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("illegal OpenAPI sync transition");

        org.mockito.Mockito.verifyNoInteractions(jdbc);
    }

    @Test
    void validTransitionAndSetValidUseAffectedRows() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.update(anyString(), any(Object[].class))).thenReturn(1);
        JdbcGatewayOpenApiSyncRepository repository =
                new JdbcGatewayOpenApiSyncRepository(jdbc);

        assertThat(repository.transition(
                "sync-1",
                1,
                GatewayOpenApiSyncStateEnum.FETCHING,
                GatewayOpenApiSyncStateEnum.VALIDATING,
                NOW
        )).isTrue();
        assertThat(repository.setValid(
                "sync-1",
                2,
                "snapshot-1",
                "set-http-1",
                NOW
        )).isTrue();
        verify(jdbc, org.mockito.Mockito.times(2)).update(
                anyString(),
                any(Object[].class)
        );
    }

    @Test
    void nonPositiveDueLimitIsSafe() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        JdbcGatewayOpenApiSyncRepository repository =
                new JdbcGatewayOpenApiSyncRepository(jdbc);

        assertThat(repository.findDue(NOW, 0)).isEmpty();
        org.mockito.Mockito.verifyNoInteractions(jdbc);
    }
}
