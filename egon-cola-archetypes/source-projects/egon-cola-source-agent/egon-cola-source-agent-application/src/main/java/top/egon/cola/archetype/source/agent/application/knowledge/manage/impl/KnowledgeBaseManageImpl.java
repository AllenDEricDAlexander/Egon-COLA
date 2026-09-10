package top.egon.cola.archetype.source.agent.application.knowledge.manage.impl;

import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import top.egon.cola.archetype.source.agent.application.knowledge.command.CreateKnowledgeBaseCommand;
import top.egon.cola.archetype.source.agent.application.knowledge.command.UpdateKnowledgeBaseCommand;
import top.egon.cola.archetype.source.agent.application.knowledge.config.KnowledgeRuntimeProperties;
import top.egon.cola.archetype.source.agent.application.knowledge.exception.KnowledgeApplicationException;
import top.egon.cola.archetype.source.agent.application.knowledge.manage.KnowledgeBaseManage;
import top.egon.cola.archetype.source.agent.application.knowledge.service.KnowledgeRemovalService;
import top.egon.cola.archetype.source.agent.common.error.KnowledgeErrorCodeEnum;
import top.egon.cola.archetype.source.agent.domain.knowledge.model.DocumentIngestStatusEnum;
import top.egon.cola.archetype.source.agent.domain.knowledge.model.KnowledgeBaseBO;
import top.egon.cola.archetype.source.agent.domain.knowledge.model.KnowledgeChunkConfigBO;
import top.egon.cola.archetype.source.agent.domain.knowledge.model.KnowledgeDocumentBO;
import top.egon.cola.archetype.source.agent.domain.knowledge.repository.KnowledgeBaseRepository;
import top.egon.cola.archetype.source.agent.domain.knowledge.repository.KnowledgeDocumentRepository;
import top.egon.cola.component.common.core.pojo.PageResultRecord;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.common.mybatis.business.EgonColaTenantIdProvider;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;


/**
 * Knowledge base lifecycle: create, read, edit and the cascading delete.
 *
 * <p>The indexing configuration is frozen at creation: no later use case accepts it, and the update
 * path rejects a request that carries one instead of dropping it, so a caller cannot believe a
 * change took effect.
 *
 * <p>The delete is the one use case that spans storage, the vector store and the database. Stored
 * files go first, outside any transaction, because deleting them inside one would hold a database
 * transaction open across slow file I/O; the database part — chunk removal, document and base
 * soft deletion — then commits as one unit.
 */
@Slf4j
@Service("knowledgeBaseManage")
@RequiredArgsConstructor
public class KnowledgeBaseManageImpl implements KnowledgeBaseManage {

    /** The message the immutable-field rejection attaches to every field it names. */
    private static final String IMMUTABLE_FIELD_MESSAGE = "immutable after creation";

    private final @Qualifier("knowledgeBaseRepository") KnowledgeBaseRepository knowledgeBaseRepository;

    private final @Qualifier("knowledgeDocumentRepository") KnowledgeDocumentRepository documentRepository;

    private final @Qualifier("knowledgeRemovalService") KnowledgeRemovalService removalService;

    private final @Qualifier("knowledgeRuntimeProperties") KnowledgeRuntimeProperties properties;

    private final @Qualifier("agentValidationUtils") ValidationUtils validationUtils;

    private final EgonColaTenantIdProvider tenantIdProvider;

    @Override
    public KnowledgeBaseBO create(CreateKnowledgeBaseCommand command) {
        validate(command, command == null ? null : command.traceId());
        KnowledgeBaseBO stored = knowledgeBaseRepository.insert(assemble(command));
        log.info("knowledge base created knowledgeBaseId={} code={} logicalModelName={} chunkStrategy={}",
                stored.knowledgeBaseId(), stored.code(), stored.embeddingModel(), stored.chunkStrategy());
        return stored;
    }

    @Override
    public KnowledgeBaseBO get(Long knowledgeBaseId) {
        return required(knowledgeBaseId, null);
    }

    @Override
    public PageResultRecord<KnowledgeBaseBO> page(int page, int size, String keyword, String embeddingModel) {
        requirePage(page, size, null);
        long total = knowledgeBaseRepository.count(keyword, embeddingModel);
        List<KnowledgeBaseBO> records = knowledgeBaseRepository.page(offset(page, size), size, keyword, embeddingModel);
        return PageResultRecord.success(records, total, page, size);
    }

    @Override
    public KnowledgeBaseBO update(UpdateKnowledgeBaseCommand command) {
        validate(command, command == null ? null : command.traceId());
        String traceId = command.traceId();
        List<String> immutable = command.immutableFields();
        if (!immutable.isEmpty()) {
            // Probing before the write is what makes the rejection truthful: a silently dropped
            // field would let the caller believe the new value was stored.
            throw new KnowledgeApplicationException(KnowledgeErrorCodeEnum.KNOWLEDGE_IMMUTABLE_FIELD,
                    traceId, immutableFieldErrors(immutable), null);
        }
        required(command.knowledgeBaseId(), traceId);
        knowledgeBaseRepository.updateNameAndDescription(
                command.knowledgeBaseId(), command.name(), command.description());
        KnowledgeBaseBO updated = required(command.knowledgeBaseId(), traceId);
        log.info("knowledge base updated knowledgeBaseId={} outcome=UPDATED", updated.knowledgeBaseId());
        return updated;
    }

    @Override
    public long documentCount(Long knowledgeBaseId) {
        Objects.requireNonNull(knowledgeBaseId, "knowledgeBaseId must not be null");
        required(knowledgeBaseId, null);
        return documentRepository.countByKnowledgeBaseId(knowledgeBaseId);
    }

    @Override
    public void delete(Long knowledgeBaseId) {
        Objects.requireNonNull(knowledgeBaseId, "knowledgeBaseId must not be null");
        required(knowledgeBaseId, null);
        long inFlight = countInFlight(knowledgeBaseId);
        if (inFlight > 0) {
            // A delivery that is already running cannot be recalled, so the base waits for it
            // rather than letting the cascade race a chunk write.
            log.info("knowledge base delete knowledgeBaseId={} outcome=REJECTED reason=documents_in_progress count={}",
                    knowledgeBaseId, inFlight);
            throw new KnowledgeApplicationException(KnowledgeErrorCodeEnum.KNOWLEDGE_BASE_BUSY, null);
        }
        List<KnowledgeDocumentBO> documents = allDocuments(knowledgeBaseId);
        // Files first, outside the transaction below: deleting them inside one would hold a database
        // transaction open across slow file I/O. The deletion is idempotent by contract, so a
        // transaction that still fails leaves the files gone and a retry finds nothing to delete.
        documents.forEach(removalService::deleteStoredFile);
        try {
            removalService.removeBase(String.valueOf(knowledgeBaseId), knowledgeBaseId,
                    documents.stream().map(KnowledgeDocumentBO::documentId).toList());
        } catch (RuntimeException failure) {
            // The transaction rolled both the chunks and the rows back, so the base is unchanged and
            // a retry is safe; the caller is told the cascade failed rather than that it succeeded.
            log.warn("knowledge base delete knowledgeBaseId={} documentCount={} outcome=FAILED code={}",
                    knowledgeBaseId, documents.size(), KnowledgeErrorCodeEnum.KNOWLEDGE_INTERNAL_ERROR.code());
            throw new KnowledgeApplicationException(
                    KnowledgeErrorCodeEnum.KNOWLEDGE_INTERNAL_ERROR, null, failure);
        }
        log.info("knowledge base deleted knowledgeBaseId={} documentCount={} outcome=DELETED",
                knowledgeBaseId, documents.size());
    }

    /** Turns the request into the domain value, refusing a combination the domain does not allow. */
    private KnowledgeBaseBO assemble(CreateKnowledgeBaseCommand command) {
        try {
            return KnowledgeBaseBO.create(command.code(), command.name(), command.description(),
                    command.embeddingModel(), command.chunkStrategy(), chunkConfig(command), currentTenantId());
        } catch (IllegalArgumentException invalid) {
            throw new KnowledgeApplicationException(
                    KnowledgeErrorCodeEnum.KNOWLEDGE_VALIDATION_ERROR, command.traceId(), invalid);
        }
    }

    private static KnowledgeChunkConfigBO chunkConfig(CreateKnowledgeBaseCommand command) {
        CreateKnowledgeBaseCommand.ChunkingConfigCommand config = command.chunkConfig();
        try {
            return new KnowledgeChunkConfigBO(config.maxTokensPerChunk(), config.overlapTokens(),
                    config.minChunkChars(), config.headingLevels());
        } catch (IllegalArgumentException invalid) {
            throw new KnowledgeApplicationException(KnowledgeErrorCodeEnum.KNOWLEDGE_VALIDATION_ERROR,
                    command.traceId(), Map.of("chunkConfig", List.of(invalid.getMessage())), invalid);
        }
    }

    private KnowledgeBaseBO required(Long knowledgeBaseId, String traceId) {
        return knowledgeBaseRepository.findById(knowledgeBaseId)
                .orElseThrow(() -> new KnowledgeApplicationException(
                        KnowledgeErrorCodeEnum.KNOWLEDGE_BASE_NOT_FOUND, traceId));
    }

    /** Non-terminal documents, counted per status so the state machine stays the single source. */
    private long countInFlight(Long knowledgeBaseId) {
        long inFlight = 0L;
        for (DocumentIngestStatusEnum status : DocumentIngestStatusEnum.values()) {
            if (!status.isTerminal()) {
                inFlight += documentRepository.count(knowledgeBaseId, status, null);
            }
        }
        return inFlight;
    }

    /**
     * Every document of the base, not just the first page: the per-base cap bounds how many there
     * can be, but a lowered cap must not leave the surplus behind on a delete.
     */
    private List<KnowledgeDocumentBO> allDocuments(Long knowledgeBaseId) {
        int pageSize = properties.runtime().maxDocumentsPerBase();
        List<KnowledgeDocumentBO> documents = new ArrayList<>();
        List<KnowledgeDocumentBO> batch;
        do {
            batch = documentRepository.page(knowledgeBaseId, documents.size(), pageSize, null, null);
            documents.addAll(batch);
        } while (batch.size() == pageSize);
        return documents;
    }

    private Long currentTenantId() {
        Long tenantId = tenantIdProvider.currentTenantId();
        if (tenantId == null) {
            throw new KnowledgeApplicationException(
                    KnowledgeErrorCodeEnum.KNOWLEDGE_INTERNAL_ERROR, null);
        }
        return tenantId;
    }

    private static Map<String, List<String>> immutableFieldErrors(List<String> fields) {
        Map<String, List<String>> errors = new LinkedHashMap<>();
        fields.forEach(field -> errors.put(field, List.of(IMMUTABLE_FIELD_MESSAGE)));
        return Map.copyOf(errors);
    }

    private static int offset(int page, int size) {
        return (page - 1) * size;
    }

    private void requirePage(int page, int size, String traceId) {
        if (page < 1 || size < 1 || size > 100) {
            throw new KnowledgeApplicationException(
                    KnowledgeErrorCodeEnum.KNOWLEDGE_VALIDATION_ERROR, traceId);
        }
    }

    private void validate(Object command, String traceId) {
        try {
            validationUtils.validate(command);
        } catch (ConstraintViolationException | IllegalArgumentException invalid) {
            throw new KnowledgeApplicationException(
                    KnowledgeErrorCodeEnum.KNOWLEDGE_VALIDATION_ERROR, traceId, invalid);
        }
    }
}
