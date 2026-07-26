package top.egon.cola.component.gateway.admin.migration;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayV4MigrationTest {

    @Test
    void createsPublicationJournalWithRecoveryConstraints()
            throws IOException {
        String migration = new String(
                getClass().getClassLoader().getResourceAsStream(
                        "db/migration/"
                                + "V4__add_release_publication_journal.sql"
                ).readAllBytes(),
                StandardCharsets.UTF_8
        );

        assertThat(migration)
                .contains("CREATE TABLE gateway_release_publication")
                .contains("PRIMARY KEY (release_id, attempt_no, phase_order)")
                .contains("UNIQUE (change_id)")
                .contains("REFERENCES gateway_release_attempt")
                .contains("expected_version")
                .contains("content_value")
                .contains("status = 'PLANNED' OR expected_version IS NOT NULL");
    }
}
