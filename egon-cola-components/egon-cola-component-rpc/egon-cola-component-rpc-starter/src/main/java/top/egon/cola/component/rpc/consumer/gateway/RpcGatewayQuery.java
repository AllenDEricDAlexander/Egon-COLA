package top.egon.cola.component.rpc.consumer.gateway;

/**
 * 内部 RPC Gateway 服务的注册中心无关查询条件。
 *
 * <p>Registry-neutral selector for an internal RPC Gateway service.
 */
public record RpcGatewayQuery(
        String env,
        String bizCode,
        String appCode,
        String serviceName,
        String group,
        String version
) {

    public RpcGatewayQuery {
        env = required(env, "environment");
        serviceName = required(serviceName, "service name");
        group = required(group, "group");
        version = required(version, "version");
        bizCode = optional(bizCode);
        appCode = optional(appCode);
        if ((bizCode == null) != (appCode == null)) {
            throw new IllegalArgumentException(
                    "RPC Gateway biz-code and app-code must be configured together"
            );
        }
    }

    private static String required(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "RPC Gateway " + name + " is required"
            );
        }
        return value.trim();
    }

    private static String optional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
