package top.egon.cola.component.gateway.engine.traffic;

public record GatewayResourceLimits(
        int maximumQueryParameters,
        int maximumPathSegments,
        int maximumMetadataBytes,
        long maximumBodyBytes,
        int maximumRpcMessageBytes
) {

    public GatewayResourceLimits {
        if (maximumQueryParameters < 1
                || maximumPathSegments < 1
                || maximumMetadataBytes < 1
                || maximumBodyBytes < 1
                || maximumRpcMessageBytes < 1) {
            throw new IllegalArgumentException(
                    "resource limits must be positive"
            );
        }
    }
}
