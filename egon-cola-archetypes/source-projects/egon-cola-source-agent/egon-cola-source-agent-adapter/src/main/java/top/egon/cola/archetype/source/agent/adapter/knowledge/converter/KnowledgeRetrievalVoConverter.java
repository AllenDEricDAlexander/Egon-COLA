package top.egon.cola.archetype.source.agent.adapter.knowledge.converter;

import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;
import top.egon.cola.archetype.source.agent.adapter.knowledge.vo.KnowledgeRetrievalVO;
import top.egon.cola.archetype.source.agent.adapter.knowledge.vo.KnowledgeRetrievedChunkVO;
import top.egon.cola.archetype.source.agent.domain.knowledge.model.KnowledgeRetrievalBO;
import top.egon.cola.archetype.source.agent.domain.knowledge.model.KnowledgeRetrievedChunkBO;
import top.egon.cola.component.common.core.converter.BaseConverter;

import java.util.Objects;

/**
 * Mapping from one retrieval outcome to its published representation (Spec B §10.4).
 *
 * <p>The query and the correlation id are request-scoped facts the carrier does not hold — it carries
 * only what the vector store produced — so both arrive as context instead of being mapped fields.
 * The reverse direction is complete: everything the representation adds beyond the carrier is the
 * caller's own input, so a carrier rebuilt from it describes the same retrieval truthfully.
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface KnowledgeRetrievalVoConverter extends BaseConverter<KnowledgeRetrievalBO, KnowledgeRetrievalVO> {

    KnowledgeRetrievalVoConverter INSTANCE = Mappers.getMapper(KnowledgeRetrievalVoConverter.class);

    @Override
    default KnowledgeRetrievalVO toTarget(KnowledgeRetrievalBO source) {
        return toVO(source, null, null);
    }

    /** Renders one retrieval with the query and the correlation id of the request that asked for it. */
    default KnowledgeRetrievalVO toVO(KnowledgeRetrievalBO source, String query, String traceId) {
        Objects.requireNonNull(source, "retrieval must not be null");
        return map(source, query, traceId);
    }

    /**
     * Both request-scoped facts are plain parameters rather than contexts: MapStruct admits one
     * context per type, and the query and the correlation id are both text.
     */
    @Mapping(target = "items", source = "source.items")
    @Mapping(target = "query", expression = "java(query)")
    @Mapping(target = "embeddingModel", source = "source.embeddingModel")
    @Mapping(target = "traceId", expression = "java(traceId)")
    KnowledgeRetrievalVO map(KnowledgeRetrievalBO source, String query, String traceId);

    /** Renders one matched chunk of the response. */
    KnowledgeRetrievedChunkVO toChunk(KnowledgeRetrievedChunkBO source);

    /**
     * Reverse direction: the chained chunks and the model, which is all the carrier states.
     *
     * <p>The query and the trace id belong to the caller that supplied them and are not written back
     * into a carrier that never held them.
     */
    @Override
    @Mapping(target = "items", source = "items")
    @Mapping(target = "embeddingModel", source = "embeddingModel")
    KnowledgeRetrievalBO toSource(KnowledgeRetrievalVO target);
}
