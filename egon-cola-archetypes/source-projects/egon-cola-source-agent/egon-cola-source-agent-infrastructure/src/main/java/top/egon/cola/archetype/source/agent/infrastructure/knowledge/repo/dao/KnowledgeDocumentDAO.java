package top.egon.cola.archetype.source.agent.infrastructure.knowledge.repo.dao;

import org.apache.ibatis.annotations.Mapper;
import top.egon.cola.archetype.source.agent.infrastructure.knowledge.repo.po.KnowledgeDocumentPO;
import top.egon.cola.component.common.mybatis.extension.EgonColaMapper;

/**
 * CRUD component of {@code knowledge_document}.
 *
 * <p>Like the base DAO it declares no statement: the conditional status writes are condition
 * builders in the repository, where the expected status is part of the update condition.
 */
@Mapper
public interface KnowledgeDocumentDAO extends EgonColaMapper<KnowledgeDocumentPO> {
    java.util.List<KnowledgeDocumentPO> selectFilteredPage(@org.apache.ibatis.annotations.Param("baseId") Long baseId, @org.apache.ibatis.annotations.Param("offset") int offset, @org.apache.ibatis.annotations.Param("size") int size, @org.apache.ibatis.annotations.Param("status") String status, @org.apache.ibatis.annotations.Param("keyword") String keyword);
    long countFiltered(@org.apache.ibatis.annotations.Param("baseId") Long baseId, @org.apache.ibatis.annotations.Param("status") String status, @org.apache.ibatis.annotations.Param("keyword") String keyword);
    long countByKnowledgeBaseId(@org.apache.ibatis.annotations.Param("baseId") Long baseId);
    int updateState(@org.apache.ibatis.annotations.Param("et") KnowledgeDocumentPO entity, @org.apache.ibatis.annotations.Param("expectedStatuses") java.util.List<String> expectedStatuses);
    int softDeleteByKnowledgeBaseId(@org.apache.ibatis.annotations.Param("baseId") Long baseId, @org.apache.ibatis.annotations.Param("userId") String userId, @org.apache.ibatis.annotations.Param("updateTime") java.time.Instant updateTime);
}
