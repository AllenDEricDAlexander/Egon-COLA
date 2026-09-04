package top.egon.cola.component.gateway.contract.runtime;

import java.util.Arrays;
import java.util.Optional;

/**
 * Gateway 可执行进程固定的数据面角色。
 *
 * <p>角色由可执行程序确定，并通过 DDC 元数据上报；不能由部署配置切换。
 * English summary: Defines the fixed engine roles reported through DDC metadata.
 */
public enum GatewayEngineRoleEnum {

    API_RPC,
    MCP;

    /**
     * 解析外部元数据中的角色；只去除首尾空格，缺失或未知值不推断为任一角色。
     * English summary: Parses case-sensitive role metadata without a fallback role.
     *
     * @param value DDC 角色元数据，允许为空；nullable DDC role metadata
     * @return 已知角色或空结果；known role or an empty result
     */
    public static Optional<GatewayEngineRoleEnum> fromWire(String value) {
        String normalized = value == null ? "" : value.trim();
        return Arrays.stream(values())
                .filter(role -> role.name().equals(normalized))
                .findFirst();
    }
}
