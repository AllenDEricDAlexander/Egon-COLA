package top.egon.cola.archetype.source.agent.application.research.manage.impl;

import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import top.egon.cola.archetype.source.agent.application.research.command.StartDeepResearchCommand;
import top.egon.cola.archetype.source.agent.application.research.config.DeepResearchRuntimeProperties;
import top.egon.cola.archetype.source.agent.application.research.exception.DeepResearchApplicationException;
import top.egon.cola.archetype.source.agent.application.research.manage.DeepResearchManage;
import top.egon.cola.archetype.source.agent.application.research.service.ResearchCapacityService;
import top.egon.cola.archetype.source.agent.common.error.ResearchErrorCodeEnum;
import top.egon.cola.archetype.source.agent.domain.research.gateway.DeepResearchAgentGateway;
import top.egon.cola.archetype.source.agent.domain.research.model.DeepResearchEvent;
import top.egon.cola.archetype.source.agent.domain.research.model.DeepResearchTaskBO;
import top.egon.cola.archetype.source.agent.domain.research.service.DeepResearchEventObserverService;
import top.egon.cola.archetype.source.agent.domain.research.service.DeepResearchRunService;
import top.egon.cola.component.common.core.validation.ValidationUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/** Atomic application use case that owns capacity and terminal cleanup. */
@Service("deepResearchManage")
@RequiredArgsConstructor
@Slf4j
public class DeepResearchManageImpl implements DeepResearchManage {

    @Qualifier("deepResearchAgentGateway")
    private final DeepResearchAgentGateway gateway;

    @Qualifier("researchCapacityService")
    private final ResearchCapacityService capacityService;

    @Qualifier("agentValidationUtils")
    private final ValidationUtils validationUtils;

    @Qualifier("deepResearchRuntimeProperties")
    private final DeepResearchRuntimeProperties properties;

    @Qualifier("agentClock")
    private final Clock clock;

    @Override
    public DeepResearchRunService startResearch(
            StartDeepResearchCommand command, DeepResearchEventObserverService observer) {
        validateCommand(command, observer);
        if (command.maxSources() > properties.maxSources()) {
            throw new DeepResearchApplicationException(
                    ResearchErrorCodeEnum.RESEARCH_VALIDATION_ERROR, command.traceId());
        }

        Instant startedAt = clock.instant();
        String runId = UUID.randomUUID().toString();
        DeepResearchTaskBO task;
        try {
            task = DeepResearchTaskBO.create(runId, command.topic(), command.reportLanguage(), command.maxSources(),
                    startedAt.plus(properties.maxDuration()), command.traceId());
        } catch (RuntimeException failure) {
            throw new DeepResearchApplicationException(
                    ResearchErrorCodeEnum.RESEARCH_VALIDATION_ERROR, command.traceId(), failure);
        }

        ResearchCapacityService.Lease lease = capacityService.acquire(task.traceId());
        AtomicBoolean terminal = new AtomicBoolean();
        AtomicReference<DeepResearchRunService> downstream = new AtomicReference<>();
        DeepResearchEventObserverService guardedObserver = event -> {
            if (terminal.get()) {
                return;
            }
            try {
                DeepResearchEvent checkedEvent = Objects.requireNonNull(event, "research event must not be null");
                observer.onEvent(checkedEvent);
                if (checkedEvent.isTerminal() && terminal.compareAndSet(false, true)) {
                    lease.close();
                    log.debug("Deep Research runId={} traceId={} stage=RUN outcome={} durationMs={} code={}",
                            task.runId(), task.traceId(), checkedEvent.type(), durationMs(startedAt),
                            checkedEvent.errorCode() == null ? "NONE" : checkedEvent.errorCode().code());
                }
            } catch (RuntimeException observerFailure) {
                if (terminal.compareAndSet(false, true)) {
                    lease.close();
                    cancelDownstream(downstream.get());
                    log.warn("Deep Research runId={} traceId={} stage=RUN outcome=OBSERVER_ERROR durationMs={} code={}",
                            task.runId(), task.traceId(), durationMs(startedAt),
                            ResearchErrorCodeEnum.RESEARCH_INTERNAL_ERROR.code());
                }
                throw observerFailure;
            }
        };

        try {
            DeepResearchRunService run = Objects.requireNonNull(gateway.start(task, guardedObserver),
                    "gateway returned no run");
            downstream.set(run);
            if (terminal.get()) {
                cancelDownstream(run);
            }
            return new ManagedRunService(task.runId(), run, terminal, lease);
        } catch (DeepResearchApplicationException failure) {
            lease.close();
            throw failure;
        } catch (RuntimeException failure) {
            lease.close();
            throw new DeepResearchApplicationException(
                    ResearchErrorCodeEnum.RESEARCH_DEPENDENCY_UNAVAILABLE, task.traceId(), failure);
        }
    }

    private void validateCommand(StartDeepResearchCommand command, DeepResearchEventObserverService observer) {
        try {
            validationUtils.validate(command);
        } catch (ConstraintViolationException failure) {
            String traceId = command == null ? null : command.traceId();
            throw new DeepResearchApplicationException(
                    ResearchErrorCodeEnum.RESEARCH_VALIDATION_ERROR, traceId, failure);
        } catch (IllegalArgumentException failure) {
            String traceId = command == null ? null : command.traceId();
            throw new DeepResearchApplicationException(
                    ResearchErrorCodeEnum.RESEARCH_VALIDATION_ERROR, traceId, failure);
        }
        if (observer == null) {
            throw new DeepResearchApplicationException(
                    ResearchErrorCodeEnum.RESEARCH_VALIDATION_ERROR,
                    command == null ? null : command.traceId());
        }
    }

    private static void cancelDownstream(DeepResearchRunService run) {
        if (run == null) {
            return;
        }
        try {
            run.cancel();
        } catch (RuntimeException ignored) {
            // The lease is released by the caller; downstream cancellation is best effort.
        }
    }

    private long durationMs(Instant startedAt) {
        return Duration.between(startedAt, clock.instant()).toMillis();
    }

    private static final class ManagedRunService implements DeepResearchRunService {
        private final String runId;
        private final DeepResearchRunService downstream;
        private final AtomicBoolean terminal;
        private final ResearchCapacityService.Lease lease;
        private final AtomicBoolean cancelled = new AtomicBoolean();

        private ManagedRunService(String runId, DeepResearchRunService downstream, AtomicBoolean terminal,
                                  ResearchCapacityService.Lease lease) {
            this.runId = runId;
            this.downstream = downstream;
            this.terminal = terminal;
            this.lease = lease;
        }

        @Override
        public String runId() {
            return runId;
        }

        @Override
        public void cancel() {
            if (!terminal.compareAndSet(false, true) || !cancelled.compareAndSet(false, true)) {
                return;
            }
            try {
                downstream.cancel();
            } finally {
                lease.close();
            }
        }
    }
}
