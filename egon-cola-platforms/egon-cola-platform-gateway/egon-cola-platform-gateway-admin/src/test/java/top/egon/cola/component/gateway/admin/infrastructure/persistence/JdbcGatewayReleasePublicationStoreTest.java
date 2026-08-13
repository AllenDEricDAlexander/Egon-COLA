package top.egon.cola.component.gateway.admin.release.repository.jdbc;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import top.egon.cola.component.gateway.admin.release.repository.GatewayReleasePublicationRepository;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static top.egon.cola.component.gateway.admin.release.domain.enums.GatewayPublicationPhaseEnum.ACTIVATION;
import static top.egon.cola.component.gateway.admin.release.domain.enums.GatewayPublicationPhaseEnum.CHUNK;
import static top.egon.cola.component.gateway.admin.release.domain.enums.GatewayPublicationStatusEnum.PLANNED;
import static top.egon.cola.component.gateway.admin.release.domain.enums.GatewayPublicationStatusEnum.RESOLVED;
import static top.egon.cola.component.gateway.admin.release.domain.enums.GatewayPublicationStatusEnum.SUBMITTED;
import static top.egon.cola.component.gateway.admin.release.domain.enums.GatewayPublicationStatusEnum.SUCCESS;

class JdbcGatewayReleasePublicationStoreTest {

    private static final Instant NOW =
            Instant.parse("2026-07-26T08:00:00Z");

    @Test
    void insertsAndReadsAttemptInPhaseOrder() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleasePublicationPO chunk = record(
                0,
                CHUNK,
                "gateway.rules.chunk.release-1.0",
                "018f22d8-155d-7000-8000-000000000001"
        );
        top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleasePublicationPO activation = record(
                1,
                ACTIVATION,
                "gateway.rules.active",
                "018f22d8-155d-7000-8000-000000000002"
        );
        when(jdbc.query(
                contains("FROM gateway_release_publication"),
                any(org.springframework.jdbc.core.RowMapper.class),
                any(Object[].class)
        )).thenReturn(List.of(chunk, activation));
        JdbcGatewayReleasePublicationRepository store =
                new JdbcGatewayReleasePublicationRepository(jdbc);

        store.insertAll(List.of(chunk, activation));

        assertThat(store.findAttempt("release-1", 1))
                .containsExactly(chunk, activation);
        verify(jdbc).query(
                contains("ORDER BY phase_order"),
                any(org.springframework.jdbc.core.RowMapper.class),
                any(Object[].class)
        );
    }

    @Test
    void resolvesBeforeSubmittingAndKeepsChangeIdStable() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.update(
                contains("status = 'RESOLVED'"),
                any(Object[].class)
        )).thenReturn(1);
        when(jdbc.update(
                contains("status = 'SUBMITTED'"),
                any(Object[].class)
        )).thenReturn(1);
        when(jdbc.update(
                contains("ddc_status = ?"),
                any(Object[].class)
        )).thenReturn(1);
        JdbcGatewayReleasePublicationRepository store =
                new JdbcGatewayReleasePublicationRepository(jdbc);

        store.resolveDocument(
                "018f22d8-155d-7000-8000-000000000001",
                3L,
                "gateway:\n  rules:\n    active: value\n",
                NOW
        );
        store.markSubmitted(
                "018f22d8-155d-7000-8000-000000000001",
                NOW
        );
        store.markResult(
                "018f22d8-155d-7000-8000-000000000001",
                4L,
                SUCCESS,
                null,
                null,
                NOW
        );

        verify(jdbc).update(
                contains("content_value = ?"),
                any(Object[].class)
        );
    }

    @Test
    void refusesSubmittedTransitionUnlessResolved() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.update(anyString(), any(Object[].class)))
                .thenReturn(0);
        JdbcGatewayReleasePublicationRepository store =
                new JdbcGatewayReleasePublicationRepository(jdbc);

        assertThatThrownBy(() -> store.markSubmitted(
                "018f22d8-155d-7000-8000-000000000001",
                NOW
        )).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("RESOLVED");
    }

    @Test
    void acceptsOnlyTerminalStatusesAsResults() {
        JdbcGatewayReleasePublicationRepository store =
                new JdbcGatewayReleasePublicationRepository(
                        mock(JdbcTemplate.class)
                );

        assertThatThrownBy(() -> store.markResult(
                "018f22d8-155d-7000-8000-000000000001",
                null,
                SUBMITTED,
                null,
                null,
                NOW
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("terminal");
    }

    @Test
    void cleanupCandidatesProtectActiveAndRetainPredecessor() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        top.egon.cola.component.gateway.admin.release.domain.po.GatewayChunkCleanupCandidatePO candidate =
                new top.egon.cola.component.gateway.admin.release.domain.po.GatewayChunkCleanupCandidatePO(
                        "change-1",
                        "release-old",
                        "gateway-engine-orders",
                        "test",
                        "default",
                        "gateway.rules.chunk.release-old.0",
                        3L
                );
        when(jdbc.query(
                contains("active_draft"),
                any(org.springframework.jdbc.core.RowMapper.class),
                any(Object[].class)
        )).thenReturn(List.of(candidate));
        JdbcGatewayReleasePublicationRepository store =
                new JdbcGatewayReleasePublicationRepository(jdbc);

        assertThat(store.findChunkCleanupCandidates(NOW))
                .containsExactly(candidate);
        verify(jdbc).query(
                contains("activation.updated_at <= ?"),
                any(org.springframework.jdbc.core.RowMapper.class),
                any(Object[].class)
        );
    }

    private top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleasePublicationPO record(
            int phaseOrder,
            top.egon.cola.component.gateway.admin.release.domain.enums.GatewayPublicationPhaseEnum phaseType,
            String configKey,
            String changeId) {
        return new top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleasePublicationPO(
                "release-1",
                1,
                phaseOrder,
                phaseType,
                configKey,
                "{\"releaseId\":\"release-1\"}",
                "a".repeat(64),
                null,
                changeId,
                null,
                PLANNED,
                null,
                null,
                NOW,
                NOW
        );
    }
}
