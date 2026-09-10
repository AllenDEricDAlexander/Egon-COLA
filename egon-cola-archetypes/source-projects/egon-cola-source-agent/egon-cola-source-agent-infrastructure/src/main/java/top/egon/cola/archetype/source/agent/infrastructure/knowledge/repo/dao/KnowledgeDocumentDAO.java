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
}
