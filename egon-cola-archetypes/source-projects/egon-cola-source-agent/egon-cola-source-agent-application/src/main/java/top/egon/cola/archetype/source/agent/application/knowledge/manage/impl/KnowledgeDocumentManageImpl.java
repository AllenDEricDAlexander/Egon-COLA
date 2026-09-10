package top.egon.cola.archetype.source.agent.application.knowledge.manage.impl;

import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import top.egon.cola.archetype.source.agent.application.knowledge.command.UploadKnowledgeDocumentCommand;
import top.egon.cola.archetype.source.agent.application.knowledge.config.KnowledgeRuntimeProperties;
import top.egon.cola.archetype.source.agent.application.knowledge.exception.KnowledgeApplicationException;
import top.egon.cola.archetype.source.agent.application.knowledge.manage.KnowledgeDocumentManage;
import top.egon.cola.archetype.source.agent.application.knowledge.service.KnowledgeIngestQueueService;
import top.egon.cola.archetype.source.agent.application.knowledge.service.KnowledgeRemovalService;
import top.egon.cola.archetype.source.agent.common.error.KnowledgeErrorCodeEnum;
import top.egon.cola.archetype.source.agent.domain.knowledge.model.DocumentIngestStatusEnum;
import top.egon.cola.archetype.source.agent.domain.knowledge.model.KnowledgeBaseBO;
import top.egon.cola.archetype.source.agent.domain.knowledge.model.KnowledgeDocumentBO;
import top.egon.cola.archetype.source.agent.domain.knowledge.repository.KnowledgeBaseRepository;
import top.egon.cola.archetype.source.agent.domain.knowledge.repository.KnowledgeDocumentRepository;
import top.egon.cola.component.common.core.pojo.PageResultRecord;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.common.mybatis.business.EgonColaTenantIdProvider;
import top.egon.cola.component.rag.api.RagExtractionService;
import top.egon.cola.component.rag.exception.RagExtractorMissingException;
import top.egon.cola.component.rag.extract.RagDocumentExtractorRegistry;
import top.egon.cola.component.rag.model.ExtractedDocumentBO;
import top.egon.cola.component.rag.model.RagExtractionCommand;
import top.egon.cola.component.rag.storage.RagDocumentStorage;

import java.io.ByteArrayInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;
import java.util.UUID;

/**
 * Document lifecycle of one knowledge base: upload, read, reprocess and delete.
 *
 * <p>The upload is the only use case with a hand-written transaction boundary, and it is deliberate
 * that the boundary is narrow. The format is routed, the original stored and the text parsed first,
 * outside any transaction: a database transaction held open across file I/O and document parsing is
 * the one way this path could exhaust the pool. The row and its queue message are then written
 * together, and only that step is atomic — the file is compensated if it fails.
 *
 * <p>The parse result is what gets stored: the text is persisted on the row, so a retried ingest
 * re-chunks what is already there instead of re-reading the original, and a reprocess never needs
 * the file at all.
 */
@Slf4j
@Service("knowledgeDocumentManage")
@RequiredArgsConstructor
public class KnowledgeDocumentManageImpl implements KnowledgeDocumentManage {

    private static final int MAX_PAGE_SIZE = 100;

    private final @Qualifier("knowledgeBaseRepository") KnowledgeBaseRepository knowledgeBaseRepository;

    private final @Qualifier("knowledgeDocumentRepository") KnowledgeDocumentRepository documentRepository;

    private final @Qualifier("knowledgeIngestQueueService") KnowledgeIngestQueueService ingestQueueService;

    private final @Qualifier("knowledgeRemovalService") KnowledgeRemovalService removalService;

    private final @Qualifier("ragDocumentStorage") RagDocumentStorage documentStorage;

    private final @Qualifier("ragExtractionService") RagExtractionService ragExtractionService;

    /** The same registry the extraction service routes through, asked before anything is written. */
    private final @Qualifier("ragDocumentExtractorRegistry") RagDocumentExtractorRegistry extractorRegistry;

    private final @Qualifier("knowledgeRuntimeProperties") KnowledgeRuntimeProperties properties;

    private final @Qualifier("agentValidationUtils") ValidationUtils validationUtils;

    private final EgonColaTenantIdProvider tenantIdProvider;

    @Override
    public KnowledgeDocumentBO upload(UploadKnowledgeDocumentCommand command) {
        validate(command, command == null ? null : command.traceId());
        String traceId = command.traceId();
        KnowledgeBaseBO base = requiredBase(command.knowledgeBaseId(), traceId);
        requireCapacity(base, traceId);
        requireSizeWithinLimit(command, traceId);
        requireExtractor(command, traceId);

        String collectionId = String.valueOf(base.knowledgeBaseId());
        // The identifier is generated here, before the row exists: the file has to be stored first,
        // and the key the row carries has to name the location the file actually went to.
        String storageDocumentId = UUID.randomUUID().toString();
        String storageKey = collectionId + "/" + storageDocumentId + "/" + baseName(command.fileName());
        storeOriginal(command, collectionId, storageDocumentId, traceId);
        ExtractedDocumentBO extracted = extract(command, collectionId, storageDocumentId, traceId);

        KnowledgeDocumentBO document = KnowledgeDocumentBO.create(currentTenantId(traceId), base.knowledgeBaseId(),
                command.effectiveDisplayName(), command.fileName(), mimeType(command, extracted),
                command.content().length, contentHash(command.content()), documentStorage.type().name(),
                storageKey, extracted.text());
        try {
            return ingestQueueService.storeAndEnqueue(document, traceId);
        } catch (RuntimeException failure) {
            // The row and its message rolled back together, so the original has no owner left: it is
            // deleted here rather than orphaned by a retry that will never come.
            removalService.deleteStoredFile(collectionId, storageDocumentId);
            log.warn("knowledge upload knowledgeBaseId={} sizeBytes={} outcome=FAILED code={}",
                    collectionId, command.content().length, KnowledgeErrorCodeEnum.KNOWLEDGE_INTERNAL_ERROR.code());
            throw asApplicationFailure(failure, traceId);
        }
    }

    @Override
    public KnowledgeDocumentBO get(Long documentId) {
        return required(documentId, null);
    }

    @Override
    public PageResultRecord<KnowledgeDocumentBO> page(Long knowledgeBaseId, int page, int size,
                                                      DocumentIngestStatusEnum status, String keyword) {
        requirePage(page, size);
        requiredBase(knowledgeBaseId, null);
        long total = documentRepository.count(knowledgeBaseId, status, keyword);
        return PageResultRecord.success(
                documentRepository.page(knowledgeBaseId, (page - 1) * size, size, status, keyword),
                total, page, size);
    }

    @Override
    public KnowledgeDocumentBO reingest(Long documentId) {
        KnowledgeDocumentBO document = required(documentId, null);
        if (!document.status().isTerminal()) {
            throw new KnowledgeApplicationException(KnowledgeErrorCodeEnum.KNOWLEDGE_DOCUMENT_BUSY, null);
        }
        if (document.content() == null || document.content().isBlank()) {
            // Nothing to embed: the only way out is a fresh upload, which the code tells the caller.
            throw new KnowledgeApplicationException(KnowledgeErrorCodeEnum.KNOWLEDGE_CONTENT_MISSING, null);
        }
        if (!ingestQueueService.resetAndEnqueue(document, null)) {
            throw new KnowledgeApplicationException(KnowledgeErrorCodeEnum.KNOWLEDGE_DOCUMENT_BUSY, null);
        }
        return required(documentId, null);
    }

    @Override
    public void delete(Long documentId) {
        KnowledgeDocumentBO document = required(documentId, null);
        removalService.deleteStoredFile(document);
        try {
            removalService.removeDocument(String.valueOf(document.knowledgeBaseId()), document.documentId());
        } catch (RuntimeException failure) {
            // Chunks and row went back together, so the document is unchanged and still retrievable.
            log.warn("knowledge document delete documentId={} knowledgeBaseId={} outcome=FAILED code={}",
                    document.documentId(), document.knowledgeBaseId(),
                    KnowledgeErrorCodeEnum.KNOWLEDGE_INTERNAL_ERROR.code());
            throw new KnowledgeApplicationException(
                    KnowledgeErrorCodeEnum.KNOWLEDGE_INTERNAL_ERROR, null, failure);
        }
        log.info("knowledge document deleted documentId={} knowledgeBaseId={}",
                document.documentId(), document.knowledgeBaseId());
    }

    private void storeOriginal(UploadKnowledgeDocumentCommand command, String collectionId,
                               String storageDocumentId, String traceId) {
        try {
            documentStorage.store(collectionId, storageDocumentId, command.fileName(),
                    new ByteArrayInputStream(command.content()));
        } catch (RuntimeException failure) {
            // Nothing was written anywhere: the caller gets the failure and no row exists to clean up.
            throw asApplicationFailure(failure, traceId);
        }
    }

    /** Parses the stored bytes; a parse that fails is answered with 500 and no partial persistence. */
    private ExtractedDocumentBO extract(UploadKnowledgeDocumentCommand command, String collectionId,
                                        String storageDocumentId, String traceId) {
        try {
            return ragExtractionService.extract(new RagExtractionCommand(command.fileName(), command.mimeType(),
                    new ByteArrayInputStream(command.content())));
        } catch (RuntimeException failure) {
            removalService.deleteStoredFile(collectionId, storageDocumentId);
            log.warn("knowledge upload knowledgeBaseId={} outcome=FAILED code={}",
                    collectionId, KnowledgeErrorCodeEnum.KNOWLEDGE_INTERNAL_ERROR.code());
            throw asApplicationFailure(failure, traceId);
        }
    }

    /**
     * Refuses a format no registered extractor accepts, before the original is written.
     *
     * <p>The routing is asked of the same registry the extraction service routes through, with the
     * same file name and MIME type, so the answer is the one {@code extract} would reach — only
     * earlier, while nothing has been stored and no row has to be compensated.
     */
    private void requireExtractor(UploadKnowledgeDocumentCommand command, String traceId) {
        try {
            extractorRegistry.route(command.mimeType(), command.fileName());
        } catch (RagExtractorMissingException missing) {
            log.info("knowledge upload outcome=REJECTED code={}",
                    KnowledgeErrorCodeEnum.KNOWLEDGE_EXTRACTOR_MISSING.code());
            throw KnowledgeApplicationException.onField(KnowledgeErrorCodeEnum.KNOWLEDGE_EXTRACTOR_MISSING,
                    traceId, "file", missing.safeMessage(), missing);
        }
    }

    private void requireCapacity(KnowledgeBaseBO base, String traceId) {
        long stored = documentRepository.countByKnowledgeBaseId(base.knowledgeBaseId());
        if (stored >= properties.runtime().maxDocumentsPerBase()) {
            throw new KnowledgeApplicationException(KnowledgeErrorCodeEnum.KNOWLEDGE_CAPACITY_EXHAUSTED, traceId);
        }
    }

    private void requireSizeWithinLimit(UploadKnowledgeDocumentCommand command, String traceId) {
        long limit = properties.runtime().maxUploadBytes();
        if (command.content().length > limit) {
            // Checked before anything is read into the engine, so the buffer stays bounded by it.
            throw KnowledgeApplicationException.onField(KnowledgeErrorCodeEnum.KNOWLEDGE_FILE_TOO_LARGE,
                    traceId, "file", "the upload exceeds " + limit + " bytes", null);
        }
    }

    private KnowledgeDocumentBO required(Long documentId, String traceId) {
        Objects.requireNonNull(documentId, "documentId must not be null");
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new KnowledgeApplicationException(
                        KnowledgeErrorCodeEnum.KNOWLEDGE_DOCUMENT_NOT_FOUND, traceId));
    }

    private KnowledgeBaseBO requiredBase(Long knowledgeBaseId, String traceId) {
        Objects.requireNonNull(knowledgeBaseId, "knowledgeBaseId must not be null");
        return knowledgeBaseRepository.findById(knowledgeBaseId)
                .orElseThrow(() -> new KnowledgeApplicationException(
                        KnowledgeErrorCodeEnum.KNOWLEDGE_BASE_NOT_FOUND, traceId));
    }

    private Long currentTenantId(String traceId) {
        Long tenantId = tenantIdProvider.currentTenantId();
        if (tenantId == null) {
            throw new KnowledgeApplicationException(KnowledgeErrorCodeEnum.KNOWLEDGE_INTERNAL_ERROR, traceId);
        }
        return tenantId;
    }

    /** The extractor's own type when it reported one, the caller's hint otherwise. */
    private static String mimeType(UploadKnowledgeDocumentCommand command, ExtractedDocumentBO extracted) {
        String detected = extracted.mimeType();
        return detected == null || detected.isBlank() ? command.mimeType() : detected;
    }

    private static String contentHash(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException missing) {
            throw new IllegalStateException("SHA-256 is required by the Java platform", missing);
        }
    }

    /** The last path segment of a file name, the part the storage layer keeps on disk. */
    private static String baseName(String fileName) {
        String normalized = fileName.replace('\\', '/');
        int separator = normalized.lastIndexOf('/');
        return separator < 0 ? normalized : normalized.substring(separator + 1);
    }

    private static KnowledgeApplicationException asApplicationFailure(RuntimeException failure, String traceId) {
        return failure instanceof KnowledgeApplicationException applicationFailure
                ? applicationFailure
                : new KnowledgeApplicationException(KnowledgeErrorCodeEnum.KNOWLEDGE_INTERNAL_ERROR, traceId, failure);
    }

    private void requirePage(int page, int size) {
        if (page < 1 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new KnowledgeApplicationException(KnowledgeErrorCodeEnum.KNOWLEDGE_VALIDATION_ERROR, null);
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
