package top.egon.cola.archetype.source.agent.infrastructure.knowledge.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import top.egon.cola.archetype.source.agent.domain.knowledge.model.DocumentIngestStatusEnum;
import top.egon.cola.archetype.source.agent.domain.knowledge.model.KnowledgeDocumentBO;
import top.egon.cola.archetype.source.agent.domain.knowledge.repository.KnowledgeDocumentRepository;
import top.egon.cola.archetype.source.agent.infrastructure.knowledge.repo.converter.KnowledgeDocumentPOConverter;
import top.egon.cola.archetype.source.agent.infrastructure.knowledge.repo.dao.KnowledgeDocumentDAO;
import top.egon.cola.archetype.source.agent.infrastructure.knowledge.repo.po.KnowledgeDocumentPO;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * MyBatis-Plus implementation of the document port.
 *
 * <p>Each status write carries the status it expects to leave in its condition, so a delivery
 * attempt that lost the document to a concurrent reprocess reports {@code false} instead of
 * overwriting the newer state. The tenant scope and the soft-delete filter are injected by the
 * interceptor chain, never written here.
 */
@Slf4j
@Repository("knowledgeDocumentRepository")
@RequiredArgsConstructor
public class KnowledgeDocumentRepositoryImpl implements KnowledgeDocumentRepository {

    /** The statuses a reprocess may leave, derived from the domain state machine. */
    private static final List<String> TERMINAL_STATUSES = Arrays.stream(DocumentIngestStatusEnum.values())
            .filter(DocumentIngestStatusEnum::isTerminal)
            .map(Enum::name)
            .toList();

    private final @Qualifier("knowledgeDocumentDAO") KnowledgeDocumentDAO knowledgeDocumentDAO;

    @Override
    public KnowledgeDocumentBO insert(KnowledgeDocumentBO document) {
        Objects.requireNonNull(document, "document must not be null");
        KnowledgeDocumentPO stored = KnowledgeDocumentPOConverter.INSTANCE.toSource(document);
        knowledgeDocumentDAO.insert(stored);
        log.debug("stored document {} of knowledge base {}", stored.getId(), stored.getKnowledgeBaseId());
        return KnowledgeDocumentPOConverter.INSTANCE.toTarget(stored);
    }

    @Override
    public Optional<KnowledgeDocumentBO> findById(Long documentId) {
        Objects.requireNonNull(documentId, "documentId must not be null");
        return Optional.ofNullable(knowledgeDocumentDAO.selectById(documentId))
                .map(KnowledgeDocumentPOConverter.INSTANCE::toTarget);
    }

    @Override
    public List<KnowledgeDocumentBO> findByIds(Collection<Long> documentIds) {
        Objects.requireNonNull(documentIds, "documentIds must not be null");
        if (documentIds.isEmpty()) {
            return List.of();
        }
        return KnowledgeDocumentPOConverter.INSTANCE.toTargetList(knowledgeDocumentDAO.selectList(
                Wrappers.<KnowledgeDocumentPO>lambdaQuery()
                        .in(KnowledgeDocumentPO::getId, documentIds)
                        .orderByDesc(KnowledgeDocumentPO::getCreateTime, KnowledgeDocumentPO::getId)));
    }

    @Override
    public List<KnowledgeDocumentBO> page(Long knowledgeBaseId, int offset, int size,
                                          DocumentIngestStatusEnum status, String keyword) {
        return KnowledgeDocumentPOConverter.INSTANCE.toTargetList(knowledgeDocumentDAO.selectList(
                filtered(knowledgeBaseId, status, keyword)
                        .orderByDesc(KnowledgeDocumentPO::getCreateTime, KnowledgeDocumentPO::getId)
                        .last(window(offset, size))));
    }

    @Override
    public long count(Long knowledgeBaseId, DocumentIngestStatusEnum status, String keyword) {
        return knowledgeDocumentDAO.selectCount(filtered(knowledgeBaseId, status, keyword));
    }

    @Override
    public long countByKnowledgeBaseId(Long knowledgeBaseId) {
        Objects.requireNonNull(knowledgeBaseId, "knowledgeBaseId must not be null");
        return knowledgeDocumentDAO.selectCount(Wrappers.<KnowledgeDocumentPO>lambdaQuery()
                .eq(KnowledgeDocumentPO::getKnowledgeBaseId, knowledgeBaseId));
    }

    @Override
    public boolean markProcessing(Long documentId, int attemptCount) {
        return writeTransition(documentId, DocumentIngestStatusEnum.PENDING, change -> change
                .setStatus(DocumentIngestStatusEnum.PROCESSING.name())
                .setAttemptCount(attemptCount)
                .setErrorCode(null)
                .setErrorMessage(null));
    }

    @Override
    public boolean markSucceeded(Long documentId, int chunkCount) {
        return writeTransition(documentId, DocumentIngestStatusEnum.PROCESSING, change -> change
                .setStatus(DocumentIngestStatusEnum.SUCCEEDED.name())
                .setChunkCount(chunkCount));
    }

    @Override
    public boolean markRetryPending(Long documentId, int attemptCount, String errorCode,
                                    String errorMessage) {
        return writeTransition(documentId, DocumentIngestStatusEnum.PROCESSING, change -> change
                .setStatus(DocumentIngestStatusEnum.PENDING.name())
                .setAttemptCount(attemptCount)
                .setErrorCode(errorCode)
                .setErrorMessage(errorMessage));
    }

    @Override
    public boolean markDead(Long documentId, String errorCode, String errorMessage) {
        return writeTransition(documentId, DocumentIngestStatusEnum.PROCESSING, change -> change
                .setStatus(DocumentIngestStatusEnum.DEAD.name())
                .setErrorCode(errorCode)
                .setErrorMessage(errorMessage));
    }

    @Override
    public boolean resetForReingest(Long documentId) {
        Objects.requireNonNull(documentId, "documentId must not be null");
        KnowledgeDocumentPO current = knowledgeDocumentDAO.selectById(documentId);
        if (current == null || !TERMINAL_STATUSES.contains(current.getStatus())) {
            log.debug("document {} was not left in a terminal status, nothing reset", documentId);
            return false;
        }
        KnowledgeDocumentPO change = carrierOf(current)
                .setStatus(DocumentIngestStatusEnum.PENDING.name())
                .setChunkCount(0)
                .setAttemptCount(0)
                .setErrorCode(null)
                .setErrorMessage(null);
        return knowledgeDocumentDAO.update(change, Wrappers.<KnowledgeDocumentPO>lambdaUpdate()
                .eq(KnowledgeDocumentPO::getId, documentId)
                .in(KnowledgeDocumentPO::getStatus, TERMINAL_STATUSES)) > 0;
    }

    @Override
    public void softDelete(Long documentId) {
        Objects.requireNonNull(documentId, "documentId must not be null");
        log.debug("soft deleted document {}, {} row(s) affected",
                documentId, knowledgeDocumentDAO.deleteById(documentId));
    }

    @Override
    public void softDeleteByKnowledgeBaseId(Long knowledgeBaseId) {
        Objects.requireNonNull(knowledgeBaseId, "knowledgeBaseId must not be null");
        int deleted = knowledgeDocumentDAO.delete(Wrappers.<KnowledgeDocumentPO>lambdaQuery()
                .eq(KnowledgeDocumentPO::getKnowledgeBaseId, knowledgeBaseId));
        log.debug("soft deleted the documents of knowledge base {}, {} row(s) affected",
                knowledgeBaseId, deleted);
    }

    /**
     * Applies one status move: the row is read for the columns the persistence model requires, then
     * written under the status it must still stand in, so an attempt that lost the document to a
     * concurrent transition changes nothing.
     */
    private boolean writeTransition(Long documentId, DocumentIngestStatusEnum expected,
                                    Consumer<KnowledgeDocumentPO> transition) {
        Objects.requireNonNull(documentId, "documentId must not be null");
        KnowledgeDocumentPO current = knowledgeDocumentDAO.selectById(documentId);
        if (current == null || !expected.name().equals(current.getStatus())) {
            log.debug("document {} no longer stands in {}, nothing written", documentId, expected);
            return false;
        }
        KnowledgeDocumentPO change = carrierOf(current);
        transition.accept(change);
        return knowledgeDocumentDAO.update(change,
                Wrappers.<KnowledgeDocumentPO>lambdaUpdate()
                        .eq(KnowledgeDocumentPO::getId, documentId)
                        .eq(KnowledgeDocumentPO::getStatus, expected.name())) > 0;
    }

    /** A status write carries only the columns it changes, on top of the loaded row. */
    private static KnowledgeDocumentPO carrierOf(KnowledgeDocumentPO current) {
        KnowledgeDocumentPO change = new KnowledgeDocumentPO();
        change.setId(current.getId());
        change.setTenantId(current.getTenantId());
        change.setCreateUserId(current.getCreateUserId());
        change.setCreateTime(current.getCreateTime());
        change.setIsDeleted(current.getIsDeleted());
        return change;
    }

    /** Optional filters of the list query; the tenant predicate is injected by the interceptor. */
    private static LambdaQueryWrapper<KnowledgeDocumentPO> filtered(Long knowledgeBaseId,
                                                                    DocumentIngestStatusEnum status,
                                                                    String keyword) {
        Objects.requireNonNull(knowledgeBaseId, "knowledgeBaseId must not be null");
        LambdaQueryWrapper<KnowledgeDocumentPO> filters = Wrappers.lambdaQuery();
        filters.eq(KnowledgeDocumentPO::getKnowledgeBaseId, knowledgeBaseId);
        if (status != null) {
            filters.eq(KnowledgeDocumentPO::getStatus, status.name());
        }
        if (keyword != null && !keyword.isBlank()) {
            String pattern = "%" + keyword.trim() + "%";
            filters.apply("lower(display_name) like {0}", pattern);
        }
        return filters;
    }

    /** Guards the requested window and renders it as the row window of the statement. */
    private static String window(int offset, int size) {
        if (offset < 0) {
            throw new IllegalArgumentException("offset must not be negative");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("size must be positive");
        }
        return "limit " + size + " offset " + offset;
    }
}
