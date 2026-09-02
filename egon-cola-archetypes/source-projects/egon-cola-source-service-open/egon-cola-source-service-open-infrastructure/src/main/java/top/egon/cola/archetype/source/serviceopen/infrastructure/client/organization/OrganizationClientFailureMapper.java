package top.egon.cola.archetype.source.serviceopen.infrastructure.client.organization;

import top.egon.cola.archetype.source.serviceopen.domain.client.ExternalDependencyException;
import top.egon.cola.archetype.source.serviceopen.domain.client.ExternalDependencyFailure;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.apache.dubbo.rpc.RpcException;

final class OrganizationClientFailureMapper {

    private static final String DEPENDENCY = "organization";

    private OrganizationClientFailureMapper() {
    }

    static ExternalDependencyException map(RuntimeException failure) {
        if (failure instanceof StatusRuntimeException statusFailure) {
            Status.Code code = statusFailure.getStatus().getCode();
            ExternalDependencyFailure category = switch (code) {
                case DEADLINE_EXCEEDED -> ExternalDependencyFailure.TIMEOUT;
                case UNAVAILABLE -> ExternalDependencyFailure.UNAVAILABLE;
                case NOT_FOUND -> ExternalDependencyFailure.NOT_FOUND;
                case INVALID_ARGUMENT -> ExternalDependencyFailure.VALIDATION_FAILED;
                case PERMISSION_DENIED, FAILED_PRECONDITION -> ExternalDependencyFailure.BUSINESS_REJECTED;
                default -> ExternalDependencyFailure.SERVICE_FAILURE;
            };
            String externalCode = statusFailure.getTrailers() == null
                    ? code.name() : statusFailure.getTrailers().get(
                    io.grpc.Metadata.Key.of("x-egon-error-code", io.grpc.Metadata.ASCII_STRING_MARSHALLER));
            return failure(category, externalCode == null ? code.name() : externalCode, statusFailure);
        }
        if (failure instanceof RpcException rpcFailure) {
            ExternalDependencyFailure category = rpcFailure.isTimeout()
                    ? ExternalDependencyFailure.TIMEOUT
                    : ExternalDependencyFailure.UNAVAILABLE;
            return failure(category, "DUBBO_" + rpcFailure.getCode(), rpcFailure);
        }
        return failure(ExternalDependencyFailure.SERVICE_FAILURE, "UNKNOWN", failure);
    }

    static ExternalDependencyException incompatible(String operation) {
        return new ExternalDependencyException(
                DEPENDENCY,
                ExternalDependencyFailure.CONTRACT_INCOMPATIBLE,
                "NULL_RESPONSE",
                "organization dependency returned an invalid response for " + operation,
                null);
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
