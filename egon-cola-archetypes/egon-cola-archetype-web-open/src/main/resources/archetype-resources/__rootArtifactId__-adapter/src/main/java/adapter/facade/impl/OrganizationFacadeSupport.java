package ${package}.adapter.facade.impl;

import ${package}.application.context.OrganizationRequestContext;
import ${package}.application.context.OrganizationRequestContextHolder;
import ${package}.application.exceptions.OrganizationApplicationException;
import top.egon.cola.organization.facade.exceptions.OrganizationFacadeException;
import org.apache.dubbo.rpc.RpcContext;

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
        return RpcContext.getServerAttachment().getAttachment(name);
    }

    private static String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
