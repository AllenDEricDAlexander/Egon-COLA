package top.egon.cola.archetype.source.agent.adapter.research.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import top.egon.cola.archetype.source.agent.adapter.filter.ResearchTraceFilter;
import top.egon.cola.archetype.source.agent.adapter.handler.DeepResearchErrorResponse;
import top.egon.cola.archetype.source.agent.adapter.research.converter.DeepResearchCommandConverter;
import top.egon.cola.archetype.source.agent.adapter.research.converter.ResearchEventConverter;
import top.egon.cola.archetype.source.agent.adapter.research.dto.StartDeepResearchRequest;
import top.egon.cola.archetype.source.agent.adapter.research.vo.DeepResearchEventVO;
import top.egon.cola.archetype.source.agent.application.research.command.StartDeepResearchCommand;
import top.egon.cola.archetype.source.agent.application.research.config.DeepResearchRuntimeProperties;
import top.egon.cola.archetype.source.agent.application.research.manage.DeepResearchManage;
import top.egon.cola.archetype.source.agent.domain.research.model.DeepResearchEvent;
import top.egon.cola.archetype.source.agent.common.error.ResearchErrorCodeEnum;
import top.egon.cola.archetype.source.agent.domain.research.service.DeepResearchEventObserverService;
import top.egon.cola.archetype.source.agent.domain.research.service.DeepResearchRunService;

import java.io.IOException;
import java.time.Clock;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/** Single REST Command endpoint that bridges domain Observer events to SSE. */
@RestController("deepResearchController")
@RequestMapping("/api/v1/deep-research/runs")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Deep Research")
public class DeepResearchController {

    @Qualifier("deepResearchManage")
    private final DeepResearchManage manage;

    @Qualifier("deepResearchRuntimeProperties")
    private final DeepResearchRuntimeProperties runtimeProperties;

    @Qualifier("agentClock")
    private final Clock clock;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Start Deep Research", operationId = "startDeepResearch",
            description = "Starts one bounded, non-idempotent source-grounded research run.")
    @SecurityRequirement(name = "researchApiKey")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "SSE research lifecycle stream",
                    content = @Content(mediaType = MediaType.TEXT_EVENT_STREAM_VALUE,
                            schema = @Schema(implementation = DeepResearchEventVO.class))),
            @ApiResponse(responseCode = "400", description = "Validation failure",
                    content = @Content(schema = @Schema(implementation = DeepResearchErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "API key failure",
                    content = @Content(schema = @Schema(implementation = DeepResearchErrorResponse.class))),
            @ApiResponse(responseCode = "406", description = "SSE is not accepted",
                    content = @Content(schema = @Schema(implementation = DeepResearchErrorResponse.class))),
            @ApiResponse(responseCode = "415", description = "JSON is not supplied",
                    content = @Content(schema = @Schema(implementation = DeepResearchErrorResponse.class))),
            @ApiResponse(responseCode = "429", description = "Research capacity is exhausted",
                    content = @Content(schema = @Schema(implementation = DeepResearchErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal failure",
                    content = @Content(schema = @Schema(implementation = DeepResearchErrorResponse.class))),
            @ApiResponse(responseCode = "503", description = "Research dependency is unavailable",
                    content = @Content(schema = @Schema(implementation = DeepResearchErrorResponse.class)))
    })
    public SseEmitter startDeepResearch(
            @Valid @RequestBody StartDeepResearchRequest request,
            @Parameter(in = ParameterIn.HEADER, name = ResearchTraceFilter.TRACE_HEADER,
                    description = "Optional safe request correlation id")
            @RequestAttribute(ResearchTraceFilter.TRACE_ATTRIBUTE) String traceId,
            HttpServletResponse response) {
        SseEmitter emitter = new SseEmitter(runtimeProperties.maxDuration().toMillis());
        AtomicBoolean terminal = new AtomicBoolean();
        AtomicLong lastSequence = new AtomicLong();
        AtomicReference<DeepResearchRunService> runReference = new AtomicReference<>();
        AtomicReference<String> runIdReference = new AtomicReference<>();

        DeepResearchEventObserverService observer = event -> publish(
                event, emitter, terminal, lastSequence, runReference, runIdReference);
        emitter.onCompletion(() -> cancel(runReference, terminal));
        emitter.onTimeout(() -> {
            if (terminal.compareAndSet(false, true)) {
                String runId = runIdReference.get();
                if (runId != null) {
                    sendTerminal(emitter, DeepResearchEvent.failed(runId, lastSequence.incrementAndGet(),
                            ResearchErrorCodeEnum.RESEARCH_TIMEOUT, clock.instant(), traceId));
                }
                DeepResearchRunService run = runReference.get();
                if (run != null) {
                    run.cancel();
                }
            }
            emitter.complete();
        });
        emitter.onError(error -> cancel(runReference, terminal));

        StartDeepResearchCommand command = DeepResearchCommandConverter.INSTANCE.toCommand(request, traceId);
        DeepResearchRunService run = manage.startResearch(command, observer);
        runReference.set(run);
        runIdReference.set(run.runId());
        if (terminal.get()) {
            run.cancel();
        }
        response.setHeader(ResearchTraceFilter.TRACE_HEADER, traceId);
        return emitter;
    }

    private void publish(DeepResearchEvent event, SseEmitter emitter, AtomicBoolean terminal,
                         AtomicLong lastSequence,
                         AtomicReference<DeepResearchRunService> runReference,
                         AtomicReference<String> runIdReference) {
        if (event == null || terminal.get()) {
            return;
        }
        runIdReference.compareAndSet(null, event.runId());
        lastSequence.accumulateAndGet(event.sequence(), Math::max);
        if (event.isTerminal() && !terminal.compareAndSet(false, true)) {
            return;
        }
        try {
            DeepResearchEventVO view = ResearchEventConverter.INSTANCE.toTarget(event);
            emitter.send(SseEmitter.event()
                    .id(event.runId() + ":" + event.sequence())
                    .name(eventName(event))
                    .data(view, MediaType.APPLICATION_JSON));
            if (event.isTerminal()) {
                emitter.complete();
            }
        } catch (IOException | IllegalStateException failure) {
            DeepResearchRunService run = runReference.get();
            if (run != null) {
                run.cancel();
            }
            terminal.set(true);
            throw new IllegalStateException("SSE event could not be sent", failure);
        }
    }

    private void sendTerminal(SseEmitter emitter, DeepResearchEvent event) {
        try {
            emitter.send(SseEmitter.event()
                    .id(event.runId() + ":" + event.sequence())
                    .name("research.failed")
                    .data(ResearchEventConverter.INSTANCE.toTarget(event), MediaType.APPLICATION_JSON));
        } catch (IOException | IllegalStateException failure) {
        }
    }

    private static void cancel(AtomicReference<DeepResearchRunService> runReference, AtomicBoolean terminal) {
        DeepResearchRunService run = runReference.get();
        if (run != null && terminal.compareAndSet(false, true)) {
            run.cancel();
        }
    }

    private static String eventName(DeepResearchEvent event) {
        return switch (event.type()) {
            case STARTED -> "research.started";
            case PROGRESS -> "research.progress";
            case COMPLETED -> "research.completed";
            case FAILED -> "research.failed";
        };
    }
}
