package top.egon.cola.component.gateway.admin.migration;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayV6MigrationTest {

    @Test
    void enforcesOneActiveApplicationPerPhysicalIdentity()
            throws IOException {
        InputStream resource = getClass().getClassLoader()
                .getResourceAsStream(
                        "db/migration/"
                                + "V6__enforce_gateway_application_"
                                + "physical_identity.sql"
                );

        assertThat(resource).isNotNull();
        String migration = new String(
                resource.readAllBytes(),
                StandardCharsets.UTF_8
        );

        assertThat(migration)
                .contains("HAVING COUNT(*) > 1")
                .contains("DROP INDEX IF EXISTS "
                        + "uk_gateway_application_scope_active")
                .contains("CREATE UNIQUE INDEX "
                        + "uk_gateway_application_physical_active")
                .contains("(biz_code, application_code, env)")
                .contains("WHERE deleted = FALSE");
    }
}
