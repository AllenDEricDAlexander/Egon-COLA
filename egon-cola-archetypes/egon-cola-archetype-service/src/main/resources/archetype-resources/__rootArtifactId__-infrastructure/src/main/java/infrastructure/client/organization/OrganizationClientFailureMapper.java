#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.client.organization;

import ${organizationFacadePackage}.facade.exceptions.OrganizationFacadeException;
import ${package}.domain.client.ExternalDependencyException;
import ${package}.domain.client.ExternalDependencyFailure;
import java.util.Locale;
import org.apache.dubbo.rpc.RpcException;

final class OrganizationClientFailureMapper {

    private static final String DEPENDENCY = "organization";

    private OrganizationClientFailureMapper() {
    }

    static ExternalDependencyException map(RuntimeException failure) {
        if (failure instanceof OrganizationFacadeException facadeFailure) {
            String code = facadeFailure.code();
            return failure(category(code), code, facadeFailure);
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
