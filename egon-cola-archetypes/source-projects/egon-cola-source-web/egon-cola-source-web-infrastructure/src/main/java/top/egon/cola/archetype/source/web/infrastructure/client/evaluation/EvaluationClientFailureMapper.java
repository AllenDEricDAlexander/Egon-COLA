package top.egon.cola.archetype.source.web.infrastructure.client.evaluation;

import top.egon.cola.evaluation.facade.dto.SingleResponse;
import top.egon.cola.archetype.source.web.domain.client.ExternalDependencyException;
import top.egon.cola.archetype.source.web.domain.client.ExternalDependencyFailure;
import java.util.Locale;
import top.egon.cola.component.rpc.exception.EgonRpcException;

final class EvaluationClientFailureMapper {

    private static final String DEPENDENCY = "evaluation";

    private EvaluationClientFailureMapper() {
    }

    static <T> T requireData(SingleResponse<T> response, String operation) {
        if (response == null) {
            throw incompatible(operation);
        }
        if (!response.isSuccess()) {
            throw providerFailure(response.getCode());
        }
        if (response.getData() == null) {
            throw incompatible(operation);
        }
        return response.getData();
    }

    static ExternalDependencyException map(RuntimeException failure) {
        if (failure instanceof EgonRpcException rpcFailure) {
            ExternalDependencyFailure category = switch (rpcFailure.getCode()) {
                case RPC_DEADLINE_EXCEEDED -> ExternalDependencyFailure.TIMEOUT;
                case RPC_INVALID_CONTRACT, RPC_METHOD_NOT_FOUND -> ExternalDependencyFailure.CONTRACT_INCOMPATIBLE;
                default -> ExternalDependencyFailure.UNAVAILABLE;
            };
            return failure(category, rpcFailure.getCode().name(), rpcFailure);
        }
        return failure(ExternalDependencyFailure.SERVICE_FAILURE, "UNKNOWN", failure);
    }

    private static ExternalDependencyException providerFailure(String code) {
        return failure(category(code), code, null);
    }

    static ExternalDependencyException incompatible(String operation) {
        return new ExternalDependencyException(
                DEPENDENCY,
                ExternalDependencyFailure.CONTRACT_INCOMPATIBLE,
                "INVALID_RESPONSE",
                "evaluation dependency returned an invalid response for " + operation,
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
                "evaluation dependency failed: " + category.name(),
                cause);
    }
}
