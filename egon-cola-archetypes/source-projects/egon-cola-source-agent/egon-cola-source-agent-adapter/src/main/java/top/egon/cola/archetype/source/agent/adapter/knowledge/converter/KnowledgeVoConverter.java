package top.egon.cola.archetype.source.agent.adapter.knowledge.converter;

import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;
import top.egon.cola.archetype.source.agent.adapter.knowledge.vo.KnowledgeBaseVO;
import top.egon.cola.archetype.source.agent.domain.knowledge.model.ChunkingStrategyEnum;
import top.egon.cola.archetype.source.agent.domain.knowledge.model.KnowledgeBaseBO;
import top.egon.cola.archetype.source.agent.domain.knowledge.model.KnowledgeBaseStatusEnum;
import top.egon.cola.archetype.source.agent.domain.knowledge.model.KnowledgeChunkConfigBO;
import top.egon.cola.component.common.core.converter.BaseConverter;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Mapping from the knowledge base domain carrier to its published representation (Spec B §10.4).
 *
 * <p>The document count and the trace id are request-scoped facts the domain carrier does not hold,
 * so they arrive as context. The frozen chunking configuration is rendered per strategy: a heading
 * strategy shows its heading levels, and any other strategy omits them, because the contract only
 * ever accepts them for a heading strategy (§9.2.1).
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface KnowledgeVoConverter extends BaseConverter<KnowledgeBaseBO, KnowledgeBaseVO> {

    KnowledgeVoConverter INSTANCE = Mappers.getMapper(KnowledgeVoConverter.class);

    /**
     * Tenant used when a representation is mapped back: the interface carries no tenant by contract
     * (§9.2, `REQ-013`), and this version stores the default tenant, so the value is not a guess.
     */
    long WIRE_TENANT_ID = 0L;

    @Override
    default KnowledgeBaseVO toTarget(KnowledgeBaseBO source) {
        return toVO(source, 0L, null);
    }

    /** Renders one base with the count and correlation id of the request that asked for it. */
    default KnowledgeBaseVO toVO(KnowledgeBaseBO source, long documentCount, String traceId) {
        return map(source, documentCount, traceId);
    }

    @Mapping(target = "knowledgeBaseId", source = "knowledgeBaseId")
    @Mapping(target = "code", source = "code")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "embeddingModel", source = "embeddingModel")
    @Mapping(target = "chunkStrategy", source = "chunkStrategy")
    @Mapping(target = "chunkConfig", expression = "java(toChunkConfig(source))")
    @Mapping(target = "documentCount", expression = "java(documentCount)")
    @Mapping(target = "status", expression = "java(toStatus(source))")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedAt", source = "updatedAt")
    @Mapping(target = "traceId", expression = "java(traceId)")
    KnowledgeBaseVO map(KnowledgeBaseBO source, @Context long documentCount, @Context String traceId);

    /**
     * Reverse direction, for the round-trip contract of a converter.
     *
     * <p>A rendered base does not carry its tenant, so the stored default is used rather than written
     * back as {@code null}: this version persists the default tenant for every base, which is exactly
     * what the value states.
     */
    @Override
    default KnowledgeBaseBO toSource(KnowledgeBaseVO target) {
        if (target == null) {
            return null;
        }
        return new KnowledgeBaseBO(target.knowledgeBaseId(), WIRE_TENANT_ID, target.code(), target.name(),
                target.description(), target.embeddingModel(), target.chunkStrategy(),
                fromChunkConfig(target.chunkConfig()),
                target.status() == null ? null : KnowledgeBaseStatusEnum.valueOf(target.status()),
                target.createdAt(), target.updatedAt());
    }

    /** Renders the frozen configuration, keeping the heading levels a heading strategy owns. */
    default Map<String, Object> toChunkConfig(KnowledgeBaseBO source) {
        KnowledgeChunkConfigBO config = source.chunkConfig();
        if (config == null) {
            return null;
        }
        Map<String, Object> rendered = new LinkedHashMap<>(4);
        rendered.put("maxTokensPerChunk", config.maxTokensPerChunk());
        rendered.put("overlapTokens", config.overlapTokens());
        rendered.put("minChunkChars", config.minChunkChars());
        if (source.chunkStrategy() == ChunkingStrategyEnum.MARKDOWN_HEADING) {
            rendered.put("headingLevels", List.copyOf(config.headingLevels()));
        }
        return Collections.unmodifiableMap(rendered);
    }

    /** Reads the rendered configuration back into its carrier, outside a heading strategy. */
    default KnowledgeChunkConfigBO fromChunkConfig(Map<String, Object> rendered) {
        if (rendered == null) {
            return null;
        }
        return new KnowledgeChunkConfigBO(number(rendered.get("maxTokensPerChunk"), 0),
                number(rendered.get("overlapTokens"), 0), number(rendered.get("minChunkChars"), 1),
                headingLevels(rendered.get("headingLevels")));
    }

    default String toStatus(KnowledgeBaseBO source) {
        return source.status() == null ? null : source.status().name();
    }

    private static int number(Object value, int fallback) {
        return value instanceof Number counted ? counted.intValue() : fallback;
    }

    private static List<Integer> headingLevels(Object value) {
        if (!(value instanceof List<?> levels)) {
            return List.of();
        }
        return levels.stream().map(level -> number(level, 1)).toList();
    }
}
