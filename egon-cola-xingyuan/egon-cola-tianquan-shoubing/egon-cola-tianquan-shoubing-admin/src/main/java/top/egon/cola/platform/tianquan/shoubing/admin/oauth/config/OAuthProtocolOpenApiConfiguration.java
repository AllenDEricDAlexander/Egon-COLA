package top.egon.cola.platform.tianquan.shoubing.admin.oauth.config;

import java.util.List;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;

@Configuration(value = "OAuthProtocolOpenApiConfiguration", proxyBeanMethods = false)
public class OAuthProtocolOpenApiConfiguration {

    private static final String DISCOVERY_PATH = "/tenants/{tenantId}/.well-known/openid-configuration";
    private static final String LOGIN_PATH = "/tenants/{tenantId}/login";

    @Bean("oauthProtocolOpenApiCustomizer")
    OpenApiCustomizer oauthProtocolOpenApiCustomizer() {
        return openApi -> {
            Operation discovery = new Operation()
                    .operationId("oauthTenantOidcDiscovery")
                    .summary("Query tenant OpenID Provider configuration")
                    .description("Returns protocol metadata for an exact active tenant issuer.")
                    .tags(List.of("OAuth Protocol"))
                    .addParametersItem(new Parameter()
                            .name("tenantId")
                            .in("path")
                            .required(true)
                            .description("Positive tenant identifier")
                            .schema(new StringSchema().pattern("^[1-9][0-9]{0,18}$")))
                    .responses(discoveryResponses())
                    .security(List.of());
            openApi.path(DISCOVERY_PATH, new PathItem().get(discovery));
            PathItem login = openApi.getPaths().get(LOGIN_PATH);
            if (login == null) {
                login = new PathItem();
            }
            login.setPost(loginSubmitOperation());
            openApi.path(LOGIN_PATH, login);
            addNativeProtocolOperations(openApi);
            addNativePreflightOperations(openApi);
        };
    }

    private static void addNativePreflightOperations(io.swagger.v3.oas.models.OpenAPI api) {
        java.util.Map<String, String> operations = java.util.Map.of(
                "oauth2/token", "oauthTenantTokenCorsPreflight", "userinfo", "oauthTenantUserInfoCorsPreflight",
                ".well-known/openid-configuration", "oauthTenantDiscoveryCorsPreflight",
                ".well-known/jwks.json", "oauthTenantJwksCorsPreflight");
        operations.forEach((suffix, id) -> {
            String method = suffix.equals("oauth2/token") ? "POST" : "GET";
            Operation operation = nativeOperation(id, "Negotiate exact-origin tenant OAuth CORS", false)
                    .security(List.of()).addParametersItem(new Parameter().name("Origin").in("header").required(true)
                            .schema(new StringSchema().maxLength(2048)))
                    .addParametersItem(new Parameter().name("Access-Control-Request-Method").in("header").required(true)
                            .schema(new StringSchema()._enum(List.of(method))))
                    .addParametersItem(new Parameter().name("Access-Control-Request-Headers").in("header")
                            .description("Single comma-separated, duplicate-free Authorization/Content-Type subset; at most 16 tokens of 64 characters.")
                            .schema(new StringSchema()))
                    .responses(new ApiResponses().addApiResponse("200", new ApiResponse().description("Bodyless preflight allowed")
                            .addHeaderObject("Access-Control-Allow-Origin", new Header().schema(new StringSchema()))
                            .addHeaderObject("Access-Control-Allow-Methods", new Header().schema(new StringSchema()._enum(List.of(method))))
                            .addHeaderObject("Access-Control-Allow-Headers", new Header().schema(new StringSchema()))
                            .addHeaderObject("Access-Control-Max-Age", new Header().schema(new StringSchema()._enum(List.of("600"))))
                            .addHeaderObject("Vary", new Header().schema(new StringSchema())))
                            .addApiResponse("403", new ApiResponse().description("Invalid CORS request; no allow headers")));
            api.getPaths().get("/tenants/{tenantId}/" + suffix).setOptions(operation);
        });
    }

    private static void addNativeProtocolOperations(io.swagger.v3.oas.models.OpenAPI api) {
        if (api.getComponents() == null) {
            api.setComponents(new io.swagger.v3.oas.models.Components());
        }
        api.getComponents().addSecuritySchemes("oauthClientBasic", new io.swagger.v3.oas.models.security.SecurityScheme()
                .type(io.swagger.v3.oas.models.security.SecurityScheme.Type.HTTP).scheme("basic")
                .description("Registered confidential client; no body client_secret or mixed credentials."));
        api.getComponents().addSecuritySchemes("oauthUserBearer", new io.swagger.v3.oas.models.security.SecurityScheme()
                .type(io.swagger.v3.oas.models.security.SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")
                .description("USER access token for this issuer and openid scope; ID Token is not an API credential."));
        api.getComponents().addSecuritySchemes("oauthAuthorizationSession", new io.swagger.v3.oas.models.security.SecurityScheme()
                .type(io.swagger.v3.oas.models.security.SecurityScheme.Type.APIKEY)
                .in(io.swagger.v3.oas.models.security.SecurityScheme.In.COOKIE).name("SESSION")
                .description("Authenticated SAS authorization session and transaction-bound CSRF."));

        ObjectSchema jwks = new ObjectSchema();
        jwks.addProperty("keys", new io.swagger.v3.oas.models.media.ArraySchema().items(new ObjectSchema()
                .addProperty("kty", new StringSchema()._enum(List.of("RSA")))
                .addProperty("kid", new StringSchema()).addProperty("use", new StringSchema()._enum(List.of("sig")))
                .addProperty("alg", new StringSchema()._enum(List.of("RS256")))
                .addProperty("n", new StringSchema()).addProperty("e", new StringSchema())
                .required(List.of("kty", "kid", "n", "e"))));
        jwks.required(List.of("keys"));
        for (boolean platform : List.of(false, true)) {
            String prefix = platform ? "/platform" : "/tenants/{tenantId}";
            String role = platform ? "Platform" : "Tenant";
            Operation metadata = nativeOperation("oauth" + role + "OauthMetadata", "OAuth authorization server metadata", platform)
                    .responses(discoveryResponses());
            if (platform) {
                metadata.setOperationId("oauthPlatformMetadata");
            }
            api.path("/.well-known/oauth-authorization-server" + prefix, new PathItem().get(metadata));
            api.path(prefix + "/.well-known/jwks.json", new PathItem().get(nativeOperation(
                    "oauth" + role + "Jwks", "Public issuer RSA keys", platform)
                    .responses(protocolResponses(jwks, "public, max-age=60", false))));
            Operation token = nativeOperation("oauth" + role + "Token", "Issue tokens for a registered client", platform)
                    .description(platform ? "PLATFORM accepts only client_credentials and emits no tenant, ID or refresh token."
                            : "authorization_code requires S256 PKCE. Only confidential USER may receive refresh tokens; SERVICE never receives ID/refresh tokens.")
                    .requestBody(new RequestBody().required(true).content(new Content().addMediaType(
                            "application/x-www-form-urlencoded", new MediaType().schema(tokenForm(platform)))))
                    .responses(protocolResponses(tokenResult(platform), "no-store", true));
            token.addSecurityItem(new io.swagger.v3.oas.models.security.SecurityRequirement().addList("oauthClientBasic"));
            if (!platform) {
                token.addSecurityItem(new io.swagger.v3.oas.models.security.SecurityRequirement());
            }
            api.path(prefix + "/oauth2/token", new PathItem().post(token));
            ObjectSchema revokeForm = new ObjectSchema();
            revokeForm.addProperty("token", new StringSchema().minLength(1).maxLength(16384));
            revokeForm.addProperty("token_type_hint", new StringSchema()._enum(List.of("access_token", "refresh_token")));
            if (!platform) {
                revokeForm.addProperty("client_id", new StringSchema().minLength(1).maxLength(128));
            }
            revokeForm.required(List.of("token"));
            Operation revoke = nativeOperation("oauth" + role + "Revoke", "Revoke a token belonging to this client", platform)
                    .description("No cross-client or cross-issuer lookup; success follows the durable shared revocation visibility gate.")
                    .requestBody(new RequestBody().required(true).content(new Content().addMediaType(
                            "application/x-www-form-urlencoded", new MediaType().schema(revokeForm))))
                    .responses(protocolResponses(null, "no-store", true))
                    .addSecurityItem(new io.swagger.v3.oas.models.security.SecurityRequirement().addList("oauthClientBasic"));
            if (!platform) {
                revoke.addSecurityItem(new io.swagger.v3.oas.models.security.SecurityRequirement());
            }
            api.path(prefix + "/oauth2/revoke", new PathItem().post(revoke));
        }

        Operation authorize = nativeOperation("oauthAuthorizationRequest", "Start a PKCE authorization transaction", false);
        for (String name : List.of("client_id", "redirect_uri", "state", "scope", "nonce", "resource")) {
            StringSchema schema = new StringSchema();
            switch (name) {
                case "state", "nonce" -> schema.minLength(32).maxLength(512);
                case "redirect_uri", "resource" -> schema.minLength(1).maxLength(2048).format("uri");
                case "scope" -> schema.minLength(1).maxLength(8320)
                        .pattern("^[A-Za-z0-9._:-]{1,128}( [A-Za-z0-9._:-]{1,128}){0,63}$");
                default -> schema.minLength(1).maxLength(128);
            }
            authorize.addParametersItem(new Parameter().name(name).in("query")
                    .required(true).schema(schema)
                    .description("Single registered/transaction-bound value; never chooses a storage tenant."));
        }
        authorize.addParametersItem(new Parameter().name("prompt").in("query").required(false)
                .schema(new StringSchema()._enum(List.of("none", "login", "consent"))));
        authorize.addParametersItem(new Parameter().name("max_age").in("query").required(false)
                .schema(new io.swagger.v3.oas.models.media.IntegerSchema().minimum(java.math.BigDecimal.ZERO)
                        .maximum(java.math.BigDecimal.valueOf(86400))));
        authorize.addParametersItem(new Parameter().name("response_type").in("query").required(true)
                .schema(new StringSchema()._enum(List.of("code"))));
        authorize.addParametersItem(new Parameter().name("code_challenge").in("query").required(true)
                .schema(new StringSchema().pattern("^[A-Za-z0-9_-]{43}$")));
        authorize.addParametersItem(new Parameter().name("code_challenge_method").in("query").required(true)
                .schema(new StringSchema()._enum(List.of("S256"))));
        authorize.setResponses(redirectResponses("302", "Validated client callback or native consent page")
                .addApiResponse("303", new ApiResponse().description("Resume through the issuer login page")
                        .addHeaderObject("Location", new Header().required(true).schema(new StringSchema().format("uri")))
                        .addHeaderObject("Cache-Control", new Header().schema(new StringSchema()._enum(List.of("no-store"))))));
        ObjectSchema consentForm = new ObjectSchema();
        consentForm.addProperty("client_id", new StringSchema().minLength(1).maxLength(128));
        consentForm.addProperty("state", new StringSchema().minLength(32).maxLength(256).pattern("^[A-Za-z0-9_-]{32,256}={0,2}$"));
        consentForm.addProperty("scope", new io.swagger.v3.oas.models.media.ArraySchema().maxItems(64)
                .items(new StringSchema().minLength(1).maxLength(128)));
        consentForm.addProperty("_csrf", new StringSchema().minLength(1));
        consentForm.addProperty("consent_action", new StringSchema()._enum(List.of("deny")));
        consentForm.required(List.of("client_id", "state", "_csrf"));
        Operation consent = nativeOperation("oauthAuthorizationConsent", "Allow or reject this authorization transaction", false)
                .description("Scopes must be a subset of the pending request; an empty selection denies consent. Session and CSRF are required by the effective contract.")
                .requestBody(new RequestBody().required(true).content(new Content().addMediaType(
                        "application/x-www-form-urlencoded", new MediaType().schema(consentForm))))
                .responses(redirectResponses("302", "Validated client callback with code or access_denied"))
                .addSecurityItem(new io.swagger.v3.oas.models.security.SecurityRequirement().addList("oauthAuthorizationSession"));
        api.path("/tenants/{tenantId}/oauth2/authorize", new PathItem().get(authorize).post(consent));
        Operation consentPage = nativeOperation("oauthSasConsentPage", "Render the current native consent transaction", false)
                .description("Issuer/client/subject/session-bound pending state; escaped HTML, unchecked optional scopes and independent cancel form. Read only; no token or secret output.")
                .addParametersItem(new Parameter().name("client_id").in("query").required(true)
                        .schema(new StringSchema().minLength(1).maxLength(128)))
                .addParametersItem(new Parameter().name("state").in("query").required(true)
                        .schema(new StringSchema().minLength(32).maxLength(256).pattern("^[A-Za-z0-9_-]{32,256}={0,2}$")))
                .addParametersItem(new Parameter().name("scope").in("query").required(false)
                        .schema(new StringSchema().maxLength(8320)))
                .addSecurityItem(new io.swagger.v3.oas.models.security.SecurityRequirement().addList("oauthAuthorizationSession"));
        ApiResponses consentPageResponses = new ApiResponses();
        for (String code : List.of("200", "400", "401", "404", "503")) {
            consentPageResponses.addApiResponse(code, new ApiResponse().description(code.equals("200") ? "Current escaped consent form" : "Constant HTML rejection")
                    .addHeaderObject("Cache-Control", new Header().schema(new StringSchema()._enum(List.of("no-store"))))
                    .content(new Content().addMediaType("text/html", new MediaType().schema(new StringSchema()))));
        }
        api.path("/tenants/{tenantId}/consent", new PathItem().get(consentPage.responses(consentPageResponses)));
        api.path("/tenants/{tenantId}/userinfo", new PathItem().get(nativeOperation(
                "oauthTenantUserinfo", "Query this USER identity", false)
                .responses(protocolResponses(new ObjectSchema().addProperty("sub", new StringSchema()).required(List.of("sub")),
                        "no-store", true))
                .addSecurityItem(new io.swagger.v3.oas.models.security.SecurityRequirement().addList("oauthUserBearer"))));

        String[] legacy = {"/.well-known/oauth-authorization-server", "/oauth2/jwks", "/oauth2/login/csrf",
                "/oauth2/login", "/oauth2/token", "/oauth2/revoke", "/oauth2/logout", "/oauth2/step-up",
                "/oauth2/userinfo", "/internal/v1/oauth2/refresh-token/validate"};
        for (int i = 0; i < legacy.length; i++) {
            var body = new ObjectSchema().addProperty("error", new StringSchema()._enum(List.of("legacy_endpoint_retired")))
                    .addProperty("error_description", new StringSchema()).required(List.of("error"));
            Operation retired = new Operation().operationId("oauthLegacyRetired" + (i + 1)).deprecated(true)
                    .summary("Retired legacy operation").description("Returns 410 before reading credentials; no token, redirect or Cookie is issued.")
                    .security(List.of()).responses(new ApiResponses().addApiResponse("410", new ApiResponse()
                            .description("Use the exact configured issuer and reauthenticate")
                            .addHeaderObject("Cache-Control", new Header().schema(new StringSchema()._enum(List.of("no-store"))))
                            .content(new Content().addMediaType("application/json", new MediaType().schema(body)))));
            boolean get = i < 3 || i == 8;
            api.path(legacy[i], get ? new PathItem().get(retired) : new PathItem().post(retired));
        }
    }

    private static Operation nativeOperation(String id, String summary, boolean platform) {
        Operation operation = new Operation().operationId(id).summary(summary).tags(List.of("OAuth Protocol"))
                .security(new java.util.ArrayList<>());
        if (!platform) {
            operation.addParametersItem(new Parameter().name("tenantId").in("path").required(true)
                    .schema(new StringSchema().pattern("^[1-9][0-9]{0,18}$")));
        }
        return operation;
    }

    private static ObjectSchema tokenForm(boolean platform) {
        ObjectSchema form = new ObjectSchema();
        form.addProperty("grant_type", new StringSchema()._enum(platform ? List.of("client_credentials")
                : List.of("authorization_code", "refresh_token", "client_credentials")));
        form.addProperty("resource", new StringSchema().format("uri"));
        form.addProperty("scope", new StringSchema().description("Space-separated, registered scopes within this client's grant."));
        if (!platform) {
            form.addProperty("client_id", new StringSchema().minLength(1).maxLength(128)
                    .description("Required for public none authentication; prohibited when Basic is sent."));
            form.addProperty("code", new StringSchema().minLength(1).maxLength(16384));
            form.addProperty("code_verifier", new StringSchema().pattern("^[A-Za-z0-9._~-]{43,128}$"));
            form.addProperty("redirect_uri", new StringSchema().format("uri"));
            form.addProperty("refresh_token", new StringSchema().minLength(1).maxLength(16384));
        }
        form.required(List.of("grant_type"));
        return form;
    }

    private static ObjectSchema tokenResult(boolean platform) {
        ObjectSchema result = new ObjectSchema();
        result.addProperty("access_token", new StringSchema());
        result.addProperty("token_type", new StringSchema()._enum(List.of("Bearer")));
        result.addProperty("expires_in", new io.swagger.v3.oas.models.media.IntegerSchema().format("int64"));
        result.addProperty("scope", new StringSchema());
        if (!platform) {
            result.addProperty("refresh_token", new StringSchema().nullable(true));
            result.addProperty("id_token", new StringSchema().nullable(true));
        }
        result.required(List.of("access_token", "token_type", "expires_in", "scope"));
        return result;
    }

    private static ApiResponses protocolResponses(io.swagger.v3.oas.models.media.Schema<?> schema, String cache, boolean clientErrors) {
        ApiResponse success = new ApiResponse().description(schema == null ? "Success; no response body" : "Protocol response")
                .addHeaderObject("Cache-Control", new Header().schema(new StringSchema()._enum(List.of(cache))));
        if (schema != null) {
            success.content(new Content().addMediaType("application/json", new MediaType().schema(schema)));
        }
        Content error = new Content().addMediaType("application/json", new MediaType().schema(new ObjectSchema()
                .addProperty("error", new StringSchema()).addProperty("error_description", new StringSchema()).required(List.of("error"))));
        ApiResponses responses = new ApiResponses().addApiResponse("200", success)
                .addApiResponse("400", new ApiResponse().description("Invalid request/grant/target").content(error))
                .addApiResponse("404", new ApiResponse().description("Unknown issuer").content(error))
                .addApiResponse("503", new ApiResponse().description("Security/registry dependency unavailable").content(error));
        if (clientErrors) {
            responses.addApiResponse("401", new ApiResponse().description("Invalid client or access token").content(error));
            responses.addApiResponse("403", new ApiResponse().description("Forbidden scope/identity").content(error));
        }
        return responses;
    }

    private static ApiResponses redirectResponses(String status, String description) {
        ApiResponses responses = protocolResponses(null, "no-store", true);
        responses.remove("200");
        responses.addApiResponse(status, new ApiResponse().description(description)
                .addHeaderObject("Location", new Header().required(true).schema(new StringSchema().format("uri")))
                .addHeaderObject("Cache-Control", new Header().schema(new StringSchema()._enum(List.of("no-store")))));
        return responses;
    }

    private static Operation loginSubmitOperation() {
        ObjectSchema form = new ObjectSchema();
        form.addProperty("username", new StringSchema().minLength(1).maxLength(128));
        form.addProperty("password", new StringSchema().minLength(1).maxLength(1024).writeOnly(true));
        form.addProperty("_csrf", new StringSchema().minLength(1));
        form.required(List.of("username", "password", "_csrf"));
        Content requestContent = new Content().addMediaType(
                "application/x-www-form-urlencoded", new MediaType().schema(form));
        ApiResponses responses = new ApiResponses()
                .addApiResponse("303", new ApiResponse().description("Authenticated; resume saved authorization request")
                        .addHeaderObject("Location", new Header().required(true)
                                .schema(new StringSchema().format("uri-reference"))))
                .addApiResponse("400", htmlError("Invalid login form or CSRF"))
                .addApiResponse("401", htmlError("Authentication failed"))
                .addApiResponse("403", htmlError("Membership disabled or password change required"))
                .addApiResponse("404", htmlError("Authorization transaction unavailable"))
                .addApiResponse("503", htmlError("Identity or issuer service unavailable"));
        return new Operation()
                .operationId("oauthSasLoginSubmit")
                .summary("Authenticate the user and resume the saved issuer authorization request")
                .tags(List.of("OAuth Protocol"))
                .addParametersItem(new Parameter()
                        .name("tenantId")
                        .in("path")
                        .required(true)
                        .description("Positive tenant identifier")
                        .schema(new StringSchema().pattern("^[1-9][0-9]{0,18}$")))
                .requestBody(new RequestBody().required(true).content(requestContent))
                .responses(responses)
                .security(List.of());
    }

    private static ApiResponse htmlError(String description) {
        return new ApiResponse().description(description)
                .content(new Content().addMediaType("text/html", new MediaType().schema(new StringSchema())));
    }

    private static ApiResponses discoveryResponses() {
        ObjectSchema metadata = new ObjectSchema();
        metadata.addProperty("issuer", new StringSchema().format("uri"));
        metadata.addProperty("authorization_endpoint", new StringSchema().format("uri"));
        metadata.addProperty("token_endpoint", new StringSchema().format("uri"));
        metadata.addProperty("jwks_uri", new StringSchema().format("uri"));
        metadata.addProperty("userinfo_endpoint", new StringSchema().format("uri"));
        metadata.addProperty("response_types_supported", new io.swagger.v3.oas.models.media.ArraySchema()
                .items(new StringSchema()));
        metadata.addProperty("grant_types_supported", new io.swagger.v3.oas.models.media.ArraySchema()
                .items(new StringSchema()));
        metadata.addProperty("subject_types_supported", new io.swagger.v3.oas.models.media.ArraySchema()
                .items(new StringSchema()));
        metadata.addProperty("id_token_signing_alg_values_supported",
                new io.swagger.v3.oas.models.media.ArraySchema().items(new StringSchema()));
        metadata.addProperty("token_endpoint_auth_methods_supported",
                new io.swagger.v3.oas.models.media.ArraySchema().items(new StringSchema()));
        metadata.addProperty("code_challenge_methods_supported",
                new io.swagger.v3.oas.models.media.ArraySchema().items(new StringSchema()));
        metadata.addProperty("scopes_supported", new io.swagger.v3.oas.models.media.ArraySchema()
                .items(new StringSchema()));
        metadata.required(List.of(
                "issuer",
                "authorization_endpoint",
                "token_endpoint",
                "jwks_uri",
                "userinfo_endpoint",
                "response_types_supported",
                "grant_types_supported",
                "subject_types_supported",
                "id_token_signing_alg_values_supported",
                "token_endpoint_auth_methods_supported",
                "code_challenge_methods_supported",
                "scopes_supported"));
        Content json = new Content().addMediaType("application/json", new MediaType().schema(metadata));
        Content error = new Content().addMediaType("application/json", new MediaType().schema(
                new ObjectSchema()
                        .addProperty("error", new StringSchema())
                        .addProperty("error_description", new StringSchema())
                        .required(List.of("error"))));
        return new ApiResponses()
                .addApiResponse("200", new ApiResponse().description("OpenID Provider configuration").content(json))
                .addApiResponse("404", new ApiResponse().description("Issuer is not available").content(error))
                .addApiResponse("503", new ApiResponse().description("Issuer registry is unavailable").content(error));
    }
}
