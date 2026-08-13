package top.egon.cola.component.rpc.context.invocation;

import io.grpc.Metadata;

public final class RpcMetadataKeys {

    public static final Metadata.Key<String> SERVICE =
            ascii("x-egon-rpc-service");

    public static final Metadata.Key<String> GROUP =
            ascii("x-egon-rpc-group");

    public static final Metadata.Key<String> VERSION =
            ascii("x-egon-rpc-version");

    public static final Metadata.Key<String> INVOCATION_ID =
            ascii("x-egon-rpc-invocation-id");

    public static final Metadata.Key<String> SOURCE_APP =
            ascii("x-egon-rpc-source-app");

    public static final Metadata.Key<String> SOURCE_INSTANCE =
            ascii("x-egon-rpc-source-instance");

    public static final Metadata.Key<String> REQUEST_ID =
            ascii("x-egon-request-id");

    public static final Metadata.Key<String> TRACEPARENT =
            ascii("traceparent");

    public static final Metadata.Key<String> TRACESTATE =
            ascii("tracestate");

    public static final Metadata.Key<String> FAILURE_STAGE =
            ascii("x-egon-rpc-failure-stage");

    private RpcMetadataKeys() {
    }

    private static Metadata.Key<String> ascii(String name) {
        return Metadata.Key.of(name, Metadata.ASCII_STRING_MARSHALLER);
    }
}
