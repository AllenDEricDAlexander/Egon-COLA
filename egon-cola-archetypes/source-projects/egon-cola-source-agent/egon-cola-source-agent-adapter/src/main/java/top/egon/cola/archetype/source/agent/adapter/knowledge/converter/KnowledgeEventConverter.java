package top.egon.cola.archetype.source.agent.adapter.knowledge.converter;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;
import top.egon.cola.archetype.source.agent.adapter.knowledge.vo.KnowledgeQaEventVO;
import top.egon.cola.archetype.source.agent.adapter.knowledge.vo.KnowledgeQaReferenceVO;
import top.egon.cola.archetype.source.agent.domain.knowledge.model.KnowledgeQaEvent;
import top.egon.cola.archetype.source.agent.domain.knowledge.model.KnowledgeRetrievedChunkBO;
import top.egon.cola.component.common.core.converter.BaseConverter;

import java.util.List;

/**
 * Mapping from one knowledge answer event to its published payload (Spec B §10.4).
 *
 * <p>The event a client receives carries the code and the message a failure is meant to be acted on,
 * which the domain already reduced to the safe vocabulary; the adapter maps them and adds nothing, so
 * a provider message cannot reach the stream by accident.
 *
 * <p>A reference is rendered without the chunk text: the domain model carries it for the retrieval
 * endpoint, but the event contract publishes identities, and the converter is where that narrowing
 * happens. The reverse direction is complete — an event whose references carry no text is exactly
 * what the domain builds for citations ({@code withoutContent()}), so nothing has to be invented.
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface KnowledgeEventConverter extends BaseConverter<KnowledgeQaEvent, KnowledgeQaEventVO> {

    KnowledgeEventConverter INSTANCE = Mappers.getMapper(KnowledgeEventConverter.class);

    @Override
    @Mapping(target = "answerId", source = "answerId")
    @Mapping(target = "sequence", source = "sequence")
    @Mapping(target = "type", source = "type")
    @Mapping(target = "retrievals", expression = "java(references(source))")
    @Mapping(target = "delta", source = "delta")
    @Mapping(target = "answer", source = "answer")
    @Mapping(target = "code", source = "errorCode")
    @Mapping(target = "message", source = "errorMessage")
    @Mapping(target = "retryable", source = "retryable")
    @Mapping(target = "occurredAt", source = "occurredAt")
    @Mapping(target = "traceId", source = "traceId")
    KnowledgeQaEventVO toTarget(KnowledgeQaEvent source);

    /**
     * The wire references of one event, or {@code null} for the variants that publish none.
     *
     * <p>Which variants carry references is a property of the event, not of how many chunks matched:
     * the two events the contract gives references to always carry the array, empty when nothing
     * matched, while a progress or failure event has no references field at all — stating the field
     * there would invite a client to read a retrieval into an event that never reported one.
     */
    default List<KnowledgeQaReferenceVO> references(KnowledgeQaEvent source) {
        return switch (source.type()) {
            case STARTED, COMPLETED -> source.retrievals().stream().map(this::toReference).toList();
            case PROGRESS, FAILED -> null;
        };
    }

    /** Renders one citation, dropping the text the event contract does not publish. */
    KnowledgeQaReferenceVO toReference(KnowledgeRetrievedChunkBO source);

    /**
     * Reverse direction: the event as the domain validates it.
     *
     * <p>A reference on the wire carries no text, so the chunks it names are rebuilt with the empty
     * text the domain itself uses for a citation without its body. An event the domain would refuse —
     * a progress event without a delta — is refused here too, by the carrier's own constructor.
     */
    @Override
    @Mapping(target = "answerId", source = "answerId")
    @Mapping(target = "sequence", source = "sequence")
    @Mapping(target = "type", source = "type")
    @Mapping(target = "retrievals", expression = "java(chunks(target))")
    @Mapping(target = "delta", source = "delta")
    @Mapping(target = "answer", source = "answer")
    @Mapping(target = "errorCode", source = "code")
    @Mapping(target = "errorMessage", source = "message")
    @Mapping(target = "retryable", source = "retryable")
    @Mapping(target = "occurredAt", source = "occurredAt")
    @Mapping(target = "traceId", source = "traceId")
    KnowledgeQaEvent toSource(KnowledgeQaEventVO target);

    /** Rebuilds the chunks an event's references name, without their text. */
    default List<KnowledgeRetrievedChunkBO> chunks(KnowledgeQaEventVO source) {
        if (source.retrievals() == null) {
            return List.of();
        }
        return source.retrievals().stream()
                .map(reference -> new KnowledgeRetrievedChunkBO(reference.documentId(), reference.chunkIndex(),
                        reference.displayName(), reference.score(), ""))
                .toList();
    }
}
