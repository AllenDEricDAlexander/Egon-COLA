package ${package}.adapter.mq;

import ${package}.application.context.OrganizationRequestContext;
import ${package}.application.context.OrganizationRequestContextHolder;
import ${package}.application.exceptions.OrganizationApplicationException;
import ${package}.application.exceptions.OrganizationFailureType;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;

import java.util.Set;
import java.util.UUID;

public final class OrganizationMessageSupport {

    private OrganizationMessageSupport() {
    }

    public static void consume(Runnable action) {
        OrganizationRequestContextHolder.set(new OrganizationRequestContext(
                "rabbit-system", Set.of("SYSTEM"), UUID.randomUUID().toString()));
        try {
            action.run();
        } catch (OrganizationApplicationException failure) {
            classify(failure);
        } catch (RuntimeException failure) {
            throw new RetryableOrganizationMessageException("Organization message handling failed", failure);
        } finally {
            OrganizationRequestContextHolder.clear();
        }
    }

    private static void classify(OrganizationApplicationException failure) {
        if (failure.failureType() == OrganizationFailureType.CONFLICT) {
            return;
        }
        if (failure.failureType() == OrganizationFailureType.DEPENDENCY_UNAVAILABLE
                || failure.failureType() == OrganizationFailureType.INTERNAL) {
            throw new RetryableOrganizationMessageException(failure.getMessage(), failure);
        }
        throw new AmqpRejectAndDontRequeueException(failure.getMessage(), failure);
    }
}
