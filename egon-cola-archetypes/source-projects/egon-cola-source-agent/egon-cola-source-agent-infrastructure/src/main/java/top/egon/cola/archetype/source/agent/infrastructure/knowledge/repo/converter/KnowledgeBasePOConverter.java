package top.egon.cola.archetype.source.agent.infrastructure.knowledge.repo.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;
import top.egon.cola.archetype.source.agent.domain.knowledge.model.KnowledgeBaseBO;
import top.egon.cola.archetype.source.agent.domain.knowledge.model.KnowledgeChunkConfigBO;
import top.egon.cola.archetype.source.agent.infrastructure.knowledge.repo.po.KnowledgeBasePO;
import top.egon.cola.component.common.core.converter.BaseConverter;

/**
 * Mapping between one {@code knowledge_base} row and its domain carrier.
 *
 * <p>The read direction projects every stored column; the write direction maps only the columns the
 * business owns — the identifier is generated, and the tenant, audit and soft-delete columns are
 * filled by the persistence layer from the request context. The {@code chunk_config} jsonb text and
 * its carrier are converted here so a caller never sees the raw column.
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface KnowledgeBasePOConverter extends BaseConverter<KnowledgeBasePO, KnowledgeBaseBO> {

    ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    KnowledgeBasePOConverter INSTANCE = Mappers.getMapper(KnowledgeBasePOConverter.class);

    @Override
    @Mapping(target = "knowledgeBaseId", source = "id")
    @Mapping(target = "tenantId", source = "tenantId")
    @Mapping(target = "code", source = "code")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "embeddingModel", source = "embeddingModel")
    @Mapping(target = "chunkStrategy", source = "chunkStrategy")
    @Mapping(target = "chunkConfig", expression = "java(toChunkConfig(source.getChunkConfig()))")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "createdAt", source = "createTime")
    @Mapping(target = "updatedAt", source = "updateTime")
    KnowledgeBaseBO toTarget(KnowledgeBasePO source);

    @Override
    @Mapping(target = "code", source = "code")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "embeddingModel", source = "embeddingModel")
    @Mapping(target = "chunkStrategy", source = "chunkStrategy")
    @Mapping(target = "chunkConfig", expression = "java(toChunkConfigJson(source.chunkConfig()))")
    @Mapping(target = "status", source = "status")
    KnowledgeBasePO toSource(KnowledgeBaseBO source);

    /** Projects the stored jsonb document to the chunking carrier. */
    default KnowledgeChunkConfigBO toChunkConfig(String chunkConfig) {
        if (chunkConfig == null || chunkConfig.isBlank()) {
            throw new IllegalArgumentException("chunk_config must not be blank");
        }
        try {
            return OBJECT_MAPPER.readValue(chunkConfig, KnowledgeChunkConfigBO.class);
        } catch (JsonProcessingException failure) {
            throw new IllegalArgumentException("chunk_config is not a chunk configuration: "
                    + failure.getOriginalMessage(), failure);
        }
    }

    /** Renders the chunking carrier as the jsonb document stored in the column. */
    default String toChunkConfigJson(KnowledgeChunkConfigBO chunkConfig) {
        if (chunkConfig == null) {
            throw new IllegalArgumentException("chunkConfig must not be null");
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(chunkConfig);
        } catch (JsonProcessingException failure) {
            throw new IllegalArgumentException("chunkConfig cannot be rendered as json", failure);
        }
    }
}
