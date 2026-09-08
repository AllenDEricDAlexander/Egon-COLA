package top.egon.cola.archetype.source.web.adapter.facade.impl;

import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Binds legacy organization header semantics to gRPC's managed context lifecycle. */
@Component("organizationRpcContextInterceptor")
@RequiredArgsConstructor
@Slf4j
public class OrganizationRpcContextInterceptor implements ServerInterceptor {
    private static final Metadata.Key<String> ACTOR_ID = Metadata.Key.of("x-actor-id", Metadata.ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> ACTOR_ROLES = Metadata.Key.of("x-actor-roles", Metadata.ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> TRACE_ID = Metadata.Key.of("x-trace-id", Metadata.ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> IDEMPOTENCY_KEY = Metadata.Key.of("idempotency-key", Metadata.ASCII_STRING_MARSHALLER);

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        var metadata = new OrganizationRpcContextDTO(headers.get(ACTOR_ID), headers.get(ACTOR_ROLES),
                headers.get(TRACE_ID), headers.get(IDEMPOTENCY_KEY));
        return Contexts.interceptCall(Context.current().withValue(OrganizationRpcContextDTO.CONTEXT_KEY, metadata),
                call, headers, next);
    }
}
