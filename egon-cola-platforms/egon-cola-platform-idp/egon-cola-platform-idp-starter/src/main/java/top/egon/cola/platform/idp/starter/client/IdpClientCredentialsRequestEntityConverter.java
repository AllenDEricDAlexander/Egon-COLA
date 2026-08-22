package top.egon.cola.platform.idp.starter.client;

import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.security.oauth2.client.endpoint.OAuth2ClientCredentialsGrantRequest;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import top.egon.cola.platform.idp.contract.ServiceTokenContext;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

/**
 * Builds the IdP client-credentials request without putting the Secret in the form body.
 *
 * <p>The Spring client provider supplies the registration; the facade binds the
 * request-specific resource, context, tenant and scopes through a short-lived
 * thread-local while the synchronous manager call is in progress.</p>
 */
public class IdpClientCredentialsRequestEntityConverter
        implements Converter<OAuth2ClientCredentialsGrantRequest,
        RequestEntity<?>> {

    private final ThreadLocal<RequestParameters> requestParameters =
            new ThreadLocal<>();

    /** Converts a grant using the parameters bound by the facade. */
    @Override
    public RequestEntity<?> convert(
            OAuth2ClientCredentialsGrantRequest request
    ) {
        RequestParameters parameters = requestParameters.get();
        return create(request, parameters);
    }

    /** Converts the registration credentials into the standard Basic header. */
    public HttpHeaders convertHeaders(
            OAuth2ClientCredentialsGrantRequest request
    ) {
        if (request == null) {
            throw new IllegalArgumentException("grant request is required");
        }
        var registration = request.getClientRegistration();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBasicAuth(
                registration.getClientId(),
                registration.getClientSecret(),
                StandardCharsets.UTF_8
        );
        return headers;
    }

    /** Converts the grant and bound dimensions into form parameters. */
    public MultiValueMap<String, String> convertParameters(
            OAuth2ClientCredentialsGrantRequest request
    ) {
        if (request == null) {
            throw new IllegalArgumentException("grant request is required");
        }
        return parameters(requestParameters.get());
    }

    /**
     * Converts a grant with explicit request dimensions.
     *
     * <p>This overload is also useful for contract tests and custom manager
     * integrations; it never changes the Spring registration Secret.</p>
     */
    public RequestEntity<?> convert(
            OAuth2ClientCredentialsGrantRequest request,
            URI resource,
            ServiceTokenContext context,
            String tenantId,
            Set<String> scopes
    ) {
        return create(
                request,
                new RequestParameters(resource, context, tenantId, scopes)
        );
    }

    /** Binds dimensions for one synchronous manager authorization call. */
    public void bind(
            URI resource,
            ServiceTokenContext context,
            String tenantId,
            Set<String> scopes
    ) {
        requestParameters.set(new RequestParameters(
                resource,
                context,
                tenantId,
                scopes
        ));
    }

    /** Clears the request-scoped dimensions after manager authorization. */
    public void clear() {
        requestParameters.remove();
    }

    private static RequestEntity<?> create(
            OAuth2ClientCredentialsGrantRequest request,
            RequestParameters parameters
    ) {
        if (request == null) {
            throw new IllegalArgumentException("grant request is required");
        }
        var registration = request.getClientRegistration();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBasicAuth(
                registration.getClientId(),
                registration.getClientSecret(),
                StandardCharsets.UTF_8
        );
        MultiValueMap<String, String> body = parameters(parameters);
        URI tokenUri = URI.create(
                registration.getProviderDetails().getTokenUri()
        );
        return new RequestEntity<MultiValueMap<String, String>>(
                body,
                headers,
                HttpMethod.POST,
                tokenUri
        );
    }

    private static MultiValueMap<String, String> parameters(
            RequestParameters parameters
    ) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");
        if (parameters != null) {
            body.add("resource", parameters.resource().toString());
            if (parameters.context() == ServiceTokenContext.TENANT) {
                body.add("tenant_id", parameters.tenantId());
            }
            body.add("scope", String.join(" ", parameters.scopes()));
        }
        return body;
    }

    private record RequestParameters(
            URI resource,
            ServiceTokenContext context,
            String tenantId,
            Set<String> scopes
    ) {
        private RequestParameters {
            if (resource == null || context == null || scopes == null
                    || scopes.isEmpty()) {
                throw new IllegalArgumentException(
                        "OAuth request parameters are invalid"
                );
            }
            if (context == ServiceTokenContext.TENANT
                    && (tenantId == null || tenantId.isBlank())) {
                throw new IllegalArgumentException(
                        "tenantId is required for TENANT context"
                );
            }
            if (context == ServiceTokenContext.PLATFORM && tenantId != null) {
                throw new IllegalArgumentException(
                        "PLATFORM context cannot carry tenantId"
                );
            }
            scopes = Set.copyOf(new LinkedHashSet<>(new TreeSet<>(scopes)));
        }
    }
}
