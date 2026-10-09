package top.egon.cola.platform.tianquan.shoubing.admin.oauth.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OAuthPublicEndpointSettingsTest {

    @Test
    void retiredGlobalProtocolKeysAreAbsentFromBothProfiles() {
        for (String file : java.util.List.of("application.yml", "application-local.yml")) {
            org.springframework.beans.factory.config.YamlPropertiesFactoryBean loader =
                    new org.springframework.beans.factory.config.YamlPropertiesFactoryBean();
            loader.setResources(new org.springframework.core.io.ClassPathResource(file));
            java.util.Properties properties = loader.getObject();
            String prefix = "egon.tianquan-shoubing.oauth.";
            assertThat(properties.stringPropertyNames()).noneMatch(key -> key.equals(prefix + "issuer")
                    || key.equals(prefix + "login-uri") || key.equals(prefix + "refresh-cookie-secure")
                    || key.startsWith(prefix + "signing-key."));
            assertThat(properties.stringPropertyNames()).contains(prefix + "public-origin",
                    prefix + "key-encryption.key-file", prefix + "key-encryption.key-id",
                    "egon.cola.platform.tianquan.shoubing.issuer");
        }
    }

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
