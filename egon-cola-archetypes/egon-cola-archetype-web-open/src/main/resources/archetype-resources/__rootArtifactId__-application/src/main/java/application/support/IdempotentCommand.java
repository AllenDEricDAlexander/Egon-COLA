package ${package}.application.support;

import ${package}.application.exceptions.OrganizationApplicationException;
import ${package}.application.exceptions.OrganizationFailureType;
import ${package}.domain.client.CommandIdempotencyPort;

import java.util.function.Supplier;

public final class IdempotentCommand {
    private IdempotentCommand() {}

    public static <T> T execute(
            CommandIdempotencyPort port, String operation, String requestId, Supplier<T> action) {
        if (!port.claim(operation, requestId)) {
            throw new OrganizationApplicationException(
                OrganizationFailureType.CONFLICT, "ORG_CONFLICT", "Duplicate command request");
        }
        try {
            return action.get();
        } catch (RuntimeException | Error failure) {
            port.release(operation, requestId);
            throw failure;
        }
    }

    public static void execute(
            CommandIdempotencyPort port, String operation, String requestId, Runnable action) {
        execute(port, operation, requestId, () -> { action.run(); return null; });
    }
}
