package top.egon.cola.platform.rbac3.admin.migration;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class Rbac3IdpMigrationIT {

    @Test
    void v3AdoptsGlobalIdentityAndInvalidatesLegacySessions() throws Exception {
        try (var input = getClass().getClassLoader().getResourceAsStream(
                "db/migration/V3__adopt_idp_identity.sql")) {
            assertThat(input).isNotNull();
            String sql = new String(input.readAllBytes(), StandardCharsets.UTF_8)
                    .toLowerCase();
            assertThat(sql)
                    .contains("identity_sub")
                    .contains("context_version")
                    .contains("delete from rbac3_refresh_token")
                    .contains("delete from rbac3_session_active_role")
                    .contains("delete from rbac3_session");
        }
    }
}
