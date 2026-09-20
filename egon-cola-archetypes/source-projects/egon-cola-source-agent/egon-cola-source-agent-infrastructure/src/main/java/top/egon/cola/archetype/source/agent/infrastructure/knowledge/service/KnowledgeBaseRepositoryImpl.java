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
import top.egon.cola.archetype.source.agent.domain.knowledge.repository.KnowledgeBaseRepository;
import top.egon.cola.archetype.source.agent.infrastructure.knowledge.repo.converter.KnowledgeBasePOConverter;
import top.egon.cola.archetype.source.agent.infrastructure.knowledge.repo.dao.KnowledgeBaseDAO;
import top.egon.cola.archetype.source.agent.infrastructure.knowledge.repo.po.KnowledgeBasePO;
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
@Repository("knowledgeBaseRepository")
@RequiredArgsConstructor
public class KnowledgeBaseRepositoryImpl extends EgonColaRepository<KnowledgeBaseDAO, KnowledgeBasePO> implements KnowledgeBaseRepository {
    @Getter
    @Qualifier("knowledgeBaseDAO")
    private final KnowledgeBaseDAO baseMapper;
    @Getter(AccessLevel.PROTECTED)
    @Qualifier("egon.cola.component.mybatis-plus-top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties")
    private final EgonColaMybatisPlusProperties properties;
    @Qualifier("egonColaValidationUtils")
    private final ValidationUtils validation;

    @Override
    public KnowledgeBaseBO insert(KnowledgeBaseBO knowledgeBase) {
        validation.validate(Objects.requireNonNull(knowledgeBase, "knowledgeBase"));
        KnowledgeBasePO stored = KnowledgeBasePOConverter.INSTANCE.toSource(knowledgeBase);
        if (!save(stored)) { throw new IllegalStateException("INSERT_AFFECTED_ZERO_ROWS"); }
        return KnowledgeBasePOConverter.INSTANCE.toTarget(stored);
    }

    @Override
    public Optional<KnowledgeBaseBO> findById(Long knowledgeBaseId) {
        return Optional.ofNullable(getById(knowledgeBaseId)).map(KnowledgeBasePOConverter.INSTANCE::toTarget);
    }

    @Override
    public List<KnowledgeBaseBO> page(int offset, int size, String keyword, String embeddingModel) {
        validatePage(offset, size);
        return KnowledgeBasePOConverter.INSTANCE.toTargetList(baseMapper.selectFilteredPage(offset, size, pattern(keyword), normalized(embeddingModel)));
    }

    @Override
    public long count(String keyword, String embeddingModel) { return baseMapper.countFiltered(pattern(keyword), normalized(embeddingModel)); }

    @Override
    @Transactional
    public void updateNameAndDescription(Long knowledgeBaseId, String name, String description) {
        validation.validate(new RenameCommand(knowledgeBaseId, name, description));
        KnowledgeBasePO current = getById(knowledgeBaseId);
        if (current == null) { return; }
        KnowledgeBasePO change = new KnowledgeBasePO().setName(name).setDescription(description);
        KnowledgeBasePOConverter.INSTANCE.updateMetadata(change, current);
        EgonColaModelValidationUtils.validateBusiness(change, EgonColaModelValidationGroups.Operation.UPDATE);
        if (baseMapper.updateNameAndDescription(change) != 1) {
            throw new org.springframework.dao.OptimisticLockingFailureException("VERSIONED_WRITE_CONFLICT");
        }
    }

    @Override
    @Transactional
    public void softDelete(Long knowledgeBaseId) { removeById(knowledgeBaseId); }

    private static String normalized(String value) { return value == null || value.isBlank() ? null : value.trim(); }

    private void validatePage(int offset, int size) {
        validation.validate(new PageQuery(offset, size));
        if (size > properties.getPagination().getMaxPageSize()) { throw new IllegalArgumentException("PAGE_SIZE_LIMIT_EXCEEDED"); }
    }

    private record RenameCommand(@jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Positive Long id,
                                 @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max = 128) String name,
                                 @jakarta.validation.constraints.Size(max = 512) String description) { }

    private record PageQuery(@jakarta.validation.constraints.PositiveOrZero int offset,
                             @jakarta.validation.constraints.Min(1) @jakarta.validation.constraints.Max(500) int size) { }

    private static String pattern(String keyword) { return keyword == null || keyword.isBlank() ? null : "%" + keyword.trim() + "%"; }
}
