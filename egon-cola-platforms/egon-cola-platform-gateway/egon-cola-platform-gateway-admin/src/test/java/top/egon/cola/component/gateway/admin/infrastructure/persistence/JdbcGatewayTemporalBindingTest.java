package top.egon.cola.component.gateway.admin.credential.repository.jdbc;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import top.egon.cola.component.gateway.admin.credential.repository.GatewayCredentialRepository;
import top.egon.cola.component.gateway.admin.reporting.repository.jdbc.JdbcGatewayHmacNonceRepository;
import top.egon.cola.component.gateway.admin.release.repository.jdbc.JdbcGatewayReleasePublicationRepository;

import java.sql.Timestamp;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JdbcGatewayTemporalBindingTest {

    private static final Instant NOW =
            Instant.parse("2026-07-27T06:00:00Z");

    @Test
    void credentialWritesUseJdbcTimestampValues() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        JdbcGatewayCredentialRepository store =
                new JdbcGatewayCredentialRepository(jdbc);

        store.insert(new top.egon.cola.component.gateway.admin.credential.domain.po.GatewayCredentialPO(
                "credential-1",
                "application-1",
                "access-1",
                "ciphertext",
                "v1",
                "ACTIVE",
                NOW,
                null,
                NOW,
                NOW
        ));

        ArgumentCaptor<Object[]> parameters =
                ArgumentCaptor.forClass(Object[].class);
        verify(jdbc).update(anyString(), parameters.capture());
        assertThat(parameters.getValue())
                .contains(Timestamp.from(NOW))
                .doesNotContain(NOW);
    }

    @Test
    void nonceCleanupUsesJdbcTimestampValue() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.update(anyString(), any(Object[].class))).thenReturn(0);
        JdbcGatewayHmacNonceRepository store =
                new JdbcGatewayHmacNonceRepository(jdbc);

        store.deleteExpired(NOW);

        ArgumentCaptor<Object[]> parameters =
                ArgumentCaptor.forClass(Object[].class);
        verify(jdbc).update(anyString(), parameters.capture());
        assertThat(parameters.getValue())
                .containsExactly(Timestamp.from(NOW));
    }

    @Test
    void chunkCleanupQueryUsesJdbcTimestampValue() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.query(
                anyString(),
                any(org.springframework.jdbc.core.RowMapper.class),
                any(Object[].class)
        )).thenReturn(java.util.List.of());
        JdbcGatewayReleasePublicationRepository store =
                new JdbcGatewayReleasePublicationRepository(jdbc);

        store.findChunkCleanupCandidates(NOW);

        ArgumentCaptor<Object[]> parameters =
                ArgumentCaptor.forClass(Object[].class);
        verify(jdbc).query(
                anyString(),
                any(org.springframework.jdbc.core.RowMapper.class),
                parameters.capture()
        );
        assertThat(parameters.getValue())
                .containsExactly(Timestamp.from(NOW));
    }
}
