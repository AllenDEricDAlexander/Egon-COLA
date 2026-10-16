package top.egon.cola.archetype.source.agent.adapter.research.pojo.convertor;

import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;
import top.egon.cola.archetype.source.agent.adapter.handler.DeepResearchErrorResult;
import top.egon.cola.archetype.source.agent.common.exception.DeepResearchApplicationException;
import top.egon.cola.archetype.source.agent.common.error.ResearchErrorCodeEnum;
import top.egon.cola.component.common.core.converter.BaseConverter;

import java.time.Instant;
import java.util.Map;

/** Static safe mapping from typed application failures to public JSON errors. */
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface DeepResearchErrorConverter extends BaseConverter<DeepResearchApplicationException, DeepResearchErrorResult> {

    DeepResearchErrorConverter INSTANCE = Mappers.getMapper(DeepResearchErrorConverter.class);

    @Override
    default DeepResearchErrorResult toTarget(DeepResearchApplicationException source) {
        return toResponse(source, Instant.EPOCH);
    }

    default DeepResearchErrorResult toResponse(DeepResearchApplicationException source, Instant timestamp) {
        return map(source, timestamp);
    }

    @Mapping(target = "code", expression = "java(source.code().code())")
    @Mapping(target = "message", expression = "java(source.code().safeMessage())")
    @Mapping(target = "traceId", expression = "java(source.traceId())")
    @Mapping(target = "timestamp", expression = "java(timestamp)")
    @Mapping(target = "fieldErrors", expression = "java(source.fieldErrors())")
    DeepResearchErrorResult map(DeepResearchApplicationException source, @Context Instant timestamp);

    @Override
    default DeepResearchApplicationException toSource(DeepResearchErrorResult target) {
        if (target == null) {
            return null;
        }
        ResearchErrorCodeEnum code;
        try {
            code = ResearchErrorCodeEnum.valueOf(target.code());
        } catch (RuntimeException ignored) {
            code = ResearchErrorCodeEnum.RESEARCH_INTERNAL_ERROR;
        }
        return new DeepResearchApplicationException(code, target.traceId(),
                target.fieldErrors() == null ? Map.of() : target.fieldErrors(), null);
    }
}
