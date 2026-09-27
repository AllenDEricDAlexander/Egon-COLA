package top.egon.cola.archetype.source.agent.infrastructure.knowledge.dao;

import org.apache.ibatis.annotations.Mapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.archetype.source.agent.infrastructure.knowledge.po.KnowledgeDocumentPO;
import top.egon.cola.component.common.mybatis.extension.EgonColaMapper;
import top.egon.cola.component.common.mybatis.model.EgonColaModelValidationGroups;

/**
 * CRUD component of {@code knowledge_document}.
 *
 * <p>Like the base DAO it declares no statement: the conditional status writes are condition
 * builders in the repository, where the expected status is part of the update condition.
 */
@Mapper
@Validated
public interface KnowledgeDocumentDAO extends EgonColaMapper<KnowledgeDocumentPO> {
    java.util.List<KnowledgeDocumentPO> selectFilteredPage(@org.apache.ibatis.annotations.Param("baseId") Long baseId, @org.apache.ibatis.annotations.Param("offset") int offset, @org.apache.ibatis.annotations.Param("size") int size, @org.apache.ibatis.annotations.Param("status") String status, @org.apache.ibatis.annotations.Param("keyword") String keyword);
    long countFiltered(@org.apache.ibatis.annotations.Param("baseId") Long baseId, @org.apache.ibatis.annotations.Param("status") String status, @org.apache.ibatis.annotations.Param("keyword") String keyword);
    long countByKnowledgeBaseId(@org.apache.ibatis.annotations.Param("baseId") Long baseId);
    @Validated(EgonColaModelValidationGroups.Update.class)
    int updateState(@org.apache.ibatis.annotations.Param("et") @Valid @NotNull(groups = EgonColaModelValidationGroups.Update.class) KnowledgeDocumentPO entity, @org.apache.ibatis.annotations.Param("expectedStatuses") java.util.List<String> expectedStatuses);
    int softDeleteByKnowledgeBaseId(@org.apache.ibatis.annotations.Param("baseId") Long baseId, @org.apache.ibatis.annotations.Param("userId") String userId, @org.apache.ibatis.annotations.Param("updateTime") java.time.Instant updateTime);
}
