package top.egon.cola.component.rpc.consumer.provider;

import top.egon.cola.component.rpc.exception.EgonRpcErrorCode;
import top.egon.cola.component.rpc.exception.EgonRpcException;

import java.util.regex.Pattern;

/**
 * RPC Provider 的注册中心无关精确查询条件。
 *
 * <p>Registry-neutral exact selector for an RPC Provider service.
 */
public record RpcProviderQuery(
        String bizCode,
        String appCode,
        String env,
        String serviceName,
        String group,
        String version,
        String protocol
) {

    private static final Pattern SAFE_SEGMENT = Pattern.compile(
            "[A-Za-z0-9][A-Za-z0-9._:+-]{0,127}"
    );

    public RpcProviderQuery {
        bizCode = requiredSafe(bizCode, "biz code");
        appCode = requiredSafe(appCode, "app code");
        env = requiredSafe(env, "environment");
        serviceName = requiredSafe(serviceName, "service name");
        group = requiredSafe(group, "group");
        version = requiredSafe(version, "version");
        protocol = requiredSafe(protocol, "protocol");
        if (!"grpc".equals(protocol)) {
            throw invalid("RPC Provider protocol must be grpc");
        }
    }

    private static String requiredSafe(String value, String name) {
        if (value == null || value.isBlank()) {
            throw invalid("RPC Provider " + name + " is required");
        }
        String normalized = value.trim();
        if (!SAFE_SEGMENT.matcher(normalized).matches()) {
            throw invalid("RPC Provider " + name + " is invalid");
        }
        return normalized;
    }

    private static EgonRpcException invalid(String message) {
        return new EgonRpcException(
                EgonRpcErrorCode.RPC_INVALID_CONTRACT,
                message
        );
    }
}
