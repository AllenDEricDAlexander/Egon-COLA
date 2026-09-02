package top.egon.cola.archetype.source.webopen.infrastructure.client.evaluation;

import top.egon.cola.archetype.source.webopen.domain.client.ExternalDependencyException;
import top.egon.cola.archetype.source.webopen.domain.client.ExternalDependencyFailure;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.util.Locale;

final class EvaluationClientFailureMapper {

    private static final String DEPENDENCY = "evaluation";

    private EvaluationClientFailureMapper() {
    }

    static ExternalDependencyException map(RuntimeException failure) {
        if (failure instanceof StatusRuntimeException statusFailure) {
            return failure(category(statusFailure.getStatus().getCode()),
                    "GRPC_" + statusFailure.getStatus().getCode().name(), statusFailure);
        }
        return failure(ExternalDependencyFailure.SERVICE_FAILURE, "UNKNOWN", failure);
    }

    static ExternalDependencyException incompatible(String operation) {
        return failure(
                ExternalDependencyFailure.CONTRACT_INCOMPATIBLE,
                "INVALID_RESPONSE",
                new IllegalStateException("evaluation dependency returned an invalid response for " + operation));
    }

    private static ExternalDependencyFailure category(Status.Code code) {
        return switch (code) {
            case NOT_FOUND -> ExternalDependencyFailure.NOT_FOUND;
            case INVALID_ARGUMENT, FAILED_PRECONDITION -> ExternalDependencyFailure.VALIDATION_FAILED;
            case ALREADY_EXISTS, PERMISSION_DENIED -> ExternalDependencyFailure.BUSINESS_REJECTED;
            case DEADLINE_EXCEEDED -> ExternalDependencyFailure.TIMEOUT;
            case UNAVAILABLE -> ExternalDependencyFailure.UNAVAILABLE;
            default -> ExternalDependencyFailure.SERVICE_FAILURE;
        };
    }

    private static ExternalDependencyFailure category(String code) {
        String normalized = code == null ? "" : code.toUpperCase(Locale.ROOT);
        if (normalized.contains("NOT_FOUND")) {
            return ExternalDependencyFailure.NOT_FOUND;
        }
        if (normalized.contains("INVALID") || normalized.contains("FAILED_PRECONDITION")) {
            return ExternalDependencyFailure.VALIDATION_FAILED;
        }
        return ExternalDependencyFailure.SERVICE_FAILURE;
    }

    private static ExternalDependencyException failure(
            ExternalDependencyFailure category, String externalCode, RuntimeException cause) {
        return new ExternalDependencyException(
                DEPENDENCY,
                category,
                externalCode,
                "evaluation dependency failed: " + category.name(),
                cause);
    }
}
