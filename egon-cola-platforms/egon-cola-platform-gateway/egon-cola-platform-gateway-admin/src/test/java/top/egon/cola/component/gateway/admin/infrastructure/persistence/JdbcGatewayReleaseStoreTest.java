package top.egon.cola.component.gateway.admin.release.repository.jdbc;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import top.egon.cola.component.gateway.admin.release.repository.GatewayReleaseRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JdbcGatewayReleaseStoreTest {

    @Test
    void findsLatestRecoverableAttemptFromPublicationJournal() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        top.egon.cola.component.gateway.admin.release.domain.po.GatewayRecoverableReleaseAttemptPO attempt =
                new top.egon.cola.component.gateway.admin.release.domain.po.GatewayRecoverableReleaseAttemptPO(
                        "release-1",
                        "group-1",
                        2
                );
        when(jdbc.query(
                contains("JOIN gateway_release_publication"),
                any(RowMapper.class)
        )).thenReturn(List.of(attempt));
        JdbcGatewayReleaseRepository store = new JdbcGatewayReleaseRepository(
                jdbc,
                new ObjectMapper()
        );

        assertThat(store.recoverable()).containsExactly(attempt);
        verify(jdbc).query(
                contains("SELECT MAX(candidate.attempt_no)"),
                any(RowMapper.class)
        );
    }
}
