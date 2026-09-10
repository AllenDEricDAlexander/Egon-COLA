package top.egon.cola.archetype.source.agent.adapter.knowledge.converter;

import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;
import top.egon.cola.archetype.source.agent.adapter.knowledge.vo.KnowledgeDocumentVO;
import top.egon.cola.archetype.source.agent.domain.knowledge.model.KnowledgeDocumentBO;
import top.egon.cola.component.common.core.converter.BaseConverter;

/**
 * Mapping from the document domain carrier to its published representation (Spec B §10.4).
 *
 * <p>The correlation id is a request-scoped fact the carrier does not hold, so it arrives as context.
 * The stored text is reduced to its length here rather than at the boundary: one place decides which
 * part of a document may leave the process (§9.2.8 — the text itself never does).
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface KnowledgeDocumentVoConverter extends BaseConverter<KnowledgeDocumentBO, KnowledgeDocumentVO> {

    KnowledgeDocumentVoConverter INSTANCE = Mappers.getMapper(KnowledgeDocumentVoConverter.class);

    @Override
    default KnowledgeDocumentVO toTarget(KnowledgeDocumentBO source) {
        return toVO(source, null);
    }

    /** Renders one document with the correlation id of the request that asked for it. */
    default KnowledgeDocumentVO toVO(KnowledgeDocumentBO source, String traceId) {
        return map(source, traceId);
    }

    @Mapping(target = "documentId", source = "documentId")
    @Mapping(target = "knowledgeBaseId", source = "knowledgeBaseId")
    @Mapping(target = "displayName", source = "displayName")
    @Mapping(target = "fileName", source = "fileName")
    @Mapping(target = "mimeType", source = "mimeType")
    @Mapping(target = "sizeBytes", source = "sizeBytes")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "chunkCount", source = "chunkCount")
    @Mapping(target = "attemptCount", source = "attemptCount")
    @Mapping(target = "contentChars", expression = "java(contentChars(source))")
    @Mapping(target = "errorCode", source = "errorCode")
    @Mapping(target = "errorMessage", source = "errorMessage")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedAt", source = "updatedAt")
    @Mapping(target = "traceId", expression = "java(traceId)")
    KnowledgeDocumentVO map(KnowledgeDocumentBO source, @Context String traceId);

    /** Length of the persisted text, which is what the contract publishes instead of the text. */
    default Integer contentChars(KnowledgeDocumentBO source) {
        String content = source.content();
        return content == null ? null : content.length();
    }

    /**
     * The reverse direction of {@link BaseConverter}, which this pair cannot honour.
     *
     * <p>A document representation carries neither the text, nor its digest, nor the location of the
     * original, so a carrier rebuilt from it would have to invent a fingerprint and a storage key and
     * would look persistable while describing no stored file at all. Failing loudly keeps that
     * impossibility visible; every document a caller sees is rendered from the carrier, never the
     * other way round.
     */
    @Override
    default KnowledgeDocumentBO toSource(KnowledgeDocumentVO target) {
        throw new UnsupportedOperationException("a knowledge document representation carries no stored text");
    }
}
