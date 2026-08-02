package top.egon.cola.component.gateway.test.idp;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UnifiedIdentityTopologyIT {

    @Test
    void validIdentityReachesTheRbacProtectedBackendThroughGateway()
            throws Exception {
        UnifiedIdentityLiveClient client = UnifiedIdentityLiveClient.enabled();
        String token = client.token("UNIFIED_IDENTITY_DEFAULT_TOKEN_FILE");

        assertThat(client.get(
                client.requiredEnv("UNIFIED_IDENTITY_GATEWAY_URL"),
                "/api/mock/read",
                token)).isEqualTo(200);
        assertThat(client.get(
                client.requiredEnv("UNIFIED_IDENTITY_MOCK_URL"),
                "/api/mock/read",
                token)).isEqualTo(200);
    }
}
