package top.egon.cola.archetype.source.agent.infrastructure.knowledge.repo.dao;

import org.apache.ibatis.annotations.Mapper;
import top.egon.cola.archetype.source.agent.infrastructure.knowledge.repo.po.KnowledgeBasePO;
import top.egon.cola.component.common.mybatis.extension.EgonColaMapper;

/**
 * CRUD component of {@code knowledge_base}.
 *
 * <p>No statement is declared here: every query is assembled from the MyBatis-Plus condition
 * builders in the repository, so the tenant predicate stays injected by the interceptor instead of
 * being repeated in hand written SQL.
 */
@Mapper
public interface KnowledgeBaseDAO extends EgonColaMapper<KnowledgeBasePO> {
}
