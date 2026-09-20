package top.egon.cola.archetype.source.agent.infrastructure.knowledge.service;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.archetype.source.agent.domain.knowledge.model.*;
import top.egon.cola.archetype.source.agent.domain.knowledge.repository.KnowledgeDocumentRepository;
import top.egon.cola.archetype.source.agent.infrastructure.knowledge.repo.converter.KnowledgeDocumentPOConverter;
import top.egon.cola.archetype.source.agent.infrastructure.knowledge.repo.dao.KnowledgeDocumentDAO;
import top.egon.cola.archetype.source.agent.infrastructure.knowledge.repo.po.KnowledgeDocumentPO;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;
import top.egon.cola.component.common.mybatis.extension.EgonColaRepository;
import top.egon.cola.component.common.mybatis.model.EgonColaModelValidationUtils;
import top.egon.cola.component.common.mybatis.model.EgonColaModelValidationGroups;
import java.util.List;
import java.util.Optional;
import java.util.Objects;

/** Explicit query SQL and versioned commands for the existing knowledge domain port. */
@Slf4j
@Validated
@Repository("knowledgeDocumentRepository")
@RequiredArgsConstructor
public class KnowledgeDocumentRepositoryImpl extends EgonColaRepository<KnowledgeDocumentDAO, KnowledgeDocumentPO> implements KnowledgeDocumentRepository {
    @Getter
    @Qualifier("knowledgeDocumentDAO")
    private final KnowledgeDocumentDAO baseMapper;
    @Getter(AccessLevel.PROTECTED)
    @Qualifier("egon.cola.component.mybatis-plus-top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties")
    private final EgonColaMybatisPlusProperties properties;
    @Qualifier("egonColaValidationUtils")
    private final ValidationUtils validation;
    @Qualifier("egonColaMdcUserIdProvider")
    private final top.egon.cola.component.common.mybatis.business.EgonColaUserIdProvider userIdProvider;
    @Qualifier("agentClock")
    private final java.time.Clock clock;

    private static final List<String> TERMINAL_STATUSES = java.util.Arrays.stream(DocumentIngestStatusEnum.values())
            .filter(DocumentIngestStatusEnum::isTerminal).map(Enum::name).toList();

    @Override
    public KnowledgeDocumentBO insert(KnowledgeDocumentBO document) {
        validation.validate(Objects.requireNonNull(document, "document"));
        KnowledgeDocumentPO stored = KnowledgeDocumentPOConverter.INSTANCE.toSource(document);
        if (!save(stored)) { throw new IllegalStateException("INSERT_AFFECTED_ZERO_ROWS"); }
        return KnowledgeDocumentPOConverter.INSTANCE.toTarget(stored);
    }

    @Override
    public Optional<KnowledgeDocumentBO> findById(Long documentId) {
        return Optional.ofNullable(getById(documentId)).map(KnowledgeDocumentPOConverter.INSTANCE::toTarget);
    }

    @Override
    public List<KnowledgeDocumentBO> findByIds(java.util.Collection<Long> documentIds) {
        validation.validate(new IdsQuery(documentIds));
        if (documentIds.isEmpty()) { return List.of(); }
        return KnowledgeDocumentPOConverter.INSTANCE.toTargetList(baseMapper.selectActiveByIds(documentIds));
    }

    @Override
    public List<KnowledgeDocumentBO> page(Long knowledgeBaseId, int offset, int size, DocumentIngestStatusEnum status, String keyword) {
        validation.validate(new RootCommand(knowledgeBaseId));
        validatePage(offset, size);
        return KnowledgeDocumentPOConverter.INSTANCE.toTargetList(baseMapper.selectFilteredPage(knowledgeBaseId, offset, size,
                status == null ? null : status.name(), pattern(keyword)));
    }

    @Override
    public long count(Long knowledgeBaseId, DocumentIngestStatusEnum status, String keyword) {
        validation.validate(new RootCommand(knowledgeBaseId));
        return baseMapper.countFiltered(knowledgeBaseId, status == null ? null : status.name(), pattern(keyword));
    }

    @Override
    public long countByKnowledgeBaseId(Long knowledgeBaseId) {
        validation.validate(new RootCommand(knowledgeBaseId));
        return baseMapper.countByKnowledgeBaseId(knowledgeBaseId);
    }

    @Override
    @Transactional
    public boolean markProcessing(Long documentId, int attemptCount) {
        validation.validate(new CountCommand(attemptCount));
        return writeTransition(documentId, DocumentIngestStatusEnum.PENDING,
                change -> change.setStatus(DocumentIngestStatusEnum.PROCESSING.name()).setAttemptCount(attemptCount).setErrorCode(null).setErrorMessage(null));
    }

    @Override
    @Transactional
    public boolean markSucceeded(Long documentId, int chunkCount) {
        validation.validate(new CountCommand(chunkCount));
        return writeTransition(documentId, DocumentIngestStatusEnum.PROCESSING,
                change -> change.setStatus(DocumentIngestStatusEnum.SUCCEEDED.name()).setChunkCount(chunkCount));
    }

    @Override
    @Transactional
    public boolean markRetryPending(Long documentId, int attemptCount, String errorCode, String errorMessage) {
        validation.validate(new ErrorCommand(errorCode, errorMessage));
        validation.validate(new CountCommand(attemptCount));
        return writeTransition(documentId, DocumentIngestStatusEnum.PROCESSING,
                change -> change.setStatus(DocumentIngestStatusEnum.PENDING.name()).setAttemptCount(attemptCount).setErrorCode(errorCode).setErrorMessage(errorMessage));
    }

    @Override
    @Transactional
    public boolean markDead(Long documentId, String errorCode, String errorMessage) {
        validation.validate(new ErrorCommand(errorCode, errorMessage));
        return writeTransition(documentId, DocumentIngestStatusEnum.PROCESSING,
                change -> change.setStatus(DocumentIngestStatusEnum.DEAD.name()).setErrorCode(errorCode).setErrorMessage(errorMessage));
    }

    @Override
    @Transactional
    public boolean resetForReingest(Long documentId) {
        KnowledgeDocumentPO current = getById(documentId);
        if (current == null || !TERMINAL_STATUSES.contains(current.getStatus())) { return false; }
        KnowledgeDocumentPO change = carrierOf(current).setStatus(DocumentIngestStatusEnum.PENDING.name())
                .setChunkCount(0).setAttemptCount(0).setErrorCode(null).setErrorMessage(null);
        EgonColaModelValidationUtils.validateBusiness(change, EgonColaModelValidationGroups.Operation.UPDATE);
        return baseMapper.updateState(change, TERMINAL_STATUSES) == 1;
    }

    @Override
    @Transactional
    public void softDelete(Long documentId) { removeById(documentId); }

    @Override
    @Transactional
    public void softDeleteByKnowledgeBaseId(Long knowledgeBaseId) {
        validation.validate(new RootCommand(knowledgeBaseId));
        String userId = userIdProvider.currentUserId();
        if (userId == null || userId.isBlank()) { throw new IllegalStateException("USER_CONTEXT_MISSING"); }
        baseMapper.softDeleteByKnowledgeBaseId(knowledgeBaseId, userId, clock.instant().truncatedTo(java.time.temporal.ChronoUnit.MICROS));
    }

    private boolean writeTransition(Long documentId, DocumentIngestStatusEnum expected, java.util.function.Consumer<KnowledgeDocumentPO> transition) {
        KnowledgeDocumentPO current = getById(documentId);
        if (current == null || !expected.name().equals(current.getStatus())) { return false; }
        KnowledgeDocumentPO change = carrierOf(current);
        transition.accept(change);
        EgonColaModelValidationUtils.validateBusiness(change, EgonColaModelValidationGroups.Operation.UPDATE);
        return baseMapper.updateState(change, List.of(expected.name())) == 1;
    }

    private static KnowledgeDocumentPO carrierOf(KnowledgeDocumentPO current) {
        KnowledgeDocumentPO change = new KnowledgeDocumentPO();
        KnowledgeDocumentPOConverter.INSTANCE.updateMetadata(change, current);
        return change;
    }

    private record CountCommand(@jakarta.validation.constraints.PositiveOrZero int count) { }
    private record ErrorCommand(@jakarta.validation.constraints.Size(max = 64) String code,
                                @jakarta.validation.constraints.Size(max = 512) String message) { }
    private record RootCommand(@jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Positive Long id) { }
    private record IdsQuery(@jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Size(max = 10000)
                           java.util.Collection<@jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Positive Long> ids) { }

    private void validatePage(int offset, int size) {
        validation.validate(new PageQuery(offset, size));
        if (size > properties.getPagination().getMaxPageSize()) { throw new IllegalArgumentException("PAGE_SIZE_LIMIT_EXCEEDED"); }
    }

    private record PageQuery(@jakarta.validation.constraints.PositiveOrZero int offset,
                             @jakarta.validation.constraints.Min(1) @jakarta.validation.constraints.Max(500) int size) { }

    private static String pattern(String keyword) { return keyword == null || keyword.isBlank() ? null : "%" + keyword.trim() + "%"; }
}
