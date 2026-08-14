package top.egon.cola.component.gateway.test.idp;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UnifiedIdentityTenantSwitchIT {

    @Test
    void tenantSwitchKeepsTheIdentitySubjectButIssuesATenantScopedToken()
            throws Exception {
        UnifiedIdentityLiveClient client = UnifiedIdentityLiveClient.enabled();
        String defaultToken = client.token("UNIFIED_IDENTITY_DEFAULT_TOKEN_FILE");
        String tenantBToken = client.token("UNIFIED_IDENTITY_TENANT_B_TOKEN_FILE");
        JsonNode defaultClaims = client.claims(defaultToken);
        JsonNode tenantBClaims = client.claims(tenantBToken);

        assertThat(defaultClaims.path("tid").asText()).isNotBlank();
        assertThat(tenantBClaims.path("tid").asText())
                .isNotBlank()
                .isNotEqualTo(defaultClaims.path("tid").asText());
        assertThat(tenantBClaims.path("sub").asText())
                .isEqualTo(defaultClaims.path("sub").asText());
        assertThat(defaultClaims.has("sid")).isFalse();
        assertThat(defaultClaims.has("client_id")).isFalse();
        assertThat(defaultClaims.has("token_version")).isFalse();
        assertThat(client.get(
                client.requiredEnv("UNIFIED_IDENTITY_GATEWAY_URL"),
                "/api/mock/read",
                tenantBToken)).isEqualTo(200);
    }
}
