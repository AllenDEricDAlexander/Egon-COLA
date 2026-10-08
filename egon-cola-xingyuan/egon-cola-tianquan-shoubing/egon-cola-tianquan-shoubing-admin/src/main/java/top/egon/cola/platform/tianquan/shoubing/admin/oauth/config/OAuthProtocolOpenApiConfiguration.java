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
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;

@Configuration(proxyBeanMethods = false)
public class OAuthProtocolOpenApiConfiguration {

    private static final String DISCOVERY_PATH = "/tenants/{tenantId}/.well-known/openid-configuration";

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
        };
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
