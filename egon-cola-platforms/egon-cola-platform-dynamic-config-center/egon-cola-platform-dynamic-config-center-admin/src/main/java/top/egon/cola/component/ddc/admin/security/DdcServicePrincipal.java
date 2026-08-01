package top.egon.cola.component.ddc.admin.security;

import java.security.Principal;
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

    public static final String REQUEST_ATTRIBUTE =
            DdcServicePrincipal.class.getName();

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
