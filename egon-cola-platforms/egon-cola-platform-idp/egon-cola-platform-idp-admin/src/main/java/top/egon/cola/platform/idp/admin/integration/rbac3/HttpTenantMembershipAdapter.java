package top.egon.cola.platform.idp.admin.integration.rbac3;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import top.egon.cola.platform.idp.core.port.TenantMembershipPort;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public final class HttpTenantMembershipAdapter
        implements TenantMembershipPort {

    private final RestClient restClient;
    private final String baseUrl;
    private final Supplier<String> authorizationHeader;

    public HttpTenantMembershipAdapter(
            RestClient restClient,
            String baseUrl,
            Supplier<String> authorizationHeader
    ) {
        this.restClient = Objects.requireNonNull(restClient, "restClient");
        this.baseUrl = validBaseUrl(baseUrl);
        this.authorizationHeader = Objects.requireNonNull(
                authorizationHeader,
                "authorizationHeader"
        );
    }

    @Override
    public TenantMembership resolve(
            String identitySub,
            String tenantId,
            String clientId
    ) {
        try {
            MembershipEnvelope response = restClient.post()
                    .uri(baseUrl + "/internal/v1/identity/resolve")
                    .header(
                            HttpHeaders.AUTHORIZATION,
                            serviceAuthorization()
                    )
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new ResolveRequest(
                            required(identitySub, "identitySub"),
                            required(tenantId, "tenantId"),
                            required(clientId, "clientId")
                    ))
                    .retrieve()
                    .onStatus(
                            status -> status.isError(),
                            (request, responseStatus) -> {
                                throw membershipFailure();
                            }
                    )
                    .body(MembershipEnvelope.class);
            return toDomain(response == null ? null : response.data(),
                    identitySub, tenantId);
        } catch (TenantMembershipException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw membershipFailure();
        }
    }

    @Override
    public List<TenantMembership> list(
            String identitySub,
            String clientId
    ) {
        try {
            String subject = required(identitySub, "identitySub");
            MembershipListEnvelope response = restClient.get()
                    .uri(
                            baseUrl
                                    + "/internal/v1/identity/{identitySub}"
                                    + "/tenants?clientId={clientId}",
                            subject,
                            required(clientId, "clientId")
                    )
                    .header(
                            HttpHeaders.AUTHORIZATION,
                            serviceAuthorization()
                    )
                    .retrieve()
                    .onStatus(
                            status -> status.isError(),
                            (request, responseStatus) -> {
                                throw membershipFailure();
                            }
                    )
                    .body(MembershipListEnvelope.class);
            if (response == null || response.data() == null) {
                throw membershipFailure();
            }
            return response.data().stream()
                    .map(value -> toDomain(
                            value,
                            subject,
                            value.tenantId()
                    ))
                    .filter(value -> value.status()
                            == MembershipStatus.ACTIVE)
                    .toList();
        } catch (TenantMembershipException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw membershipFailure();
        }
    }

    private TenantMembership toDomain(
            MembershipResponse response,
            String expectedIdentitySub,
            String expectedTenantId
    ) {
        if (response == null
                || !expectedIdentitySub.equals(response.identitySub())
                || !expectedTenantId.equals(response.tenantId())) {
            throw membershipFailure();
        }
        try {
            return new TenantMembership(
                    required(response.identitySub(), "identitySub"),
                    required(response.tenantId(), "tenantId"),
                    required(response.rbac3UserId(), "rbac3UserId"),
                    required(
                            response.tenantDisplayName(),
                            "tenantDisplayName"
                    ),
                    MembershipStatus.valueOf(required(
                            response.status(),
                            "status"
                    ))
            );
        } catch (IllegalArgumentException exception) {
            throw membershipFailure();
        }
    }

    private String serviceAuthorization() {
        String value = authorizationHeader.get();
        if (value == null
                || value.isBlank()
                || !value.equals(value.trim())
                || value.contains("\r")
                || value.contains("\n")) {
            throw membershipFailure();
        }
        return value;
    }

    private static String validBaseUrl(String value) {
        URI uri = URI.create(required(value, "baseUrl"));
        String scheme = uri.getScheme();
        if (!("http".equalsIgnoreCase(scheme)
                || "https".equalsIgnoreCase(scheme))
                || uri.getHost() == null
                || uri.getUserInfo() != null
                || uri.getQuery() != null
                || uri.getFragment() != null) {
            throw new IllegalArgumentException("invalid RBAC3 base URL");
        }
        String normalized = uri.toString();
        return normalized.endsWith("/")
                ? normalized.substring(0, normalized.length() - 1)
                : normalized;
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank() || !value.equals(value.trim())) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    private static TenantMembershipException membershipFailure() {
        return new TenantMembershipException(
                "active tenant membership was not resolved"
        );
    }

    private record ResolveRequest(
            String identitySub,
            String tenantId,
            String clientId
    ) {
    }

    private record MembershipEnvelope(MembershipResponse data) {
    }

    private record MembershipListEnvelope(List<MembershipResponse> data) {
    }

    private record MembershipResponse(
            String identitySub,
            String tenantId,
            String rbac3UserId,
            String tenantDisplayName,
            String status
    ) {
    }
}
