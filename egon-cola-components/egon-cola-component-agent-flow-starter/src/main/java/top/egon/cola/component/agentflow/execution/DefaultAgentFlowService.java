package top.egon.cola.component.agentflow.execution;

import com.google.adk.events.Event;
import com.google.adk.runner.InMemoryRunner;
import com.google.adk.sessions.Session;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import io.reactivex.rxjava3.core.Flowable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import top.egon.cola.component.agentflow.api.AgentFlowDescriptorDTO;
import top.egon.cola.component.agentflow.api.AgentFlowExecutionCommand;
import top.egon.cola.component.agentflow.api.AgentFlowService;
import top.egon.cola.component.agentflow.api.AgentFlowSessionCommand;
import top.egon.cola.component.agentflow.api.AgentFlowSessionResult;
import top.egon.cola.component.agentflow.autoconfigure.AgentFlowProperties;
import top.egon.cola.component.agentflow.config.AgentFlowSessionValidationGroup;
import top.egon.cola.component.agentflow.exception.AgentFlowException;
import top.egon.cola.component.agentflow.exception.AgentFlowExecutionException;
import top.egon.cola.component.agentflow.exception.AgentFlowExecutionTimeoutException;
import top.egon.cola.component.agentflow.exception.AgentFlowSessionNotFoundException;
import top.egon.cola.component.agentflow.runtime.AgentFlowRegistry;
import top.egon.cola.component.agentflow.runtime.AgentFlowRuntimeBO;
import top.egon.cola.component.common.core.validation.ValidationUtils;

import jakarta.validation.ConstraintViolationException;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/** Default facade that owns validation, Session lifecycle, reactive execution and shutdown. */
@RequiredArgsConstructor
@Slf4j
public class DefaultAgentFlowService implements AgentFlowService, AutoCloseable {

    @Qualifier("agentFlowRegistry")
    private final AgentFlowRegistry registry;

    @Qualifier("agentFlowSessionExecutionGuard")
    private final AgentFlowSessionExecutionGuard guard;

    @Qualifier("agentFlowProperties")
    private final AgentFlowProperties properties;

    @Qualifier("agentFlowValidationUtils")
    private final ValidationUtils validationUtils;

    @Qualifier("agentFlowClock")
    private final Clock clock;

    private final AtomicBoolean closed = new AtomicBoolean();

    @Override
    public List<AgentFlowDescriptorDTO> listFlows() {
        guard.ensureOpen();
        return registry.listFlows();
    }

    @Override
    public AgentFlowSessionResult createSession(AgentFlowSessionCommand command) {
        validationUtils.validate(command, AgentFlowSessionValidationGroup.Create.class);
        AgentFlowRuntimeBO runtime = registry.require(command.flowId());
        AgentFlowSessionExecutionGuard.Lease lease = guard.acquire(command.flowId(), command.userId(), null);
        try {
            Session session = runtime.runner().sessionService()
                    .createSession(command.flowId(), command.userId())
                    .blockingGet();
            if (session == null) {
                throw new AgentFlowExecutionException(command.flowId(),
                        new IllegalStateException("session creation returned no session"));
            }
            log.debug("Agent Flow session created flowId={} stage=SESSION outcome=SUCCESS", command.flowId());
            return new AgentFlowSessionResult(command.flowId(), session.id(), clock.instant());
        } catch (AgentFlowException failure) {
            throw failure;
        } catch (RuntimeException failure) {
            throw new AgentFlowExecutionException(command.flowId(), failure);
        } finally {
            lease.close();
        }
    }

    @Override
    public void deleteSession(AgentFlowSessionCommand command) {
        validationUtils.validate(command, AgentFlowSessionValidationGroup.Delete.class);
        AgentFlowRuntimeBO runtime = registry.require(command.flowId());
        AgentFlowSessionExecutionGuard.Lease lease = guard.acquire(
                command.flowId(), command.userId(), command.sessionId());
        try {
            requireSession(runtime.runner(), command.flowId(), command.userId(), command.sessionId());
            runtime.runner().sessionService()
                    .deleteSession(command.flowId(), command.userId(), command.sessionId())
                    .blockingAwait();
            log.debug("Agent Flow session deleted flowId={} stage=SESSION outcome=SUCCESS", command.flowId());
        } catch (AgentFlowException failure) {
            throw failure;
        } catch (RuntimeException failure) {
            throw new AgentFlowExecutionException(command.flowId(), failure);
        } finally {
            lease.close();
        }
    }

    @Override
    public List<Event> execute(AgentFlowExecutionCommand command) {
        return List.copyOf(executeStream(command).toList().blockingGet());
    }

    @Override
    public Flowable<Event> executeStream(AgentFlowExecutionCommand command) {
        return Flowable.defer(() -> {
            try {
                validationUtils.validate(command);
                requireMeaningfulContent(command.content());
                AgentFlowRuntimeBO runtime = registry.require(command.flowId());
                AgentFlowSessionExecutionGuard.Lease lease = guard.acquire(
                        command.flowId(), command.userId(), command.sessionId());
                try {
                    requireSession(runtime.runner(), command.flowId(), command.userId(), command.sessionId());
                    AtomicInteger eventCount = new AtomicInteger();
                    return runtime.runner().runAsync(command.userId(), command.sessionId(), command.content())
                            .doOnNext(event -> eventCount.incrementAndGet())
                            .timeout(properties.executionTimeout().toMillis(), TimeUnit.MILLISECONDS)
                            .doOnComplete(() -> log.debug(
                                    "Agent Flow execution stage=EXECUTION outcome=SUCCESS eventCount={}",
                                    eventCount.get()))
                            .doOnCancel(() -> log.debug(
                                    "Agent Flow execution stage=EXECUTION outcome=CANCELLED eventCount={}",
                                    eventCount.get()))
                            .doOnError(failure -> log.warn(
                                    "Agent Flow execution stage=EXECUTION outcome=ERROR errorType={}",
                                    failure.getClass().getSimpleName()))
                            .onErrorResumeNext(failure -> Flowable.error(
                                    mapExecutionFailure(command.flowId(), failure)))
                            .doFinally(lease::close);
                } catch (RuntimeException failure) {
                    lease.close();
                    return Flowable.error(mapExecutionFailure(command.flowId(), failure));
                }
            } catch (RuntimeException failure) {
                return Flowable.error(mapExecutionFailure(command == null ? "flow" : command.flowId(), failure));
            }
        });
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        guard.beginClosing();
        boolean quiescent = guard.awaitQuiescence(properties.shutdownTimeout());
        if (!quiescent) {
            log.warn("Agent Flow shutdown stage=QUIESCE outcome=TIMEOUT errorType=ShutdownTimeout");
        }
        try {
            registry.close().blockingAwait();
        } catch (RuntimeException failure) {
            log.warn("Agent Flow shutdown stage=REGISTRY outcome=ERROR errorType={}",
                    failure.getClass().getSimpleName());
        } finally {
            guard.markClosed();
        }
    }

    private static Session requireSession(InMemoryRunner runner, String flowId, String userId, String sessionId) {
        Session session = runner.sessionService()
                .getSession(flowId, userId, sessionId, Optional.empty())
                .blockingGet();
        if (session == null) {
            throw new AgentFlowSessionNotFoundException(flowId);
        }
        return session;
    }

    private static void requireMeaningfulContent(Content content) {
        if (content == null || content.parts().orElse(List.of()).stream().noneMatch(DefaultAgentFlowService::meaningfulPart)) {
            throw new IllegalArgumentException("content must contain a meaningful part");
        }
    }

    private static boolean meaningfulPart(Part part) {
        return part != null && (part.text().filter(text -> !text.isBlank()).isPresent()
                || part.functionCall().isPresent()
                || part.functionResponse().isPresent()
                || part.inlineData().isPresent()
                || part.fileData().isPresent());
    }

    private static Throwable mapExecutionFailure(String flowId, Throwable failure) {
        if (failure instanceof AgentFlowException) {
            return failure;
        }
        if (failure instanceof TimeoutException) {
            return new AgentFlowExecutionTimeoutException(flowId, failure);
        }
        if (failure instanceof ConstraintViolationException) {
            return failure;
        }
        return new AgentFlowExecutionException(flowId, failure);
    }
}
