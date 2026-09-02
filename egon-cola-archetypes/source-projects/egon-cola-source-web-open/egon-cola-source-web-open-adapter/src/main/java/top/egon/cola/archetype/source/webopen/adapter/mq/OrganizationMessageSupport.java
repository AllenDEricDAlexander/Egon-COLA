package top.egon.cola.archetype.source.webopen.adapter.mq;

import top.egon.cola.archetype.source.webopen.application.context.OrganizationRequestContext;
import top.egon.cola.archetype.source.webopen.application.context.OrganizationRequestContextHolder;
import top.egon.cola.archetype.source.webopen.application.exceptions.OrganizationApplicationException;
import top.egon.cola.archetype.source.webopen.application.exceptions.OrganizationFailureType;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import top.egon.cola.component.common.id.generator.LongIdGenerator;

import java.util.Set;

@Component
@RequiredArgsConstructor
public final class OrganizationMessageSupport {
    private final LongIdGenerator idGenerator;

    public void consume(Runnable action) {
        OrganizationRequestContextHolder.set(new OrganizationRequestContext(
                "rabbit-system", Set.of("SYSTEM"), Long.toString(idGenerator.nextLongId())));
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
