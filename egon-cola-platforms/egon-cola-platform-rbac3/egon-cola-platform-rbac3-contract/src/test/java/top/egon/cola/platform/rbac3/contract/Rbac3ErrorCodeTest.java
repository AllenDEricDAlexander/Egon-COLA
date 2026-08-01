package top.egon.cola.platform.rbac3.contract;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.rbac3.contract.error.Rbac3ErrorCode;
import top.egon.cola.platform.rbac3.contract.error.Rbac3ErrorResponse;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class Rbac3ErrorCodeTest {

    @Test
    void exposesEveryStableSectionTwentySevenErrorCode() {
        Set<String> codes = Arrays.stream(Rbac3ErrorCode.values())
                .map(Enum::name)
                .collect(Collectors.toSet());

        assertEquals(Set.of(
                "REQUEST_INVALID",
                "AUTHENTICATION_REQUIRED",
                "AUTHENTICATION_FAILED",
                "TOKEN_INVALID",
                "SESSION_INVALIDATED",
                "AUTH_VERSION_MISMATCH",
                "SESSION_VERSION_MISMATCH",
                "POLICY_VERSION_MISMATCH",
                "PERMISSION_DENIED",
                "MANAGEMENT_POLICY_DENIED",
                "MANAGED_USER_SCOPE_DENIED",
                "MANAGED_ROLE_SCOPE_DENIED",
                "MANAGEMENT_OPERATION_DENIED",
                "PRIVILEGED_ROLE_MANAGEMENT_DENIED",
                "SELF_PRIVILEGE_ESCALATION_DENIED",
                "ROLE_ACTIVATION_NOT_ELIGIBLE",
                "SSD_CONSTRAINT_VIOLATION",
                "OPERATION_SOD_VIOLATION",
                "DATA_SCOPE_DENIED",
                "FIELD_ACCESS_DENIED",
                "RESOURCE_MANIFEST_CONFLICT",
                "ROLE_PREREQUISITE_NOT_MET",
                "ROLE_CARDINALITY_EXCEEDED",
                "ROLE_INHERITANCE_CYCLE",
                "ASSIGNMENT_TIME_OVERLAP",
                "ROLE_ACTIVATION_REQUIRED",
                "ROLE_ACTIVATION_ROOT_AMBIGUOUS",
                "APP_ROLE_ACTIVATION_MUTEX_VIOLATION",
                "ROLE_ACTIVATION_VERSION_CONFLICT",
                "IDEMPOTENCY_CONFLICT",
                "ROLE_ACTIVATION_SET_INVALID",
                "ACTIVE_ROLE_ROOT_LIMIT_EXCEEDED",
                "ROLE_FAMILY_SIZE_LIMIT_EXCEEDED",
                "AUTH_RUNTIME_UNAVAILABLE",
                "AUTH_PROPAGATION_PENDING",
                "TENANT_CONTEXT_INVALID",
                "DIRECTORY_SNAPSHOT_INVALID",
                "USER_LOCKED",
                "REFRESH_TOKEN_REUSED",
                "STEP_UP_REQUIRED",
                "SERVICE_IDENTITY_DENIED",
                "APPLICATION_BINDING_DENIED",
                "RESOURCE_NOT_FOUND",
                "RESOURCE_VERSION_CONFLICT",
                "DIRECTORY_SNAPSHOT_CONFLICT",
                "DIRECTORY_SNAPSHOT_STALE",
                "ROLE_DISABLED",
                "AUTH_MUTATION_CONFLICT",
                "INVALID_STATE_TRANSITION",
                "ROLE_APPLICATION_MISMATCH",
                "MANAGEMENT_POLICY_INCOMPLETE",
                "RESOURCE_MANIFEST_INVALID",
                "RATE_LIMITED",
                "AUTH_SNAPSHOT_NOT_READY",
                "SIGNING_KEY_UNAVAILABLE",
                "DIRECTORY_RUNTIME_UNAVAILABLE"
        ), codes);
    }

    @Test
    void stableCodesCarryTheirHttpAndRetrySemantics() {
        assertEquals(
                409,
                Rbac3ErrorCode.ROLE_ACTIVATION_VERSION_CONFLICT
                        .httpStatus()
        );
        assertEquals(
                true,
                Rbac3ErrorCode.ROLE_ACTIVATION_VERSION_CONFLICT.retryable()
        );
        assertEquals(
                503,
                Rbac3ErrorCode.AUTH_PROPAGATION_PENDING.httpStatus()
        );
        assertEquals(
                true,
                Rbac3ErrorCode.AUTH_PROPAGATION_PENDING.retryable()
        );
        assertEquals(
                false,
                Rbac3ErrorCode.ROLE_APPLICATION_MISMATCH.retryable()
        );
        assertEquals(422, Rbac3ErrorCode.ACTIVE_ROLE_ROOT_LIMIT_EXCEEDED.httpStatus());
        assertEquals(false, Rbac3ErrorCode.ACTIVE_ROLE_ROOT_LIMIT_EXCEEDED.retryable());
    }

    @Test
    void errorEnvelopeCannotOverrideStableRetrySemantics() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Rbac3ErrorResponse.Error(
                        Rbac3ErrorCode.PERMISSION_DENIED,
                        "Permission denied",
                        true,
                        List.of()
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new Rbac3ErrorResponse.Error(
                        Rbac3ErrorCode.AUTH_RUNTIME_UNAVAILABLE,
                        "Authorization runtime unavailable",
                        false,
                        List.of()
                )
        );
    }
}
