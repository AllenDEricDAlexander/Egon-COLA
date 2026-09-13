package top.egon.cola.archetype.source.agent.infrastructure.knowledge.repo.converter;

import org.mapstruct.Mapper;
import org.mapstruct.BeanMapping;
import org.mapstruct.Builder;
import org.mapstruct.MappingTarget;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;
import top.egon.cola.archetype.source.agent.domain.knowledge.model.KnowledgeDocumentBO;
import top.egon.cola.archetype.source.agent.infrastructure.knowledge.repo.po.KnowledgeDocumentPO;
import top.egon.cola.component.common.core.converter.BaseConverter;

/**
 * Mapping between one {@code knowledge_document} row and its domain carrier.
 *
 * <p>As with the base converter, the write direction maps only the columns the upload owns: the
 * identifier is generated, and the tenant, audit and soft-delete columns are filled by the
 * persistence layer. Status writes never go through this converter — they name the status they
 * expect to leave.
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface KnowledgeDocumentPOConverter
        extends BaseConverter<KnowledgeDocumentPO, KnowledgeDocumentBO> {

    KnowledgeDocumentPOConverter INSTANCE = Mappers.getMapper(KnowledgeDocumentPOConverter.class);

    @Override
    @Mapping(target = "documentId", source = "id")
    @Mapping(target = "tenantId", source = "tenantId")
    @Mapping(target = "knowledgeBaseId", source = "knowledgeBaseId")
    @Mapping(target = "displayName", source = "displayName")
    @Mapping(target = "fileName", source = "fileName")
    @Mapping(target = "mimeType", source = "mimeType")
    @Mapping(target = "sizeBytes", source = "sizeBytes")
    @Mapping(target = "contentHash", source = "contentHash")
    @Mapping(target = "storageType", source = "storageType")
    @Mapping(target = "storageKey", source = "storageKey")
    @Mapping(target = "content", source = "content")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "chunkCount", source = "chunkCount")
    @Mapping(target = "attemptCount", source = "attemptCount")
    @Mapping(target = "errorCode", source = "errorCode")
    @Mapping(target = "errorMessage", source = "errorMessage")
    @Mapping(target = "createdAt", source = "createTime")
    @Mapping(target = "updatedAt", source = "updateTime")
    KnowledgeDocumentBO toTarget(KnowledgeDocumentPO source);

    @Override
    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createUserId", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateUserId", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "knowledgeBaseId", source = "knowledgeBaseId")
    @Mapping(target = "displayName", source = "displayName")
    @Mapping(target = "fileName", source = "fileName")
    @Mapping(target = "mimeType", source = "mimeType")
    @Mapping(target = "sizeBytes", source = "sizeBytes")
    @Mapping(target = "contentHash", source = "contentHash")
    @Mapping(target = "storageType", source = "storageType")
    @Mapping(target = "storageKey", source = "storageKey")
    @Mapping(target = "content", source = "content")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "chunkCount", source = "chunkCount")
    @Mapping(target = "attemptCount", source = "attemptCount")
    @Mapping(target = "errorCode", source = "errorCode")
    @Mapping(target = "errorMessage", source = "errorMessage")
    KnowledgeDocumentPO toSource(KnowledgeDocumentBO source);
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "tenantId", source = "tenantId")
    @Mapping(target = "createUserId", source = "createUserId")
    @Mapping(target = "createTime", source = "createTime")
    @Mapping(target = "version", source = "version")
    void updateMetadata(@MappingTarget KnowledgeDocumentPO target, KnowledgeDocumentPO source);
}
