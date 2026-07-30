package top.egon.cola.component.gateway.engine.traffic;

import java.util.Map;
import java.util.Objects;

public record GatewayTrafficContext(
        String operationId,
        String routeId,
        String applicationCode,
        String callerId,
        String clientIp,
        String providerService,
        String providerInstance,
        Map<String, String> approvedHeaders,
        Map<String, String> pathVariables,
        Map<String, String> queryParameters
) {

    public GatewayTrafficContext {
        approvedHeaders = Map.copyOf(Objects.requireNonNull(
                approvedHeaders,
                "approvedHeaders"
        ));
        pathVariables = Map.copyOf(Objects.requireNonNull(
                pathVariables,
                "pathVariables"
        ));
        queryParameters = Map.copyOf(Objects.requireNonNull(
                queryParameters,
                "queryParameters"
        ));
    }

    String value(String field) {
        return switch (field) {
            case "operationId" -> operationId;
            case "routeId" -> routeId;
            case "applicationCode" -> applicationCode;
            case "callerId" -> callerId;
            case "clientIp" -> clientIp;
            case "providerService" -> providerService;
            case "providerInstance" -> providerInstance;
            default -> dynamic(field);
        };
    }

    private String dynamic(String field) {
        int separator = field.indexOf('.');
        if (separator < 1 || separator == field.length() - 1) {
            throw new IllegalArgumentException(
                    "unsupported traffic key field " + field
            );
        }
        String namespace = field.substring(0, separator);
        String name = field.substring(separator + 1);
        return switch (namespace) {
            case "header" -> approvedHeaders.get(name);
            case "path" -> pathVariables.get(name);
            case "query" -> queryParameters.get(name);
            default -> throw new IllegalArgumentException(
                    "unsupported traffic key namespace " + namespace
            );
        };
    }
}
