package top.egon.cola.component.gateway.contract.rule;

import top.egon.cola.component.gateway.contract.protocol.GatewayProtocol;

import java.util.Objects;

/**
 * 运行时操作对应的 provider 服务定位信息。
 *
 * <p>它把 DDC 业务、应用、环境、命名空间和协议服务名组合成 Engine 可解析的目标；旧快照
 * 可暂时缺少业务和应用编码，但新发布内容应显式提供完整范围。
 */
public record GatewayProviderServiceRef(
        String bizCode,
        String appCode,
        String env,
        String namespace,
        GatewayProtocol protocol,
        String serviceName,
        String group,
        String version,
        String transport
) {

    public GatewayProviderServiceRef {
        // 仅为读取历史快照保留空值；新发布必须提供业务和应用编码，Engine 会拒绝未重新发布的旧快照。
        bizCode = optional(bizCode);
        appCode = optional(appCode);
        env = required(env, "env");
        namespace = required(namespace, "namespace");
        protocol = Objects.requireNonNull(protocol, "protocol");
        serviceName = required(serviceName, "serviceName");
        group = required(group, "group");
        version = required(version, "version");
        transport = required(transport, "transport");
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    private static String optional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
