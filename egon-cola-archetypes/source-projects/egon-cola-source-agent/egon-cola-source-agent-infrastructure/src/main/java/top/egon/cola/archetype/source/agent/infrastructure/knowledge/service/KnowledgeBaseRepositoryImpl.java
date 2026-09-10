package top.egon.cola.archetype.source.agent.infrastructure.knowledge.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import top.egon.cola.archetype.source.agent.domain.knowledge.model.KnowledgeBaseBO;
import top.egon.cola.archetype.source.agent.domain.knowledge.repository.KnowledgeBaseRepository;
import top.egon.cola.archetype.source.agent.infrastructure.knowledge.repo.converter.KnowledgeBasePOConverter;
import top.egon.cola.archetype.source.agent.infrastructure.knowledge.repo.dao.KnowledgeBaseDAO;
import top.egon.cola.archetype.source.agent.infrastructure.knowledge.repo.po.KnowledgeBasePO;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * MyBatis-Plus implementation of the knowledge base port.
 *
 * <p>Every statement is assembled from the condition builders, so no query carries a tenant
 * predicate: the scope comes from the request tenant through the shared interceptor chain. The soft
 * delete uses the logic-delete support of the model, which also keeps deleted rows out of the
 * generated conditions.
 */
@Slf4j
@Repository("knowledgeBaseRepository")
@RequiredArgsConstructor
public class KnowledgeBaseRepositoryImpl implements KnowledgeBaseRepository {

    private final @Qualifier("knowledgeBaseDAO") KnowledgeBaseDAO knowledgeBaseDAO;

    @Override
    public KnowledgeBaseBO insert(KnowledgeBaseBO knowledgeBase) {
        Objects.requireNonNull(knowledgeBase, "knowledgeBase must not be null");
        KnowledgeBasePO stored = KnowledgeBasePOConverter.INSTANCE.toSource(knowledgeBase);
        knowledgeBaseDAO.insert(stored);
        log.debug("stored knowledge base {} with code {}", stored.getId(), stored.getCode());
        return KnowledgeBasePOConverter.INSTANCE.toTarget(stored);
    }

    @Override
    public Optional<KnowledgeBaseBO> findById(Long knowledgeBaseId) {
        Objects.requireNonNull(knowledgeBaseId, "knowledgeBaseId must not be null");
        return Optional.ofNullable(knowledgeBaseDAO.selectById(knowledgeBaseId))
                .map(KnowledgeBasePOConverter.INSTANCE::toTarget);
    }

    @Override
    public List<KnowledgeBaseBO> page(int offset, int size, String keyword, String embeddingModel) {
        return KnowledgeBasePOConverter.INSTANCE.toTargetList(knowledgeBaseDAO.selectList(
                filtered(keyword, embeddingModel)
                        .orderByDesc(KnowledgeBasePO::getCreateTime, KnowledgeBasePO::getId)
                        .last(window(offset, size))));
    }

    @Override
    public long count(String keyword, String embeddingModel) {
        return knowledgeBaseDAO.selectCount(filtered(keyword, embeddingModel));
    }

    @Override
    public void updateNameAndDescription(Long knowledgeBaseId, String name, String description) {
        Objects.requireNonNull(knowledgeBaseId, "knowledgeBaseId must not be null");
        KnowledgeBasePO change = new KnowledgeBasePO().setName(name);
        // The description is written from the wrapper so that clearing it stays possible: an entity
        // field left null is skipped by the update strategy.
        int updated = knowledgeBaseDAO.update(change, Wrappers.<KnowledgeBasePO>lambdaUpdate()
                .set(KnowledgeBasePO::getDescription, description)
                .eq(KnowledgeBasePO::getId, knowledgeBaseId));
        log.debug("renamed knowledge base {}, {} row(s) affected", knowledgeBaseId, updated);
    }

    @Override
    public void softDelete(Long knowledgeBaseId) {
        Objects.requireNonNull(knowledgeBaseId, "knowledgeBaseId must not be null");
        log.debug("soft deleted knowledge base {}, {} row(s) affected",
                knowledgeBaseId, knowledgeBaseDAO.deleteById(knowledgeBaseId));
    }

    /** Optional filters of the list query; the tenant predicate is injected by the interceptor. */
    private static LambdaQueryWrapper<KnowledgeBasePO> filtered(String keyword, String embeddingModel) {
        LambdaQueryWrapper<KnowledgeBasePO> filters = Wrappers.lambdaQuery();
        if (keyword != null && !keyword.isBlank()) {
            // The documented shape matches code and name case-insensitively, like the ILIKE it
            // replaces; the pattern is bound as a parameter, never concatenated.
            String pattern = "%" + keyword.trim() + "%";
            filters.apply("(lower(code) like {0} or lower(name) like {1})", pattern, pattern);
        }
        if (embeddingModel != null && !embeddingModel.isBlank()) {
            filters.eq(KnowledgeBasePO::getEmbeddingModel, embeddingModel.trim());
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
