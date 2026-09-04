package top.egon.cola.archetype.source.agent.adapter.research.converter;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;
import top.egon.cola.archetype.source.agent.adapter.research.vo.DeepResearchEventVO;
import top.egon.cola.archetype.source.agent.domain.research.model.DeepResearchEvent;
import top.egon.cola.component.common.core.converter.BaseConverter;

/** Static bidirectional mapping between safe domain events and SSE view data. */
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ResearchEventConverter extends BaseConverter<DeepResearchEvent, DeepResearchEventVO> {

    ResearchEventConverter INSTANCE = Mappers.getMapper(ResearchEventConverter.class);

    @Override
    @BeanMapping(ignoreByDefault = false)
    @Mapping(target = "runId", source = "runId")
    @Mapping(target = "sequence", source = "sequence")
    @Mapping(target = "type", source = "type")
    @Mapping(target = "stage", source = "stage")
    @Mapping(target = "occurredAt", source = "occurredAt")
    @Mapping(target = "agentName", source = "agentName")
    @Mapping(target = "delta", source = "delta")
    @Mapping(target = "reportMarkdown", source = "reportMarkdown")
    @Mapping(target = "errorCode", source = "errorCode")
    @Mapping(target = "errorMessage", source = "errorMessage")
    @Mapping(target = "retryable", source = "retryable")
    @Mapping(target = "traceId", source = "traceId")
    DeepResearchEventVO toTarget(DeepResearchEvent source);

    @Override
    @BeanMapping(ignoreByDefault = false)
    @Mapping(target = "runId", source = "runId")
    @Mapping(target = "sequence", source = "sequence")
    @Mapping(target = "type", source = "type")
    @Mapping(target = "stage", source = "stage")
    @Mapping(target = "occurredAt", source = "occurredAt")
    @Mapping(target = "agentName", source = "agentName")
    @Mapping(target = "delta", source = "delta")
    @Mapping(target = "reportMarkdown", source = "reportMarkdown")
    @Mapping(target = "errorCode", source = "errorCode")
    @Mapping(target = "errorMessage", source = "errorMessage")
    @Mapping(target = "retryable", source = "retryable")
    @Mapping(target = "traceId", source = "traceId")
    DeepResearchEvent toSource(DeepResearchEventVO target);
}
