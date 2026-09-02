package top.egon.cola.archetype.source.webopen.adapter.facade.impl;

import top.egon.cola.archetype.source.webopen.application.context.OrganizationRequestContext;
import top.egon.cola.archetype.source.webopen.application.context.OrganizationRequestContextHolder;
import top.egon.cola.archetype.source.webopen.application.exceptions.OrganizationApplicationException;
import top.egon.cola.archetype.source.webopen.application.exceptions.OrganizationFailureType;
import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.rpc.RpcContext;
import org.springframework.stereotype.Component;
import top.egon.cola.component.common.id.generator.LongIdGenerator;

/** Shared request-context and stable gRPC error mapping for organization Triple providers. */
@Component
@RequiredArgsConstructor
public final class OrganizationFacadeSupport {

    private static final Metadata.Key<String> ERROR_CODE = Metadata.Key.of(
            "x-egon-error-code", Metadata.ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> TRACE_ID = Metadata.Key.of(
            "x-egon-trace-id", Metadata.ASCII_STRING_MARSHALLER);

    private final LongIdGenerator idGenerator;

    public String requestId() {
        String value = attachment("idempotency-key");
        return value == null || value.isBlank() ? Long.toString(idGenerator.nextLongId()) : value;
    }

    public static long positiveId(long value, String field) {
        if (value <= 0) {
            throw new IllegalArgumentException(field + " must be a positive Long");
        }
        return value;
    }

    public void invoke(Runnable action) {
        invoke(() -> {
            action.run();
            return null;
        });
    }

    public <T> T invoke(Supplier<T> action) {
        boolean created = OrganizationRequestContextHolder.current().isEmpty();
        if (created) {
            OrganizationRequestContextHolder.set(context());
        }
        try {
            return action.get();
        } catch (StatusRuntimeException failure) {
            throw failure;
        } catch (OrganizationApplicationException failure) {
            throw toStatus(failure);
        } catch (IllegalArgumentException failure) {
            throw Status.INVALID_ARGUMENT.withDescription(
                    Objects.requireNonNullElse(failure.getMessage(), "invalid organization request"))
                    .asRuntimeException(metadata("VALIDATION_FAILED"));
        } catch (RuntimeException failure) {
            throw Status.INTERNAL.withDescription("organization request failed")
                    .asRuntimeException(metadata("INTERNAL_ERROR"));
        } finally {
            if (created) {
                OrganizationRequestContextHolder.clear();
            }
        }
    }

    public StatusRuntimeException toStatus(OrganizationApplicationException failure) {
        Objects.requireNonNull(failure, "failure");
        String traceId = OrganizationRequestContextHolder.current()
                .map(OrganizationRequestContext::traceId)
                .orElseGet(() -> Long.toString(idGenerator.nextLongId()));
        Metadata metadata = metadata(failure.code());
        metadata.put(TRACE_ID, traceId);
        return status(failure.failureType()).withDescription(
                        Objects.requireNonNullElse(failure.getMessage(), "organization request failed"))
                .asRuntimeException(metadata);
    }

    private OrganizationRequestContext context() {
        String actorId = valueOrDefault(attachment("x-actor-id"), "facade-system");
        String traceId = valueOrDefault(attachment("x-trace-id"), Long.toString(idGenerator.nextLongId()));
        String roleHeader = attachment("x-actor-roles");
        Set<String> roles = roleHeader == null || roleHeader.isBlank()
                ? Set.of("SYSTEM")
                : Arrays.stream(roleHeader.split(","))
                        .map(String::trim)
                        .filter(role -> !role.isEmpty())
                        .collect(Collectors.toUnmodifiableSet());
        return new OrganizationRequestContext(actorId, roles, traceId);
    }

    private static Metadata metadata(String code) {
        Metadata metadata = new Metadata();
        metadata.put(ERROR_CODE, code == null || code.isBlank() ? "INTERNAL_ERROR" : code);
        return metadata;
    }

    private static Status status(OrganizationFailureType type) {
        return switch (type) {
            case VALIDATION -> Status.INVALID_ARGUMENT;
            case FORBIDDEN -> Status.PERMISSION_DENIED;
            case NOT_FOUND -> Status.NOT_FOUND;
            case CONFLICT -> Status.ALREADY_EXISTS;
            case DOMAIN_REJECTED -> Status.FAILED_PRECONDITION;
            case DEPENDENCY_UNAVAILABLE -> Status.UNAVAILABLE;
            case INTERNAL -> Status.INTERNAL;
        };
    }

    private static String attachment(String name) {
        return RpcContext.getServerAttachment().getAttachment(name);
    }

    private static String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
