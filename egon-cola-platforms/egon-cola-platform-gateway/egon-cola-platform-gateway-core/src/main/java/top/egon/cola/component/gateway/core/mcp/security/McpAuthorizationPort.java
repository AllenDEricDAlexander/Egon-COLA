package top.egon.cola.component.gateway.core.mcp.security;

import org.reactivestreams.Publisher;

import java.util.Objects;

@FunctionalInterface
public interface McpAuthorizationPort {

    Publisher<Decision> authorize(McpAuthorizationRequest request);

    record Decision(
            boolean allowed,
            String reasonCode,
            long authVersion,
            long contextVersion,
            long policyVersion
    ) {

        public Decision {
            reasonCode = required(reasonCode, "reasonCode");
            nonNegative(authVersion, "authVersion");
            nonNegative(contextVersion, "contextVersion");
            nonNegative(policyVersion, "policyVersion");
        }

        public static Decision allowed(
                long authVersion,
                long contextVersion,
                long policyVersion) {
            return new Decision(
                    true,
                    "RBAC3_PERMISSION_ALLOWED",
                    authVersion,
                    contextVersion,
                    policyVersion
            );
        }

        public static Decision denied(
                String reasonCode,
                long authVersion,
                long contextVersion,
                long policyVersion) {
            return new Decision(
                    false,
                    reasonCode,
                    authVersion,
                    contextVersion,
                    policyVersion
            );
        }

        private static void nonNegative(long value, String field) {
            if (value < 0L) {
                throw new IllegalArgumentException(
                        field + " must not be negative"
                );
            }
        }

        private static String required(String value, String field) {
            Objects.requireNonNull(value, field);
            if (value.isBlank()) {
                throw new IllegalArgumentException(field + " is required");
            }
            return value.trim();
        }
    }
}
