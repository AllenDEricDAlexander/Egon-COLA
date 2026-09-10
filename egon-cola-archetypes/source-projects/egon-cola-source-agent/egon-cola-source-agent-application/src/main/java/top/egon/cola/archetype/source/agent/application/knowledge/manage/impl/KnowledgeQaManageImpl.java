package top.egon.cola.archetype.source.agent.application.knowledge.manage.impl;

import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import top.egon.cola.archetype.source.agent.application.knowledge.command.AskKnowledgeBaseCommand;
import top.egon.cola.archetype.source.agent.application.knowledge.command.RetrieveKnowledgeCommand;
import top.egon.cola.archetype.source.agent.application.knowledge.exception.KnowledgeApplicationException;
import top.egon.cola.archetype.source.agent.application.knowledge.manage.KnowledgeQaManage;
import top.egon.cola.archetype.source.agent.application.knowledge.service.KnowledgeQaCapacityService;
import top.egon.cola.archetype.source.agent.common.error.KnowledgeErrorCodeEnum;
import top.egon.cola.archetype.source.agent.domain.knowledge.gateway.KnowledgeAnswerGateway;
import top.egon.cola.archetype.source.agent.domain.knowledge.gateway.KnowledgeVectorGateway;
import top.egon.cola.archetype.source.agent.domain.knowledge.model.KnowledgeAnswerTaskBO;
import top.egon.cola.archetype.source.agent.domain.knowledge.model.KnowledgeBaseBO;
import top.egon.cola.archetype.source.agent.domain.knowledge.model.KnowledgeChunkBO;
import top.egon.cola.archetype.source.agent.domain.knowledge.model.KnowledgeDocumentBO;
import top.egon.cola.archetype.source.agent.domain.knowledge.model.KnowledgeQaEvent;
import top.egon.cola.archetype.source.agent.domain.knowledge.model.KnowledgeRetrievalBO;
import top.egon.cola.archetype.source.agent.domain.knowledge.model.KnowledgeRetrievedChunkBO;
import top.egon.cola.archetype.source.agent.domain.knowledge.repository.KnowledgeBaseRepository;
import top.egon.cola.archetype.source.agent.domain.knowledge.repository.KnowledgeDocumentRepository;
import top.egon.cola.archetype.source.agent.domain.knowledge.service.KnowledgeAnswerRunService;
import top.egon.cola.archetype.source.agent.domain.knowledge.service.KnowledgeQaEventObserverService;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.rag.exception.RagException;
import top.egon.cola.component.rag.exception.RagModelNotRegisteredException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Retrieval debugging and the retrieval-backed answer stream.
 *
 * <p>Both use cases read the collection, the logical model and the tenant scope from the base and
 * the running thread, so a caller can neither ask another base's chunks nor answer from another
 * model. The retrieval is a read-only call that never reaches a chat model, which is what keeps the
 * debug path free of generation cost.
 *
 * <p>A question holds a capacity permit for as long as its generation runs, so the permit is taken
 * before the first dependency call — the retrieval embeds the question too — and released exactly
 * once: by the terminal event, by {@code cancel()} when the caller disconnects, or by the failure
 * that replaces both. Every path out of this class either releases the permit or hands it to the
 * returned handle.
 */
@Slf4j
@Service("knowledgeQaManage")
@RequiredArgsConstructor
public class KnowledgeQaManageImpl implements KnowledgeQaManage {

    /** The component's own result cap, which an absent {@code topK} leaves in charge. */
    private static final int COMPONENT_DEFAULT_TOP_K = 0;

    /** The answer path takes every retrieved chunk: the reference cap is {@code topK}, not a floor. */
    private static final double ACCEPT_EVERY_SCORE = 0.0d;

    private final @Qualifier("knowledgeBaseRepository") KnowledgeBaseRepository knowledgeBaseRepository;

    private final @Qualifier("knowledgeDocumentRepository") KnowledgeDocumentRepository documentRepository;

    private final @Qualifier("knowledgeVectorGateway") KnowledgeVectorGateway vectorGateway;

    private final @Qualifier("knowledgeAnswerGateway") KnowledgeAnswerGateway answerGateway;

    private final @Qualifier("knowledgeQaCapacityService") KnowledgeQaCapacityService capacityService;

    private final @Qualifier("agentValidationUtils") ValidationUtils validationUtils;

    private final @Qualifier("agentClock") Clock clock;

    @Override
    public KnowledgeRetrievalBO retrieve(RetrieveKnowledgeCommand command) {
        validate(command, command == null ? null : command.traceId());
        String traceId = command.traceId();
        KnowledgeBaseBO base = requiredBase(command.knowledgeBaseId(), traceId);
        List<KnowledgeRetrievedChunkBO> matched = search(base, command.query(), command.topK(),
                command.effectiveSimilarityThreshold(), traceId);
        log.info("knowledge retrieval knowledgeBaseId={} logicalModelName={} topK={} threshold={} matched={}"
                        + " outcome=OK", base.knowledgeBaseId(), base.embeddingModel(), command.topK(),
                command.effectiveSimilarityThreshold(), matched.size());
        return new KnowledgeRetrievalBO(base.embeddingModel(), matched);
    }

    @Override
    public KnowledgeAnswerRunService ask(AskKnowledgeBaseCommand command, KnowledgeQaEventObserverService observer) {
        validate(command, command == null ? null : command.traceId());
        if (observer == null) {
            throw new KnowledgeApplicationException(KnowledgeErrorCodeEnum.KNOWLEDGE_VALIDATION_ERROR,
                    command == null ? null : command.traceId());
        }
        String traceId = command.traceId();
        KnowledgeBaseBO base = requiredBase(command.knowledgeBaseId(), traceId);
        KnowledgeQaCapacityService.Lease lease = acquire(base, traceId);

        List<KnowledgeRetrievedChunkBO> matched;
        try {
            // A saturated instance refuses before the retrieval, and an empty result is a valid
            // answer source: the model is told there is nothing to cite instead of being skipped.
            matched = search(base, command.question(), command.topK(), ACCEPT_EVERY_SCORE, traceId);
        } catch (RuntimeException failure) {
            lease.close();
            throw failure;
        }

        Instant startedAt = clock.instant();
        String answerId = UUID.randomUUID().toString();
        // The task carries the chunk text, which is what the model answers from; the events carry the
        // same chunks without it, which is what the caller is shown.
        KnowledgeAnswerTaskBO task = new KnowledgeAnswerTaskBO(answerId, command.question(),
                base.embeddingModel(), matched, traceId);
        AtomicBoolean terminal = new AtomicBoolean();
        AtomicReference<KnowledgeAnswerRunService> downstream = new AtomicReference<>();
        KnowledgeQaEventObserverService guardedObserver =
                guardedObserver(observer, terminal, downstream, lease, base, answerId, startedAt);
        try {
            KnowledgeAnswerRunService run = Objects.requireNonNull(answerGateway.generate(task, guardedObserver),
                    "gateway returned no run");
            downstream.set(run);
            if (terminal.get()) {
                // The generation ended while it was being set up: its events are done, so the model
                // subscription is released instead of being held by a handle nobody will cancel.
                cancelDownstream(run);
            }
            return new ManagedRunService(run, terminal, lease);
        } catch (KnowledgeApplicationException failure) {
            lease.close();
            throw failure;
        } catch (RuntimeException failure) {
            // Nothing was published, so the caller still sees an ordinary failure response rather
            // than a stream that ends without a terminal event.
            lease.close();
            log.warn("knowledge qa answerId={} knowledgeBaseId={} logicalModelName={} outcome=FAILED code={}",
                    answerId, base.knowledgeBaseId(), base.embeddingModel(),
                    KnowledgeErrorCodeEnum.KNOWLEDGE_DEPENDENCY_UNAVAILABLE.code());
            throw new KnowledgeApplicationException(
                    KnowledgeErrorCodeEnum.KNOWLEDGE_DEPENDENCY_UNAVAILABLE, traceId, failure);
        }
    }

    /**
     * Forwards the generation's events, releasing the permit on the one that ends the stream.
     *
     * <p>The permit is closed before the observer is told nothing further will come, and an observer
     * that fails ends the stream the same way a terminal event does: the caller is gone, so the
     * model is cancelled and the permit released rather than left with a reader that never returns.
     */
    private KnowledgeQaEventObserverService guardedObserver(KnowledgeQaEventObserverService observer,
                                                            AtomicBoolean terminal,
                                                            AtomicReference<KnowledgeAnswerRunService> downstream,
                                                            KnowledgeQaCapacityService.Lease lease,
                                                            KnowledgeBaseBO base, String answerId,
                                                            Instant startedAt) {
        return event -> {
            if (terminal.get()) {
                return;
            }
            try {
                KnowledgeQaEvent checked = Objects.requireNonNull(event, "qa event must not be null");
                observer.onEvent(checked);
                if (checked.isTerminal() && terminal.compareAndSet(false, true)) {
                    lease.close();
                    log.info("knowledge qa answerId={} knowledgeBaseId={} logicalModelName={} stage=RUN"
                                    + " outcome={} durationMs={} code={}",
                            answerId, base.knowledgeBaseId(), base.embeddingModel(), checked.type(),
                            durationMs(startedAt), checked.errorCode() == null ? "NONE" : checked.errorCode().code());
                }
            } catch (RuntimeException observerFailure) {
                if (terminal.compareAndSet(false, true)) {
                    lease.close();
                    cancelDownstream(downstream.get());
                    log.warn("knowledge qa answerId={} knowledgeBaseId={} logicalModelName={} stage=RUN"
                                    + " outcome=OBSERVER_ERROR durationMs={} code={}",
                            answerId, base.knowledgeBaseId(), base.embeddingModel(), durationMs(startedAt),
                            KnowledgeErrorCodeEnum.KNOWLEDGE_INTERNAL_ERROR.code());
                }
                throw observerFailure;
            }
        };
    }

    /**
     * Resolves the display names and applies the score floor of one retrieval.
     *
     * <p>The names are resolved in one batch rather than per chunk, which is what keeps a retrieval
     * response from forcing its caller into a second lookup per document.
     */
    private List<KnowledgeRetrievedChunkBO> search(KnowledgeBaseBO base, String query, Integer topK,
                                                   double threshold, String traceId) {
        List<KnowledgeChunkBO> chunks;
        try {
            chunks = vectorGateway.retrieve(String.valueOf(base.knowledgeBaseId()), base.embeddingModel(), query,
                    topK == null ? COMPONENT_DEFAULT_TOP_K : topK, Map.of());
        } catch (RagException failure) {
            throw new KnowledgeApplicationException(failureCode(failure), traceId, failure);
        } catch (RuntimeException failure) {
            throw new KnowledgeApplicationException(KnowledgeErrorCodeEnum.KNOWLEDGE_INTERNAL_ERROR, traceId, failure);
        }
        List<KnowledgeChunkBO> accepted = chunks == null ? List.of() : chunks.stream()
                .filter(chunk -> matchesThreshold(chunk.score(), threshold))
                .toList();
        if (accepted.isEmpty()) {
            return List.of();
        }
        Map<Long, String> displayNames = displayNames(accepted);
        return accepted.stream().map(chunk -> new KnowledgeRetrievedChunkBO(chunk.documentId(), chunk.chunkIndex(),
                displayNames.get(chunk.documentId()), chunk.score(), chunk.content())).toList();
    }

    /**
     * An unknown score passes only when every score is accepted: a store that reports no score
     * cannot be compared against a floor, and dropping such a chunk would hide a match the caller
     * asked to see.
     */
    private static boolean matchesThreshold(Double score, double threshold) {
        return score == null ? threshold <= ACCEPT_EVERY_SCORE : score >= threshold;
    }

    private Map<Long, String> displayNames(List<KnowledgeChunkBO> chunks) {
        List<Long> documentIds = chunks.stream().map(KnowledgeChunkBO::documentId).distinct().toList();
        Map<Long, String> displayNames = new LinkedHashMap<>();
        for (KnowledgeDocumentBO document : documentRepository.findByIds(documentIds)) {
            displayNames.put(document.documentId(), document.displayName());
        }
        return displayNames;
    }

    private KnowledgeQaCapacityService.Lease acquire(KnowledgeBaseBO base, String traceId) {
        Optional<KnowledgeQaCapacityService.Lease> acquired = capacityService.tryAcquire();
        if (acquired.isEmpty()) {
            log.warn("knowledge qa knowledgeBaseId={} outcome=REJECTED code={}", base.knowledgeBaseId(),
                    KnowledgeErrorCodeEnum.KNOWLEDGE_CAPACITY_EXHAUSTED.code());
            throw new KnowledgeApplicationException(KnowledgeErrorCodeEnum.KNOWLEDGE_CAPACITY_EXHAUSTED, traceId);
        }
        return acquired.get();
    }

    private KnowledgeBaseBO requiredBase(Long knowledgeBaseId, String traceId) {
        Objects.requireNonNull(knowledgeBaseId, "knowledgeBaseId must not be null");
        return knowledgeBaseRepository.findById(knowledgeBaseId)
                .orElseThrow(() -> new KnowledgeApplicationException(
                        KnowledgeErrorCodeEnum.KNOWLEDGE_BASE_NOT_FOUND, traceId));
    }

    /** The stable project code for a component failure; an unregistered model is the caller's fault. */
    private static KnowledgeErrorCodeEnum failureCode(RagException failure) {
        return failure instanceof RagModelNotRegisteredException
                ? KnowledgeErrorCodeEnum.KNOWLEDGE_MODEL_NOT_REGISTERED
                : KnowledgeErrorCodeEnum.KNOWLEDGE_DEPENDENCY_UNAVAILABLE;
    }

    private static void cancelDownstream(KnowledgeAnswerRunService run) {
        if (run == null) {
            return;
        }
        try {
            run.cancel();
        } catch (RuntimeException ignored) {
            // The permit is released by the caller; downstream cancellation is best effort.
        }
    }

    private long durationMs(Instant startedAt) {
        return Duration.between(startedAt, clock.instant()).toMillis();
    }

    private void validate(Object command, String traceId) {
        try {
            validationUtils.validate(command);
        } catch (ConstraintViolationException | IllegalArgumentException invalid) {
            throw new KnowledgeApplicationException(
                    KnowledgeErrorCodeEnum.KNOWLEDGE_VALIDATION_ERROR, traceId, invalid);
        }
    }

    /** The handle that ends one generation early; its permit is released once, on the first close. */
    private static final class ManagedRunService implements KnowledgeAnswerRunService {

        private final KnowledgeAnswerRunService downstream;

        private final AtomicBoolean terminal;

        private final KnowledgeQaCapacityService.Lease lease;

        private final AtomicBoolean cancelled = new AtomicBoolean();

        private ManagedRunService(KnowledgeAnswerRunService downstream, AtomicBoolean terminal,
                                  KnowledgeQaCapacityService.Lease lease) {
            this.downstream = downstream;
            this.terminal = terminal;
            this.lease = lease;
        }

        @Override
        public void cancel() {
            // A run that already ended has released its permit; cancelling it again must not.
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
