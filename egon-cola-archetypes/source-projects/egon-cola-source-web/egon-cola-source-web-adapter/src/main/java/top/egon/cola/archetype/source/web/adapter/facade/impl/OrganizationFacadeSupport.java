package top.egon.cola.archetype.source.web.adapter.facade.impl;

import top.egon.cola.archetype.source.web.application.context.OrganizationRequestContext;
import top.egon.cola.archetype.source.web.application.context.OrganizationRequestContextHolder;
import top.egon.cola.archetype.source.web.common.exception.OrganizationApplicationException;
import top.egon.cola.component.rpc.context.invocation.RpcInvocationMetadata;
import top.egon.cola.platform.tianquan.shoubing.contract.ServiceIdentityPrincipal;
import top.egon.cola.platform.tianquan.shoubing.starter.security.rpc.IdpRpcSecurityContext;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/** Binds the request-scoped organization context around one inbound call. */
public final class OrganizationFacadeSupport {

    private OrganizationFacadeSupport() {
    }

    /** Builds the wire rejection envelope from the string code, message and trace the context holds. */
    @FunctionalInterface
    public interface RejectionMapper<T> {
        T map(String code, String message, String traceId);
    }

    public static String requestId() {
        String value = attachment("idempotency-key");
        return value == null || value.isBlank() ? UUID.randomUUID().toString() : value;
    }

    public static Long positiveId(String value, String field) {
        try {
            long parsed = Long.parseLong(value);
            if (parsed <= 0) {
                throw new IllegalArgumentException(field + " must be positive");
            }
            return parsed;
        } catch (NumberFormatException failure) {
            throw new IllegalArgumentException(field + " must be a positive long", failure);
        }
    }

    public static <T> T invoke(Supplier<T> action, RejectionMapper<T> rejection) {
        var verified = context();
        var previous = OrganizationRequestContextHolder.current();
        OrganizationRequestContextHolder.clear();
        OrganizationRequestContextHolder.set(verified);
        try {
            return action.get();
        } catch (OrganizationApplicationException failure) {
            String traceId = OrganizationRequestContextHolder.current()
                    .map(OrganizationRequestContext::traceId).orElse("unknown");
            return rejection.map(failure.code(), failure.getMessage(), traceId);
        } finally {
            OrganizationRequestContextHolder.clear();
            previous.ifPresent(OrganizationRequestContextHolder::set);
        }
    }

    private static OrganizationRequestContext context() {
        if (!(IdpRpcSecurityContext.currentPrincipalOrNull() instanceof ServiceIdentityPrincipal service)
                || service.tenantId() == null || service.subject().isBlank() || service.subject().length() > 128
                || !service.subject().equals(service.subject().strip())) {
            throw new AuthenticationCredentialsNotFoundException("Verified TENANT SERVICE identity is required");
        }
        String traceId = valueOrDefault(attachment("x-trace-id"), UUID.randomUUID().toString());
        return new OrganizationRequestContext(service.subject(), Set.of(), traceId);
    }

    private static String attachment(String name) {
        OrganizationRpcContextDTO context = OrganizationRpcContextDTO.current();
        String value = context == null ? null : switch (name) {
            case "idempotency-key" -> context.idempotencyKey();
            case "x-trace-id" -> context.traceId();
            default -> null;
        };
        RpcInvocationMetadata invocation = RpcInvocationMetadata.current();
        if ("x-trace-id".equals(name) && (value == null || value.isBlank())
                && invocation != null && invocation.traceContext() != null) {
            return invocation.traceId();
        }
        return value;
    }

    private static String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
