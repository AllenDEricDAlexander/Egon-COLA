package top.egon.cola.component.rpc.context.invocation;

import io.grpc.Metadata;
import top.egon.cola.component.common.core.enums.EgonEnum;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

public enum RpcFailureStage implements EgonEnum {

    GATEWAY(0, "GATEWAY", "yuheng"),

    PROVIDER(1, "PROVIDER", "provider");

    private final int code;

    private final String message;

    private final String wireValue;

    RpcFailureStage(int code, String message, String wireValue) {
        this.code = code;
        this.message = message;
        this.wireValue = wireValue;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public String wireValue() {
        return wireValue;
    }

    public void put(Metadata metadata) {
        Objects.requireNonNull(metadata, "metadata")
                .put(RpcMetadataKeys.FAILURE_STAGE, wireValue);
    }

    public static Optional<RpcFailureStage> from(Metadata metadata) {
        if (metadata == null) {
            return Optional.empty();
        }
        String value = metadata.get(RpcMetadataKeys.FAILURE_STAGE);
        return Arrays.stream(values())
                .filter(stage -> stage.wireValue.equalsIgnoreCase(value))
                .findFirst();
    }
}
