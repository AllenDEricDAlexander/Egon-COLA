package top.egon.cola.component.yuheng.test.tianquan.shoubing;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfEnvironmentVariable(named = "XINGYUAN_ISOLATED_INTEGRATION", matches = "true")
class UnifiedIdentityRevocationIT {

    @Test
    void actualFormalFamilyRevocationRejectsTheSameUnexpiredAccessToken() throws Exception {
        var client = UnifiedIdentityLiveClient.isolated();
        var reference = client.formal("default", client.resourceForProcess("native-light"), "admin");
        client.bindNativeTenantReadEndpoints(reference, client.formal("tenant-b", reference.getResourceKey(), "admin"));
        var jwt = client.verifiedClaims(reference);
        assertThat(reference.getExpiresAt().isAfter(Instant.now().plusSeconds(30))).isTrue();
        assertThat(client.read(reference)).isEqualTo(200);
        // The original AS owner must confirm this exact PUBLIC family; BFF logout is not its revocation event.
        client.awaitFormalRevocation(reference, jwt);
        assertThat(reference.getExpiresAt().isAfter(Instant.now())).isTrue();
        assertThat(client.read(reference)).isEqualTo(401);
        assertThat(reference.getExpiresAt().isAfter(Instant.now())).isTrue();
    }
}
