package top.egon.cola.archetype.source.service.infrastructure.client.organization;

import top.egon.cola.archetype.source.service.domain.client.ExternalDependencyException;
import top.egon.cola.archetype.source.service.domain.client.ExternalDependencyFailure;
import java.util.Locale;
import top.egon.cola.component.rpc.common.exception.EgonRpcException;

final class OrganizationClientFailureMapper {

    private static final String DEPENDENCY = "organization";

    private OrganizationClientFailureMapper() {
    }

    static ExternalDependencyException map(RuntimeException failure) {
        if (failure instanceof EgonRpcException rpcFailure) {
            ExternalDependencyFailure category = switch (rpcFailure.getRpcErrorCode()) {
                case RPC_DEADLINE_EXCEEDED -> ExternalDependencyFailure.TIMEOUT;
                case RPC_INVALID_CONTRACT, RPC_METHOD_NOT_FOUND -> ExternalDependencyFailure.CONTRACT_INCOMPATIBLE;
                default -> ExternalDependencyFailure.UNAVAILABLE;
            };
            return failure(category, rpcFailure.getRpcErrorCode().name(), rpcFailure);
        }
        return failure(ExternalDependencyFailure.SERVICE_FAILURE, "UNKNOWN", failure);
    }

    /** A provider rejection keeps only its wire string code; remote details stay sanitized out. */
    static ExternalDependencyException rejected(String code) {
        return failure(category(code), code, null);
    }

    static ExternalDependencyException incompatible(String operation) {
        return new ExternalDependencyException(
                DEPENDENCY,
                ExternalDependencyFailure.CONTRACT_INCOMPATIBLE,
                "NULL_RESPONSE",
                "organization dependency returned an invalid response for " + operation,
                null);
    }

    private static ExternalDependencyFailure category(String code) {
        String normalized = code == null ? "" : code.toUpperCase(Locale.ROOT);
        if (normalized.contains("NOT_FOUND")) {
            return ExternalDependencyFailure.NOT_FOUND;
        }
        if (normalized.contains("VALIDATION") || normalized.contains("INVALID")) {
            return ExternalDependencyFailure.VALIDATION_FAILED;
        }
        if (normalized.contains("CONFLICT")
                || normalized.contains("FORBIDDEN")
                || normalized.contains("REJECTED")) {
            return ExternalDependencyFailure.BUSINESS_REJECTED;
        }
        return ExternalDependencyFailure.SERVICE_FAILURE;
    }

    private static ExternalDependencyException failure(
            ExternalDependencyFailure category,
            String externalCode,
            RuntimeException cause) {
        return new ExternalDependencyException(
                DEPENDENCY,
                category,
                externalCode,
                "organization dependency failed: " + category.name(),
                cause);
    }
}
