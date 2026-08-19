package top.egon.cola.component.outbox.store;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxMigrationContractTest {

    @Test
    void shouldPackageOneNonAutomaticPostgresqlMigrationTemplate() throws Exception {
        ClassPathResource resource = new ClassPathResource(
                "db/transactional-outbox/postgresql/V1__create_transactional_outbox_schema.sql");
        String sql = resource.getContentAsString(StandardCharsets.UTF_8);

        assertThat(resource.exists()).isTrue();
        assertThat(sql)
                .contains("create table egon_cola_outbox_message")
                .contains("uk_outbox_message_id")
                .contains("uk_outbox_idempotency_key")
                .contains("idx_outbox_claim")
                .contains("idx_outbox_reclaim")
                .contains("idx_outbox_cleanup");
        assertThat(new ClassPathResource("db/migration/V1__create_transactional_outbox_schema.sql").exists())
                .isFalse();
    }
}
