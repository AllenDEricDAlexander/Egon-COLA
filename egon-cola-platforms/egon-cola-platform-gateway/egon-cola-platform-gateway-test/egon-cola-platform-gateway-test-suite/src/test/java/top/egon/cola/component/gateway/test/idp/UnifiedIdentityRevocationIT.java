package top.egon.cola.component.gateway.test.idp;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UnifiedIdentityRevocationIT {

    @Test
    void refreshRevocationLeavesAlreadyIssuedAccessTokenValidUntilExpiry()
            throws Exception {
        UnifiedIdentityLiveClient client = UnifiedIdentityLiveClient.enabled();

        assertThat(client.get(
                client.requiredEnv("UNIFIED_IDENTITY_GATEWAY_URL"),
                "/api/mock/read",
                client.token("UNIFIED_IDENTITY_PRE_LOGOUT_TOKEN_FILE")))
                .isEqualTo(200);
        assertThat(client.get(
                client.requiredEnv("UNIFIED_IDENTITY_GATEWAY_URL"),
                "/api/mock/read",
                client.token("UNIFIED_IDENTITY_DEFAULT_TOKEN_FILE")))
                .isEqualTo(200);
    }
}
