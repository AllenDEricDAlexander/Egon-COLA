package top.egon.cola.archetype.source.agent.adapter.research.converter;

import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;
import top.egon.cola.archetype.source.agent.adapter.research.dto.StartDeepResearchRequest;
import top.egon.cola.archetype.source.agent.application.research.command.StartDeepResearchCommand;
import top.egon.cola.archetype.source.agent.domain.research.model.ReportLanguageEnum;
import top.egon.cola.component.common.core.converter.BaseConverter;

import java.util.Objects;

/** Static MapStruct mapping from the external request to the application command. */
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface DeepResearchCommandConverter extends BaseConverter<StartDeepResearchRequest, StartDeepResearchCommand> {

    DeepResearchCommandConverter INSTANCE = Mappers.getMapper(DeepResearchCommandConverter.class);

    @Override
    default StartDeepResearchCommand toTarget(StartDeepResearchRequest source) {
        return toCommand(source, "adapter");
    }

    default StartDeepResearchCommand toCommand(StartDeepResearchRequest source, String traceId) {
        Objects.requireNonNull(source, "request must not be null");
        return map(source, Objects.requireNonNull(traceId, "traceId must not be null"));
    }

    @Mapping(target = "topic", source = "topic")
    @Mapping(target = "reportLanguage", expression = "java(language(source))")
    @Mapping(target = "maxSources", expression = "java(maxSources(source))")
    @Mapping(target = "traceId", expression = "java(traceId)")
    StartDeepResearchCommand map(StartDeepResearchRequest source, @Context String traceId);

    @Override
    @Mapping(target = "topic", source = "topic")
    @Mapping(target = "reportLanguage", source = "reportLanguage")
    @Mapping(target = "maxSources", source = "maxSources")
    StartDeepResearchRequest toSource(StartDeepResearchCommand target);

    default ReportLanguageEnum language(StartDeepResearchRequest source) {
        return source.reportLanguage() == null ? ReportLanguageEnum.ZH_CN : source.reportLanguage();
    }

    default int maxSources(StartDeepResearchRequest source) {
        return source.maxSources() == null ? 8 : source.maxSources();
    }
}
