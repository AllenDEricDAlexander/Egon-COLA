package top.egon.cola.component.rpc.context;

import io.grpc.Metadata;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

public enum RpcFailureStage {

    GATEWAY("gateway"),

    PROVIDER("provider");

    private final String wireValue;

    RpcFailureStage(String wireValue) {
        this.wireValue = wireValue;
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
