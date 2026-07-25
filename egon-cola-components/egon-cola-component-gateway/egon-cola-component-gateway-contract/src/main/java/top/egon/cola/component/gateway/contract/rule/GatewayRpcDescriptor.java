package top.egon.cola.component.gateway.contract.rule;

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
