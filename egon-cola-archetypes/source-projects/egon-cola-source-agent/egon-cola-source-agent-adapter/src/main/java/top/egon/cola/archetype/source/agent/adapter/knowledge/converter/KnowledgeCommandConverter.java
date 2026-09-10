package top.egon.cola.archetype.source.agent.adapter.knowledge.converter;

import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;
import top.egon.cola.archetype.source.agent.adapter.knowledge.dto.CreateKnowledgeBaseRequest;
import top.egon.cola.archetype.source.agent.adapter.knowledge.dto.UpdateKnowledgeBaseRequest;
import top.egon.cola.archetype.source.agent.application.knowledge.command.CreateKnowledgeBaseCommand;
import top.egon.cola.archetype.source.agent.application.knowledge.command.UpdateKnowledgeBaseCommand;
import top.egon.cola.component.common.core.converter.BaseConverter;

import java.util.Objects;

/**
 * Mapping between the published knowledge-base bodies and their application commands (Spec B §10.4).
 *
 * <p>Both create and edit live in this interface because they share one vocabulary: the edit request
 * carries the create-only fields as probes, so the two directions are written side by side and cannot
 * drift apart. The trace id belongs to the request scope, not to the body, so it arrives as context
 * instead of being a mapped field — a caller cannot forge a correlation id through the payload.
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface KnowledgeCommandConverter extends BaseConverter<CreateKnowledgeBaseRequest, CreateKnowledgeBaseCommand> {

    KnowledgeCommandConverter INSTANCE = Mappers.getMapper(KnowledgeCommandConverter.class);

    /** Correlation id for the no-context entry point of {@link BaseConverter}. */
    String DEFAULT_TRACE_ID = "adapter";

    @Override
    default CreateKnowledgeBaseCommand toTarget(CreateKnowledgeBaseRequest source) {
        return toCreateCommand(source, DEFAULT_TRACE_ID);
    }

    /** Maps one create body to its command, stamping the request-scoped correlation id. */
    default CreateKnowledgeBaseCommand toCreateCommand(CreateKnowledgeBaseRequest source, String traceId) {
        Objects.requireNonNull(source, "request must not be null");
        return mapCreate(source, Objects.requireNonNull(traceId, "traceId must not be null"));
    }

    @Mapping(target = "code", source = "code")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "embeddingModel", source = "embeddingModel")
    @Mapping(target = "chunkStrategy", source = "chunkStrategy")
    @Mapping(target = "chunkConfig", expression = "java(toChunkingConfig(source.chunkConfig()))")
    @Mapping(target = "traceId", expression = "java(traceId)")
    CreateKnowledgeBaseCommand mapCreate(CreateKnowledgeBaseRequest source, @Context String traceId);

    @Override
    @Mapping(target = "code", source = "code")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "embeddingModel", source = "embeddingModel")
    @Mapping(target = "chunkStrategy", source = "chunkStrategy")
    @Mapping(target = "chunkConfig", expression = "java(toChunkingConfigRequest(target.chunkConfig()))")
    CreateKnowledgeBaseRequest toSource(CreateKnowledgeBaseCommand target);

    /** Maps one edit body to its command, carrying the path identifier and the traced context. */
    default UpdateKnowledgeBaseCommand toUpdateCommand(UpdateKnowledgeBaseRequest source, Long knowledgeBaseId,
                                                      String traceId) {
        Objects.requireNonNull(source, "request must not be null");
        return mapUpdate(source, Objects.requireNonNull(knowledgeBaseId, "knowledgeBaseId must not be null"),
                Objects.requireNonNull(traceId, "traceId must not be null"));
    }

    @Mapping(target = "knowledgeBaseId", expression = "java(knowledgeBaseId)")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "code", source = "code")
    @Mapping(target = "embeddingModel", source = "embeddingModel")
    @Mapping(target = "chunkStrategy", source = "chunkStrategy")
    @Mapping(target = "chunkConfig", expression = "java(toChunkingConfig(source.chunkConfig()))")
    @Mapping(target = "traceId", expression = "java(traceId)")
    UpdateKnowledgeBaseCommand mapUpdate(UpdateKnowledgeBaseRequest source, @Context Long knowledgeBaseId,
                                         @Context String traceId);

    /** Reverse of the edit direction: the probes come back as the body that would send them again. */
    default UpdateKnowledgeBaseRequest toUpdateRequest(UpdateKnowledgeBaseCommand target) {
        if (target == null) {
            return null;
        }
        return new UpdateKnowledgeBaseRequest(target.name(), target.description(), target.code(),
                target.embeddingModel(), target.chunkStrategy(), toChunkingConfigRequest(target.chunkConfig()));
    }

    /**
     * Fills in the primitive command fields an omitted optional body field would leave undefined.
     *
     * <p>The request defaults the optional fields before conversion, so only {@code maxTokensPerChunk}
     * can still be missing — and it is the one field the request marks required, so the fallback only
     * ever serves a caller that bypassed the boundary. It stays inside the documented range so the
     * domain refuses it as a validation failure instead of failing on an undefined value.
     */
    default CreateKnowledgeBaseCommand.ChunkingConfigCommand toChunkingConfig(
            CreateKnowledgeBaseRequest.ChunkingConfigRequest config) {
        if (config == null) {
            return null;
        }
        return new CreateKnowledgeBaseCommand.ChunkingConfigCommand(
                config.maxTokensPerChunk() == null ? 0 : config.maxTokensPerChunk(),
                config.overlapTokens(), config.minChunkChars(), config.headingLevels());
    }

    default CreateKnowledgeBaseRequest.ChunkingConfigRequest toChunkingConfigRequest(
            CreateKnowledgeBaseCommand.ChunkingConfigCommand config) {
        if (config == null) {
            return null;
        }
        return new CreateKnowledgeBaseRequest.ChunkingConfigRequest(config.maxTokensPerChunk(),
                config.overlapTokens(), config.minChunkChars(), config.headingLevels());
    }
}
