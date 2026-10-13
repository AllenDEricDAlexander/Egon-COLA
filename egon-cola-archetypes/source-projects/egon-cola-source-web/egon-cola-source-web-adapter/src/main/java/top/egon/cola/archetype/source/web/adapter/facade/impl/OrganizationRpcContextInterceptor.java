package top.egon.cola.archetype.source.web.adapter.facade.impl;

import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** Carries correlation/idempotency metadata; the shared SDK owns verified SERVICE identity. */
@Component("organizationRpcContextInterceptor")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Slf4j
public class OrganizationRpcContextInterceptor implements ServerInterceptor {
    private static final Metadata.Key<String> TRACE_ID = Metadata.Key.of("x-trace-id", Metadata.ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> IDEMPOTENCY_KEY = Metadata.Key.of("idempotency-key", Metadata.ASCII_STRING_MARSHALLER);

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        var metadata = new OrganizationRpcContextDTO(null, null,
                headers.get(TRACE_ID), headers.get(IDEMPOTENCY_KEY));
        return Contexts.interceptCall(Context.current().withValue(OrganizationRpcContextDTO.CONTEXT_KEY, metadata),
                call, headers, next);
    }
}
