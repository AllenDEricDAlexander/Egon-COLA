package top.egon.cola.archetype.source.agent.adapter.knowledge.converter;

import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;
import top.egon.cola.archetype.source.agent.adapter.knowledge.dto.AskKnowledgeBaseRequest;
import top.egon.cola.archetype.source.agent.adapter.knowledge.dto.RetrieveKnowledgeRequest;
import top.egon.cola.archetype.source.agent.application.knowledge.command.AskKnowledgeBaseCommand;
import top.egon.cola.archetype.source.agent.application.knowledge.command.RetrieveKnowledgeCommand;
import top.egon.cola.component.common.core.converter.BaseConverter;

import java.util.Objects;

/**
 * Mapping between the published retrieval and question bodies and their commands (Spec B §10.4).
 *
 * <p>Both directions live in this interface because the two bodies share one vocabulary — a question
 * and a query differ in what the use case does with them, not in how they are bound.
 *
 * <p>The knowledge base and the correlation id belong to the request scope, not to the body, so they
 * arrive as context instead of being mapped fields: a caller cannot address another base or forge a
 * correlation id through the payload.
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface KnowledgeQaCommandConverter extends BaseConverter<AskKnowledgeBaseRequest, AskKnowledgeBaseCommand> {

    KnowledgeQaCommandConverter INSTANCE = Mappers.getMapper(KnowledgeQaCommandConverter.class);

    /**
     * Refused, and not for want of a mapping.
     *
     * <p>The entry point of {@link BaseConverter} takes the body alone, but a question without the
     * base it is asked of names no collection and no tenant — a command built from it could not be
     * executed, and inventing a base id for it would be a guess written into a security-relevant
     * field. The context-carrying translation below is the only one this pair has.
     */
    @Override
    default AskKnowledgeBaseCommand toTarget(AskKnowledgeBaseRequest source) {
        throw new UnsupportedOperationException("a question body does not name the knowledge base it is asked of");
    }

    /** Maps one question body to its command, carrying the path identifier and the traced context. */
    default AskKnowledgeBaseCommand toAskCommand(AskKnowledgeBaseRequest source, Long knowledgeBaseId,
                                                 String traceId) {
        Objects.requireNonNull(source, "request must not be null");
        return mapAsk(source, Objects.requireNonNull(knowledgeBaseId, "knowledgeBaseId must not be null"),
                Objects.requireNonNull(traceId, "traceId must not be null"));
    }

    @Mapping(target = "knowledgeBaseId", expression = "java(knowledgeBaseId)")
    @Mapping(target = "question", source = "question")
    @Mapping(target = "topK", source = "topK")
    @Mapping(target = "traceId", expression = "java(traceId)")
    AskKnowledgeBaseCommand mapAsk(AskKnowledgeBaseRequest source, @Context Long knowledgeBaseId,
                                   @Context String traceId);

    /** Reverse of the question direction: the body as it was sent, without the request-scoped facts. */
    @Override
    @Mapping(target = "question", source = "question")
    @Mapping(target = "topK", source = "topK")
    AskKnowledgeBaseRequest toSource(AskKnowledgeBaseCommand target);

    /** Maps one retrieval body to its command, carrying the path identifier and the traced context. */
    default RetrieveKnowledgeCommand toRetrieveCommand(RetrieveKnowledgeRequest source, Long knowledgeBaseId,
                                                       String traceId) {
        Objects.requireNonNull(source, "request must not be null");
        return mapRetrieve(source, Objects.requireNonNull(knowledgeBaseId, "knowledgeBaseId must not be null"),
                Objects.requireNonNull(traceId, "traceId must not be null"));
    }

    @Mapping(target = "knowledgeBaseId", expression = "java(knowledgeBaseId)")
    @Mapping(target = "query", source = "query")
    @Mapping(target = "topK", source = "topK")
    @Mapping(target = "similarityThreshold", source = "similarityThreshold")
    @Mapping(target = "traceId", expression = "java(traceId)")
    RetrieveKnowledgeCommand mapRetrieve(RetrieveKnowledgeRequest source, @Context Long knowledgeBaseId,
                                         @Context String traceId);

    /** Reverse of the retrieval direction: the body as it was sent, without the request-scoped facts. */
    @Mapping(target = "query", source = "query")
    @Mapping(target = "topK", source = "topK")
    @Mapping(target = "similarityThreshold", source = "similarityThreshold")
    RetrieveKnowledgeRequest toRetrieveRequest(RetrieveKnowledgeCommand target);
}
