package top.egon.cola.platform.tianquan.shoubing.admin.oauth.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Paths;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class OAuthProtocolOpenApiContractTest {
    @Test
    void nativeProtocolAndRetiredOperationsAreDocumentedWithoutInventingHandlers() {
        OpenAPI api = new OpenAPI().paths(new Paths());
        new OAuthProtocolOpenApiConfiguration().oauthProtocolOpenApiCustomizer().customise(api);
        assertThat(api.getPaths()).containsKeys(
                "/.well-known/oauth-authorization-server/tenants/{tenantId}",
                "/tenants/{tenantId}/.well-known/jwks.json", "/tenants/{tenantId}/oauth2/authorize",
                "/tenants/{tenantId}/oauth2/token", "/tenants/{tenantId}/oauth2/revoke", "/tenants/{tenantId}/userinfo",
                "/.well-known/oauth-authorization-server/platform", "/platform/.well-known/jwks.json",
                "/platform/oauth2/token", "/platform/oauth2/revoke",
                "/.well-known/oauth-authorization-server", "/oauth2/jwks", "/oauth2/login/csrf",
                "/oauth2/login", "/oauth2/token", "/oauth2/revoke", "/oauth2/logout",
                "/oauth2/step-up", "/oauth2/userinfo", "/internal/v1/oauth2/refresh-token/validate");
        var authorize = api.getPaths().get("/tenants/{tenantId}/oauth2/authorize").getGet();
        assertThat(authorize.getResponses()).containsKeys("302", "303", "400", "503");
        assertThat(authorize.getParameters().stream().filter(parameter -> Boolean.TRUE.equals(parameter.getRequired()))
                .map(parameter -> parameter.getName()).toList()).contains("client_id", "redirect_uri", "state", "scope", "nonce", "resource");
        var maxAge = authorize.getParameters().stream().filter(parameter -> parameter.getName().equals("max_age")).findFirst().orElseThrow();
        assertThat(maxAge.getSchema().getType()).isEqualTo("integer");
        assertThat(maxAge.getSchema().getMaximum()).isEqualByComparingTo("86400");
        assertThat(authorize.getParameters().stream().filter(parameter -> parameter.getName().equals("nonce"))
                .findFirst().orElseThrow().getSchema().getMaxLength()).isEqualTo(512);
        assertThat(api.getPaths().get("/tenants/{tenantId}/oauth2/authorize").getPost()).isNotNull();
        assertThat(api.getPaths().values().stream().flatMap(path -> path.readOperations().stream())
                .map(operation -> operation.getOperationId()).toList()).doesNotHaveDuplicates().doesNotContainNull();
        var token = api.getPaths().get("/tenants/{tenantId}/oauth2/token").getPost();
        assertThat(token.getResponses()).containsKeys("200", "400", "401", "503");
        assertThat(token.getRequestBody().getContent()).containsKey("application/x-www-form-urlencoded");
        assertThat(token.getRequestBody().getContent().get("application/x-www-form-urlencoded")
                .getSchema().getProperties()).containsKeys("grant_type", "code", "code_verifier", "refresh_token", "resource", "scope", "client_id")
                .doesNotContainKeys("password", "username", "client_secret", "tenant_id");
        var platform = api.getPaths().get("/platform/oauth2/token").getPost();
        var grant = (io.swagger.v3.oas.models.media.Schema<?>) platform.getRequestBody().getContent()
                .get("application/x-www-form-urlencoded").getSchema().getProperties().get("grant_type");
        assertThat(grant.getEnum().toString()).isEqualTo("[client_credentials]");
        assertThat(api.getComponents().getSecuritySchemes()).containsKeys("oauthClientBasic", "oauthUserBearer");
        assertThat(api.getPaths().get("/oauth2/token").getPost().getDeprecated()).isTrue();
        assertThat(api.getPaths().get("/oauth2/token").getPost().getResponses()).containsOnlyKeys("410");
        assertThat(api.getPaths().containsKey("/tenants/{tenantId}/consent")).isTrue();
        var page = api.getPaths().get("/tenants/{tenantId}/consent").getGet();
        assertThat(page.getOperationId()).isEqualTo("oauthSasConsentPage");
        assertThat(page.getResponses()).containsOnlyKeys("200", "400", "401", "404", "503");
        assertThat(page.getResponses().get("200").getContent()).containsOnlyKeys("text/html");
        assertThat(page.getSecurity().getFirst()).containsKey("oauthAuthorizationSession");
        var consent = api.getPaths().get("/tenants/{tenantId}/oauth2/authorize").getPost();
        assertThat(consent.getResponses()).containsKeys("302", "403", "503");
        assertThat(consent.getRequestBody().getContent().get("application/x-www-form-urlencoded")
                .getSchema().getProperties()).containsKey("consent_action");
    }
    @Test
    void publishesBodylessPreflightOperationsWithoutChangingNativeOperations() {
        OpenAPI api = new OpenAPI().paths(new Paths());
        new OAuthProtocolOpenApiConfiguration().oauthProtocolOpenApiCustomizer().customise(api);
        var expected = java.util.Map.of(
                "oauth2/token", "oauthTenantTokenCorsPreflight", "userinfo", "oauthTenantUserInfoCorsPreflight",
                ".well-known/openid-configuration", "oauthTenantDiscoveryCorsPreflight", ".well-known/jwks.json", "oauthTenantJwksCorsPreflight");
        expected.forEach((suffix, id) -> {
            var path = api.getPaths().get("/tenants/{tenantId}/" + suffix);
            assertThat(path.getOptions()).as("OPTIONS %s", suffix).isNotNull();
            var operation = path.getOptions();
            assertThat(operation.getOperationId()).isEqualTo(id);
            assertThat(operation.getSecurity()).isEmpty();
            assertThat(operation.getRequestBody()).isNull();
            assertThat(operation.getResponses()).containsOnlyKeys("200", "403");
            operation.getResponses().values().forEach(response -> assertThat(response.getContent()).isNull());
            assertThat(operation.getParameters().stream().map(p -> p.getName()).toList())
                    .contains("tenantId", "Origin", "Access-Control-Request-Method", "Access-Control-Request-Headers");
            assertThat(suffix.equals("oauth2/token") ? path.getPost() : path.getGet()).isNotNull();
        });
        assertThat(api.getPaths().get("/platform/oauth2/token").getOptions()).isNull();
    }

}
