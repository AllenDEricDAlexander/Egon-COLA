package top.egon.cola.archetype.source.agent.infrastructure.research.converter;

import com.google.adk.events.Event;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;
import top.egon.cola.archetype.source.agent.common.error.ResearchErrorCodeEnum;
import top.egon.cola.archetype.source.agent.domain.research.model.DeepResearchEvent;
import top.egon.cola.archetype.source.agent.domain.research.model.ResearchEventTypeEnum;
import top.egon.cola.archetype.source.agent.domain.research.model.ResearchStageEnum;
import top.egon.cola.component.common.core.converter.BaseConverter;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/** Static MapStruct boundary that projects ADK events to the safe research event allowlist. */
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface AgentFlowEventConverter extends BaseConverter<Event, DeepResearchEvent> {

    AgentFlowEventConverter INSTANCE = Mappers.getMapper(AgentFlowEventConverter.class);

    @Override
    default DeepResearchEvent toTarget(Event source) {
        if (source == null) {
            return null;
        }
        Instant occurredAt = source.timestamp() <= 0
                ? Instant.EPOCH : Instant.ofEpochMilli(source.timestamp());
        return toDomain(source, fallbackRunId(source), 1, occurredAt, "agent-flow");
    }

    /** Maps one ADK event with the run-owned identity supplied by the Infrastructure gateway. */
    default DeepResearchEvent toDomain(Event source, String runId, long sequence,
                                       Instant occurredAt, String traceId) {
        Objects.requireNonNull(source, "event must not be null");
        Objects.requireNonNull(runId, "runId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        Objects.requireNonNull(traceId, "traceId must not be null");
        if (isUnknownProgress(source)) {
            return null;
        }
        return map(source, new EventMappingContext(runId, sequence, occurredAt, traceId));
    }

    @Mapping(target = "runId", expression = "java(context.runId())")
    @Mapping(target = "sequence", expression = "java(context.sequence())")
    @Mapping(target = "type", expression = "java(eventType(source))")
    @Mapping(target = "stage", expression = "java(eventStage(source))")
    @Mapping(target = "occurredAt", expression = "java(context.occurredAt())")
    @Mapping(target = "traceId", expression = "java(context.traceId())")
    @Mapping(target = "agentName", expression = "java(agentName(source))")
    @Mapping(target = "delta", expression = "java(delta(source))")
    @Mapping(target = "reportMarkdown", expression = "java(reportMarkdown(source))")
    @Mapping(target = "errorCode", expression = "java(errorCode(source))")
    @Mapping(target = "errorMessage", expression = "java(errorMessage(source))")
    @Mapping(target = "retryable", expression = "java(retryable(source))")
    DeepResearchEvent map(Event source, @Context EventMappingContext context);

    @Override
    default Event toSource(DeepResearchEvent target) {
        if (target == null) {
            return null;
        }
        Event.Builder builder = Event.builder()
                .id(target.runId())
                .timestamp(target.occurredAt().toEpochMilli());
        if (target.agentName() != null) {
            builder.author(target.agentName());
        }
        switch (target.type()) {
            case STARTED -> builder.partial(true);
            case PROGRESS -> {
                builder.partial(true);
                if (target.delta() != null) {
                    builder.content(Content.fromParts(Part.fromText(target.delta())));
                }
            }
            case COMPLETED -> {
                if (target.reportMarkdown() != null) {
                    builder.content(Content.fromParts(Part.fromText(target.reportMarkdown())));
                }
            }
            case FAILED -> builder.errorMessage(target.errorMessage());
        }
        return builder.build();
    }

    default ResearchEventTypeEnum eventType(Event source) {
        if (isFailure(source)) {
            return ResearchEventTypeEnum.FAILED;
        }
        return source.finalResponse() ? ResearchEventTypeEnum.COMPLETED : ResearchEventTypeEnum.PROGRESS;
    }

    default ResearchStageEnum eventStage(Event source) {
        if (isFailure(source)) {
            return ResearchStageEnum.FAILED;
        }
        if (source.finalResponse()) {
            return ResearchStageEnum.COMPLETED;
        }
        return stageFor(source.author());
    }

    default String agentName(Event source) {
        if (isFailure(source) || source.finalResponse()) {
            return null;
        }
        return stageFor(source.author()) == null ? null : source.author().trim();
    }

    default String delta(Event source) {
        if (isFailure(source) || source.finalResponse()) {
            return null;
        }
        return boundedText(eventText(source), 8_192);
    }

    default String reportMarkdown(Event source) {
        return source.finalResponse() && !isFailure(source)
                ? boundedText(eventText(source), 200_000) : null;
    }

    default ResearchErrorCodeEnum errorCode(Event source) {
        return isFailure(source) ? ResearchErrorCodeEnum.RESEARCH_DEPENDENCY_UNAVAILABLE : null;
    }

    default String errorMessage(Event source) {
        return isFailure(source) ? ResearchErrorCodeEnum.RESEARCH_DEPENDENCY_UNAVAILABLE.safeMessage() : null;
    }

    default Boolean retryable(Event source) {
        return isFailure(source) ? ResearchErrorCodeEnum.RESEARCH_DEPENDENCY_UNAVAILABLE.retryable() : null;
    }

    default ResearchStageEnum stageFor(String author) {
        if (author == null) {
            return null;
        }
        return switch (author.trim()) {
            case "Planner" -> ResearchStageEnum.PLANNING;
            case "EvidenceResearcher" -> ResearchStageEnum.EVIDENCE_RESEARCH;
            case "CounterpointResearcher" -> ResearchStageEnum.COUNTERPOINT_RESEARCH;
            case "FreshnessResearcher" -> ResearchStageEnum.FRESHNESS_RESEARCH;
            case "Writer" -> ResearchStageEnum.SYNTHESIS;
            default -> null;
        };
    }

    default boolean isFailure(Event source) {
        return source.errorMessage().filter(message -> !message.isBlank()).isPresent()
                || source.interrupted().orElse(false);
    }

    default boolean isUnknownProgress(Event source) {
        return !isFailure(source) && !source.finalResponse() && stageFor(source.author()) == null;
    }

    default String eventText(Event source) {
        return source.content().stream()
                .flatMap(content -> content.parts().stream())
                .flatMap(List::stream)
                .map(Part::text)
                .flatMap(Optional::stream)
                .filter(text -> !text.isBlank())
                .collect(Collectors.joining("\n"));
    }

    default String boundedText(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
    }

    default String fallbackRunId(Event source) {
        return source.id() == null || source.id().isBlank() ? "event" : source.id();
    }

    record EventMappingContext(String runId, long sequence, Instant occurredAt, String traceId) {
    }
}
