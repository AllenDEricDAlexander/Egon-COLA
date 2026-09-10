package top.egon.cola.archetype.source.agent.adapter.knowledge.controller;

import io.swagger.v3.oas.annotations.Hidden;
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
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import top.egon.cola.archetype.source.agent.adapter.filter.ResearchTraceFilter;
import top.egon.cola.archetype.source.agent.adapter.handler.DeepResearchErrorResponse;
import top.egon.cola.archetype.source.agent.adapter.knowledge.converter.KnowledgeEventConverter;
import top.egon.cola.archetype.source.agent.adapter.knowledge.converter.KnowledgeQaCommandConverter;
import top.egon.cola.archetype.source.agent.adapter.knowledge.converter.KnowledgeRetrievalVoConverter;
import top.egon.cola.archetype.source.agent.adapter.knowledge.dto.AskKnowledgeBaseRequest;
import top.egon.cola.archetype.source.agent.adapter.knowledge.dto.RetrieveKnowledgeRequest;
import top.egon.cola.archetype.source.agent.adapter.knowledge.vo.KnowledgeQaEventVO;
import top.egon.cola.archetype.source.agent.adapter.knowledge.vo.KnowledgeRetrievalVO;
import top.egon.cola.archetype.source.agent.application.knowledge.command.AskKnowledgeBaseCommand;
import top.egon.cola.archetype.source.agent.application.knowledge.command.RetrieveKnowledgeCommand;
import top.egon.cola.archetype.source.agent.application.knowledge.config.KnowledgeRuntimeProperties;
import top.egon.cola.archetype.source.agent.application.knowledge.exception.KnowledgeApplicationException;
import top.egon.cola.archetype.source.agent.application.knowledge.manage.KnowledgeQaManage;
import top.egon.cola.archetype.source.agent.common.error.KnowledgeErrorCodeEnum;
import top.egon.cola.archetype.source.agent.domain.knowledge.model.KnowledgeQaEvent;
import top.egon.cola.archetype.source.agent.domain.knowledge.model.KnowledgeRetrievalBO;
import top.egon.cola.archetype.source.agent.domain.knowledge.service.KnowledgeAnswerRunService;
import top.egon.cola.archetype.source.agent.domain.knowledge.service.KnowledgeQaEventObserverService;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Retrieval debugging and the retrieval-backed answer stream of one knowledge base
 * (Spec B §9.2.11, §9.2.12).
 *
 * <p>Both endpoints hand the use case an intent and render what comes back; neither derives a
 * collection, a model or a filter, because that is what keeps a request from choosing whose vectors
 * it reads.
 *
 * <p>The answer endpoint streams. Its failures split in two: everything decided before an event is
 * sent — a missing base, an exhausted capacity — is raised as an application failure and answered
 * with the ordinary error body, while everything after the first event can only be reported inside
 * the stream, as the terminal {@code knowledge.failed}. A stream that ends without a terminal event
 * states that the answer's outcome is unknown, which is what a cancelled or timed-out generation
 * leaves behind.
 */
@Tag(name = "Knowledge QA")
@RestController("knowledgeQaController")
@RequiredArgsConstructor
@Slf4j
public class KnowledgeQaController {

    /** The identifier pattern the contract publishes for every path variable of this resource. */
    private static final String IDENTIFIER_PATTERN = "[A-Za-z0-9._-]{1,64}";

    @Qualifier("knowledgeQaManage")
    private final KnowledgeQaManage knowledgeQaManage;

    @Qualifier("knowledgeRuntimeProperties")
    private final KnowledgeRuntimeProperties properties;

    @PostMapping(path = "/api/v1/knowledge-bases/{knowledgeBaseId}/retrieve",
            consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(operationId = "retrieveKnowledgeChunks", summary = "Retrieve chunks from a base",
            description = "Embeds the query and returns the scored chunks of this base. No chat model "
                    + "is called: the endpoint costs one embedding and answers with the chunks a "
                    + "question would have been answered from.")
    @SecurityRequirement(name = "researchApiKey")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The matched chunks, possibly none"),
            @ApiResponse(responseCode = "400", description = "Validation failure",
                    content = @Content(schema = @Schema(implementation = DeepResearchErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "API key failure",
                    content = @Content(schema = @Schema(implementation = DeepResearchErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "The knowledge base does not exist",
                    content = @Content(schema = @Schema(implementation = DeepResearchErrorResponse.class))),
            @ApiResponse(responseCode = "503", description = "The vector store or the embedding endpoint is unavailable",
                    content = @Content(schema = @Schema(implementation = DeepResearchErrorResponse.class)))
    })
    public KnowledgeRetrievalVO retrieveKnowledgeChunks(
            @PathVariable("knowledgeBaseId")
            @Pattern(regexp = IDENTIFIER_PATTERN, message = "knowledgeBaseId must be 1 to 64 characters of [A-Za-z0-9._-]")
            String knowledgeBaseId,
            @Valid @RequestBody RetrieveKnowledgeRequest request,
            @Parameter(in = ParameterIn.HEADER, name = ResearchTraceFilter.TRACE_HEADER,
                    description = "Optional safe request correlation id")
            @RequestAttribute(ResearchTraceFilter.TRACE_ATTRIBUTE) String traceId) {
        RetrieveKnowledgeCommand command = KnowledgeQaCommandConverter.INSTANCE.toRetrieveCommand(
                request, identifier(knowledgeBaseId, "knowledgeBaseId", traceId), traceId);
        KnowledgeRetrievalBO retrieval = knowledgeQaManage.retrieve(command);
        return KnowledgeRetrievalVoConverter.INSTANCE.toVO(retrieval, command.query(), traceId);
    }

    @PostMapping(path = "/api/v1/knowledge-bases/{knowledgeBaseId}/chat",
            consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(operationId = "chatWithKnowledgeBase", summary = "Ask a base a question",
            description = "Retrieves the supporting chunks and streams one answer as server-sent "
                    + "events. The stream opens with knowledge.started, continues with "
                    + "knowledge.progress increments and ends with knowledge.completed or "
                    + "knowledge.failed; the terminal event is the last one.")
    @SecurityRequirement(name = "researchApiKey")
    @Parameter(in = ParameterIn.HEADER, name = HttpHeaders.ACCEPT, required = true,
            description = "Must declare text/event-stream; any other value is answered 406",
            schema = @Schema(type = "string", defaultValue = MediaType.TEXT_EVENT_STREAM_VALUE,
                    allowableValues = {MediaType.TEXT_EVENT_STREAM_VALUE}))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The answer stream",
                    content = @Content(mediaType = MediaType.TEXT_EVENT_STREAM_VALUE,
                            schema = @Schema(implementation = KnowledgeQaEventVO.class))),
            @ApiResponse(responseCode = "400", description = "Validation failure",
                    content = @Content(schema = @Schema(implementation = DeepResearchErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "API key failure",
                    content = @Content(schema = @Schema(implementation = DeepResearchErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "The knowledge base does not exist",
                    content = @Content(schema = @Schema(implementation = DeepResearchErrorResponse.class))),
            @ApiResponse(responseCode = "406", description = "The client cannot accept an event stream",
                    content = @Content(schema = @Schema(implementation = DeepResearchErrorResponse.class))),
            @ApiResponse(responseCode = "429", description = "Every answer slot is in use",
                    content = @Content(schema = @Schema(implementation = DeepResearchErrorResponse.class))),
            @ApiResponse(responseCode = "503", description = "The vector store or the embedding endpoint is unavailable",
                    content = @Content(schema = @Schema(implementation = DeepResearchErrorResponse.class)))
    })
    public SseEmitter chatWithKnowledgeBase(
            @PathVariable("knowledgeBaseId")
            @Pattern(regexp = IDENTIFIER_PATTERN, message = "knowledgeBaseId must be 1 to 64 characters of [A-Za-z0-9._-]")
            String knowledgeBaseId,
            @Valid @RequestBody AskKnowledgeBaseRequest request,
            @Parameter(in = ParameterIn.HEADER, name = ResearchTraceFilter.TRACE_HEADER,
                    description = "Optional safe request correlation id")
            @RequestAttribute(ResearchTraceFilter.TRACE_ATTRIBUTE) String traceId,
            HttpServletResponse response) {
        AskKnowledgeBaseCommand command = KnowledgeQaCommandConverter.INSTANCE.toAskCommand(
                request, identifier(knowledgeBaseId, "knowledgeBaseId", traceId), traceId);
        SseEmitter emitter = new SseEmitter(properties.runtime().qaMaxDuration().toMillis());
        AtomicBoolean terminal = new AtomicBoolean();
        AtomicReference<KnowledgeAnswerRunService> runReference = new AtomicReference<>();
        KnowledgeQaEventObserverService observer = event -> publish(event, emitter, terminal, runReference);
        emitter.onCompletion(() -> cancel(runReference, terminal));
        emitter.onTimeout(() -> {
            cancel(runReference, terminal);
            emitter.complete();
        });
        emitter.onError(error -> cancel(runReference, terminal));

        KnowledgeAnswerRunService run = knowledgeQaManage.ask(command, observer);
        runReference.set(run);
        if (terminal.get()) {
            run.cancel();
        }
        log.info("knowledge qa knowledgeBaseId={} topK={} outcome=STREAM_ESTABLISHED",
                command.knowledgeBaseId(), command.topK());
        response.setHeader(ResearchTraceFilter.TRACE_HEADER, traceId);
        return emitter;
    }

    /**
     * The mapping that answers a caller whose {@code Accept} cannot take an event stream.
     *
     * <p>Media negotiation fails while the handler is looked up, which is before any of this
     * controller's methods runs — and only a package-scoped advice knows the knowledge vocabulary, but
     * such an advice is not selectable while no handler is known. Declaring a second mapping on the
     * same path for a caller that has not declared the stream moves that failure inside a handler: a
     * caller that did declare it never reaches this mapping, and every other one can only be refused,
     * so the refusal is answered with the published code and status instead of the research domain's.
     *
     * <p>The condition is what the contract asks of a caller — {@code Accept} must admit an event
     * stream — so a request that names no accepted type at all is refused like one that names a type
     * it cannot have; the contract leaves that case open and this is the value it is given here.
     *
     * <p>The check precedes the base lookup, which is the one place this endpoint does not follow the
     * contract's validation order: a representation refusal is decided about the request before the
     * resource behind it, and no acceptable body could carry the not-found answer either.
     *
     * <p>It is hidden from the API document: the operation a caller is meant to call is the streaming
     * one, and this mapping is a negotiation detail of it rather than a second way to ask.
     */
    @Hidden
    @PostMapping(path = "/api/v1/knowledge-bases/{knowledgeBaseId}/chat",
            consumes = MediaType.APPLICATION_JSON_VALUE, headers = "Accept!=text/event-stream")
    public ResponseEntity<Void> rejectUnacceptableAccept(
            @RequestAttribute(ResearchTraceFilter.TRACE_ATTRIBUTE) String traceId) {
        throw new KnowledgeApplicationException(KnowledgeErrorCodeEnum.KNOWLEDGE_NOT_ACCEPTABLE, traceId);
    }

    /**
     * Sends one event and ends the stream on the terminal one.
     *
     * <p>A failure to send is not reported inside the stream — the stream is what failed — so the
     * generation is cancelled and the failure is left to propagate, where the use case's own
     * bookkeeping releases the capacity permit.
     */
    private static void publish(KnowledgeQaEvent event, SseEmitter emitter, AtomicBoolean terminal,
                                AtomicReference<KnowledgeAnswerRunService> runReference) {
        if (event == null || terminal.get()) {
            return;
        }
        if (event.isTerminal() && !terminal.compareAndSet(false, true)) {
            return;
        }
        try {
            KnowledgeQaEventVO payload = KnowledgeEventConverter.INSTANCE.toTarget(event);
            emitter.send(SseEmitter.event()
                    .id(event.answerId() + ":" + event.sequence())
                    .name(eventName(event))
                    .data(payload, MediaType.APPLICATION_JSON));
            if (event.isTerminal()) {
                emitter.complete();
            }
        } catch (IOException | IllegalStateException failure) {
            cancel(runReference, terminal);
            throw new IllegalStateException("knowledge answer event could not be sent", failure);
        }
    }

    /** Cancels the generation behind a stream that is gone, at most once per stream. */
    private static void cancel(AtomicReference<KnowledgeAnswerRunService> runReference, AtomicBoolean terminal) {
        KnowledgeAnswerRunService run = runReference.get();
        if (run != null && terminal.compareAndSet(false, true)) {
            run.cancel();
        }
    }

    private static String eventName(KnowledgeQaEvent event) {
        return switch (event.type()) {
            case STARTED -> "knowledge.started";
            case PROGRESS -> "knowledge.progress";
            case COMPLETED -> "knowledge.completed";
            case FAILED -> "knowledge.failed";
        };
    }

    /**
     * Reads a path identifier.
     *
     * <p>The published pattern admits an opaque token, while this version stores a numeric identifier:
     * a token that cannot denote one is a validation failure, not a lookup that would answer "not
     * found" for a request that never named a row.
     */
    private static Long identifier(String identifier, String field, String traceId) {
        try {
            long parsed = Long.parseLong(identifier);
            if (parsed <= 0) {
                throw new NumberFormatException("identifier must be positive");
            }
            return parsed;
        } catch (NumberFormatException invalid) {
            throw new KnowledgeApplicationException(KnowledgeErrorCodeEnum.KNOWLEDGE_VALIDATION_ERROR, traceId,
                    Map.of(field, List.of(field + " must be a positive number")), invalid);
        }
    }
}
