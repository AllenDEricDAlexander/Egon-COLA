package top.egon.cola.component.rpc.context;

import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import top.egon.cola.component.common.id.uuid.UuidV7;

import java.util.regex.Pattern;

public class RpcProviderServerInterceptor implements ServerInterceptor {

    private static final int MAX_VALUE_LENGTH = 256;

    private static final Pattern SAFE_VALUE =
            Pattern.compile("[\\x20-\\x7e]{1," + MAX_VALUE_LENGTH + "}");

    private static final Pattern TRACE_ID =
            Pattern.compile("[0-9a-fA-F]{16,64}");

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {
        String traceId = validTrace(headers.get(RpcMetadataKeys.TRACE_ID));
        RpcInvocationMetadata invocation = new RpcInvocationMetadata(
                safe(headers.get(RpcMetadataKeys.SERVICE)),
                safe(headers.get(RpcMetadataKeys.GROUP)),
                safe(headers.get(RpcMetadataKeys.VERSION)),
                safe(headers.get(RpcMetadataKeys.INVOCATION_ID)),
                safe(headers.get(RpcMetadataKeys.SOURCE_APP)),
                safe(headers.get(RpcMetadataKeys.SOURCE_INSTANCE)),
                traceId,
                safe(headers.get(RpcMetadataKeys.TRACEPARENT)),
                safe(headers.get(RpcMetadataKeys.TRACESTATE))
        );
        Context context = Context.current().withValue(
                RpcInvocationMetadata.CONTEXT_KEY,
                invocation
        );
        return Contexts.interceptCall(context, call, headers, next);
    }

    private String safe(String value) {
        return value != null && SAFE_VALUE.matcher(value).matches()
                ? value
                : null;
    }

    private String validTrace(String value) {
        return value != null && TRACE_ID.matcher(value).matches()
                ? value.toLowerCase(java.util.Locale.ROOT)
                : UuidV7.simpleString();
    }
}
