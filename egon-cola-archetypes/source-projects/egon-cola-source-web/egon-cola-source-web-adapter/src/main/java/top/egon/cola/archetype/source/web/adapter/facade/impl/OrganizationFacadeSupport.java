package top.egon.cola.archetype.source.web.adapter.facade.impl;

import top.egon.cola.archetype.source.web.application.context.OrganizationRequestContext;
import top.egon.cola.archetype.source.web.application.context.OrganizationRequestContextHolder;
import top.egon.cola.archetype.source.web.application.exceptions.OrganizationApplicationException;
import top.egon.cola.organization.facade.exceptions.OrganizationFacadeException;
import top.egon.cola.component.rpc.context.invocation.RpcInvocationMetadata;

import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public final class OrganizationFacadeSupport {

    private OrganizationFacadeSupport() {
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

    public static void invoke(Runnable action) {
        invoke(() -> {
            action.run();
            return null;
        });
    }

    public static <T> T invoke(Supplier<T> action) {
        boolean created = OrganizationRequestContextHolder.current().isEmpty();
        if (created) {
            OrganizationRequestContextHolder.set(context());
        }
        try {
            return action.get();
        } catch (OrganizationApplicationException failure) {
            String traceId = OrganizationRequestContextHolder.current()
                    .map(OrganizationRequestContext::traceId).orElse("unknown");
            throw new OrganizationFacadeException(failure.code(), failure.getMessage(), traceId);
        } finally {
            if (created) {
                OrganizationRequestContextHolder.clear();
            }
        }
    }

    private static OrganizationRequestContext context() {
        String actorId = valueOrDefault(attachment("x-actor-id"), "facade-system");
        String traceId = valueOrDefault(attachment("x-trace-id"), UUID.randomUUID().toString());
        String roleHeader = attachment("x-actor-roles");
        Set<String> roles = roleHeader == null || roleHeader.isBlank()
                ? Set.of("SYSTEM")
                : Arrays.stream(roleHeader.split(","))
                        .map(String::trim)
                        .filter(role -> !role.isEmpty())
                        .collect(Collectors.toUnmodifiableSet());
        return new OrganizationRequestContext(actorId, roles, traceId);
    }

    private static String attachment(String name) {
        OrganizationRpcContextDTO context = OrganizationRpcContextDTO.current();
        String value = context == null ? null : switch (name) {
            case "idempotency-key" -> context.idempotencyKey();
            case "x-actor-id" -> context.actorId();
            case "x-actor-roles" -> context.actorRoles();
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
