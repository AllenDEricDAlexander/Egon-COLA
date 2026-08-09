package top.egon.cola.component.ddc.admin.security.rpc;

import io.grpc.Context;

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
        String bizCode
) implements Principal {

    static final Context.Key<DdcServicePrincipal> CONTEXT_KEY =
            Context.key("egon-ddc-service-principal");

    public static final String REQUEST_ATTRIBUTE =
            DdcServicePrincipal.class.getName();

    /** 返回当前已认证 RPC Principal。 / Returns the current authenticated RPC principal. */
    public static DdcServicePrincipal current() {
        DdcServicePrincipal principal = CONTEXT_KEY.get();
        if (principal == null) {
            throw new IllegalStateException(
                    "Authenticated DDC RPC principal is required");
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
