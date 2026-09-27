package top.egon.cola.component.tianshu.admin.security.rpc;

import io.grpc.Context;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import top.egon.cola.component.tianshu.admin.security.tenancy.DdcTenantContextDTO;

import java.security.Principal;
import java.util.Objects;
import java.util.Set;

public record DdcServicePrincipal(
        String credentialId,
        String clientType,
        Set<String> appCodePatterns,
        Set<String> envPatterns,
        Set<String> bizCodePatterns,
        Set<String> allowedOperations,
        String appCode,
        String env,
        String bizCode,
        @NotNull @Positive Long tenantId,
        @NotBlank String identityTenantId,
        Set<String> namespacePatterns
) implements Principal {

    static final Context.Key<DdcServicePrincipal> CONTEXT_KEY =
            Context.key("egon-tianshu-service-principal");

    public static final String REQUEST_ATTRIBUTE =
            DdcServicePrincipal.class.getName();

    public DdcServicePrincipal {
        if (credentialId == null || credentialId.isBlank()) {
            throw new IllegalArgumentException("credentialId is required");
        }
        if (tenantId == null || tenantId <= 0) {
            throw new IllegalArgumentException("tenantId must be positive");
        }
        if (identityTenantId == null || identityTenantId.isBlank()) {
            throw new IllegalArgumentException("identityTenantId is required");
        }
        appCodePatterns = Set.copyOf(appCodePatterns);
        envPatterns = Set.copyOf(envPatterns);
        bizCodePatterns = Set.copyOf(bizCodePatterns);
        allowedOperations = Set.copyOf(allowedOperations);
        namespacePatterns = namespacePatterns == null ? Set.of() : Set.copyOf(namespacePatterns);
        identityTenantId = identityTenantId.trim();
    }

    /** 返回当前已认证 RPC Principal。 / Returns the current authenticated RPC principal. */
    public static DdcServicePrincipal current() {
        DdcServicePrincipal principal = CONTEXT_KEY.get();
        if (principal == null) {
            throw new IllegalStateException(
                    "Authenticated Tianshu RPC principal is required");
        }
        return principal;
    }

    /** 将 Principal 绑定到指定 gRPC Context。 / Binds this principal to the supplied gRPC context. */
    public Context bind(Context context) {
        return Objects.requireNonNull(context, "context")
                .withValue(CONTEXT_KEY, this);
    }

    @Override
    public String getName() {
        return credentialId;
    }

    public DdcTenantContextDTO tenantContext() {
        return new DdcTenantContextDTO(tenantId, identityTenantId, "service:" + credentialId);
    }

    public String auditOperator(String requestedOperator) {
        String trusted = "service:" + credentialId;
        if (requestedOperator == null || requestedOperator.isBlank()) {
            return trusted;
        }
        String auditDetail = requestedOperator
                .replaceAll("[\\p{Cntrl}]", " ")
                .trim();
        if (auditDetail.length() > 128) {
            auditDetail = auditDetail.substring(0, 128);
        }
        return trusted + " [requested=" + auditDetail + ']';
    }
}
