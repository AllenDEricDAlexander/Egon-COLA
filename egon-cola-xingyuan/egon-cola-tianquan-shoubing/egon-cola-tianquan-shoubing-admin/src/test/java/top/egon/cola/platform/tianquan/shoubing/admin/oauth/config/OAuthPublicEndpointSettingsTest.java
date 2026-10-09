package top.egon.cola.platform.tianquan.shoubing.admin.oauth.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OAuthPublicEndpointSettingsTest {

    @Test
    void preservesTheAcceptedProtocolPathsWithoutFixingAnIssuer() {
        var settings = new AuthorizationServerConfiguration().authorizationServerSettings();

        assertThat(settings.getJwkSetEndpoint()).isEqualTo("/.well-known/jwks.json");
        assertThat(settings.isMultipleIssuersAllowed()).isTrue();
        assertThat(settings.getIssuer()).isNull();
        assertThat(settings.getAuthorizationEndpoint()).isEqualTo("/oauth2/authorize");
        assertThat(settings.getTokenEndpoint()).isEqualTo("/oauth2/token");
        assertThat(settings.getTokenRevocationEndpoint()).isEqualTo("/oauth2/revoke");
        assertThat(settings.getOidcUserInfoEndpoint()).isEqualTo("/userinfo");
    }
}
