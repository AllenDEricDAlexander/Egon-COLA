package top.egon.cola.archetype.source.web.adapter.facade.impl;

import io.grpc.Context;

/** Carries the existing organization metadata within one gRPC invocation. */
public record OrganizationRpcContextDTO(String actorId, String actorRoles, String traceId, String idempotencyKey) {
    public static final Context.Key<OrganizationRpcContextDTO> CONTEXT_KEY = Context.key("organization-rpc-context");

    public static OrganizationRpcContextDTO current() {
        return CONTEXT_KEY.get();
    }
}
