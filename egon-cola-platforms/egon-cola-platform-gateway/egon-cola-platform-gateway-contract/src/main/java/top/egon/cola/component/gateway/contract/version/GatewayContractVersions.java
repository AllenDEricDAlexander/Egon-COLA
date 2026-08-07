package top.egon.cola.component.gateway.contract.version;

/**
 * 网关跨进程契约使用的独立版本号。
 *
 * <p>API、事件和规则分别演进，避免某一类消息升级时被迫同步升级所有消费者。
 */
public final class GatewayContractVersions {

    public static final String API_CONTRACT_V1 = "v1";

    public static final String EVENT_SCHEMA_V1 = "v1";

    public static final String RULE_SCHEMA_V1 = "v1";

    private GatewayContractVersions() {
    }
}
