package top.egon.cola.archetype.source.agent.infrastructure.knowledge.dao;

import org.apache.ibatis.annotations.Mapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.archetype.source.agent.infrastructure.knowledge.po.KnowledgeBasePO;
import top.egon.cola.component.common.mybatis.extension.EgonColaMapper;
import top.egon.cola.component.common.mybatis.model.EgonColaModelValidationGroups;

/**
 * CRUD component of {@code knowledge_base}.
 *
 * <p>No statement is declared here: every query is assembled from the MyBatis-Plus condition
 * builders in the repository, so the tenant predicate stays injected by the interceptor instead of
 * being repeated in hand written SQL.
 */
@Mapper
@Validated
public interface KnowledgeBaseDAO extends EgonColaMapper<KnowledgeBasePO> {
    java.util.List<KnowledgeBasePO> selectFilteredPage(@org.apache.ibatis.annotations.Param("offset") int offset, @org.apache.ibatis.annotations.Param("size") int size, @org.apache.ibatis.annotations.Param("keyword") String keyword, @org.apache.ibatis.annotations.Param("embeddingModel") String embeddingModel);
    long countFiltered(@org.apache.ibatis.annotations.Param("keyword") String keyword, @org.apache.ibatis.annotations.Param("embeddingModel") String embeddingModel);
    @Validated(EgonColaModelValidationGroups.Update.class)
    int updateNameAndDescription(@org.apache.ibatis.annotations.Param("et") @Valid @NotNull(groups = EgonColaModelValidationGroups.Update.class) KnowledgeBasePO entity);
}
