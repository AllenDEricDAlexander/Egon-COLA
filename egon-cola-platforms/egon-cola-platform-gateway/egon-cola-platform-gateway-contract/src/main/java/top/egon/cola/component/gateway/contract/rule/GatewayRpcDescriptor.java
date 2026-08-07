package top.egon.cola.component.gateway.contract.rule;

/**
 * RPC provider 的 Protobuf Descriptor 集合及其内容摘要。
 *
 * <p>运行时据此解析服务、方法和消息结构；{@code sha256} 用于校验快照未被篡改。
 */
public record GatewayRpcDescriptor(
        String descriptorId,
        String sha256,
        String base64DescriptorSet
) {

    public GatewayRpcDescriptor {
        descriptorId = required(descriptorId, "descriptorId");
        sha256 = required(sha256, "sha256");
        base64DescriptorSet = required(
                base64DescriptorSet,
                "base64DescriptorSet"
        );
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
