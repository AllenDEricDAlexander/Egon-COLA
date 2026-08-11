package top.egon.cola.component.gateway.admin.migration;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayV11MigrationTest {

    @Test
    void renamesMcpOAuthAudienceToResourceUri() throws IOException {
        InputStream resource = getClass().getClassLoader()
                .getResourceAsStream(
                        "db/migration/V11__rename_mcp_oauth_resource.sql"
                );

        assertThat(resource).isNotNull();
        String migration = new String(
                resource.readAllBytes(),
                StandardCharsets.UTF_8
        );

        assertThat(migration)
                .contains("ALTER TABLE gateway_mcp_server")
                .contains("RENAME COLUMN oauth_audience TO resource_uri");
    }
}
