package top.egon.cola.archetype.source.agent.adapter.knowledge.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.archetype.source.agent.adapter.filter.ResearchTraceFilter;
import top.egon.cola.archetype.source.agent.adapter.handler.DeepResearchErrorResponse;
import top.egon.cola.archetype.source.agent.adapter.knowledge.converter.KnowledgeCommandConverter;
import top.egon.cola.archetype.source.agent.adapter.knowledge.converter.KnowledgeVoConverter;
import top.egon.cola.archetype.source.agent.adapter.knowledge.dto.CreateKnowledgeBaseRequest;
import top.egon.cola.archetype.source.agent.adapter.knowledge.dto.UpdateKnowledgeBaseRequest;
import top.egon.cola.archetype.source.agent.adapter.knowledge.vo.KnowledgeBaseVO;
import top.egon.cola.archetype.source.agent.application.knowledge.exception.KnowledgeApplicationException;
import top.egon.cola.archetype.source.agent.application.knowledge.manage.KnowledgeBaseManage;
import top.egon.cola.archetype.source.agent.common.error.KnowledgeErrorCodeEnum;
import top.egon.cola.archetype.source.agent.domain.knowledge.model.KnowledgeBaseBO;
import top.egon.cola.component.common.core.pojo.PageMetaRecord;
import top.egon.cola.component.common.core.pojo.PageResultRecord;

import java.util.List;
import java.util.Map;

/**
 * The knowledge base lifecycle over HTTP (Spec B §9.2.1 to §9.2.5).
 *
 * <p>Every method hands the request scope's correlation id to the use case and renders it back, so a
 * response can be matched to the log line that produced it. The identifier arrives as a path segment:
 * it is validated at the boundary and parsed here, which keeps an unusable identifier a validation
 * failure instead of a lookup that would report the base as missing.
 */
@Tag(name = "Knowledge Base")
@RestController("knowledgeBaseController")
@RequestMapping("/api/v1/knowledge-bases")
@RequiredArgsConstructor
@Slf4j
public class KnowledgeBaseController {

    /** The identifier pattern the contract publishes for every path variable of this resource. */
    private static final String IDENTIFIER_PATTERN = "[A-Za-z0-9._-]{1,64}";

    @Qualifier("knowledgeBaseManage")
    private final KnowledgeBaseManage knowledgeBaseManage;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(operationId = "createKnowledgeBase", summary = "Create a knowledge base",
            description = "Creates one knowledge base and freezes its embedding model and indexing configuration.")
    @SecurityRequirement(name = "researchApiKey")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The created knowledge base"),
            @ApiResponse(responseCode = "400", description = "Validation failure",
                    content = @Content(schema = @Schema(implementation = DeepResearchErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "API key failure",
                    content = @Content(schema = @Schema(implementation = DeepResearchErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "The code is taken",
                    content = @Content(schema = @Schema(implementation = DeepResearchErrorResponse.class)))
    })
    public KnowledgeBaseVO createKnowledgeBase(
            @Valid @RequestBody CreateKnowledgeBaseRequest request,
            @Parameter(in = ParameterIn.HEADER, name = ResearchTraceFilter.TRACE_HEADER,
                    description = "Optional safe request correlation id")
            @RequestAttribute(ResearchTraceFilter.TRACE_ATTRIBUTE) String traceId) {
        KnowledgeBaseBO created = knowledgeBaseManage.create(
                KnowledgeCommandConverter.INSTANCE.toCreateCommand(request, traceId));
        log.info("knowledge base create knowledgeBaseId={} outcome=CREATED", created.knowledgeBaseId());
        return render(created, traceId);
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(operationId = "listKnowledgeBases", summary = "List knowledge bases",
            description = "Returns one page of knowledge bases, newest first.")
    @SecurityRequirement(name = "researchApiKey")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "One page of knowledge bases"),
            @ApiResponse(responseCode = "400", description = "Validation failure",
                    content = @Content(schema = @Schema(implementation = DeepResearchErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "API key failure",
                    content = @Content(schema = @Schema(implementation = DeepResearchErrorResponse.class)))
    })
    public PageResultRecord<KnowledgeBaseVO> listKnowledgeBases(
            @Parameter(description = "One-based page number")
            @RequestParam(name = "page", defaultValue = "1") @Min(1) int page,
            @Parameter(description = "Page size, at most 100")
            @RequestParam(name = "size", defaultValue = "20") @Min(1) @Max(100) int size,
            @Parameter(description = "Case-insensitive match on code and name")
            @RequestParam(name = "keyword", required = false) @Size(max = 64) String keyword,
            @Parameter(description = "Exact embedding model filter")
            @RequestParam(name = "embeddingModel", required = false) @Size(max = 32) String embeddingModel,
            @Parameter(in = ParameterIn.HEADER, name = ResearchTraceFilter.TRACE_HEADER,
                    description = "Optional safe request correlation id")
            @RequestAttribute(ResearchTraceFilter.TRACE_ATTRIBUTE) String traceId) {
        PageResultRecord<KnowledgeBaseBO> found = knowledgeBaseManage.page(page, size, keyword, embeddingModel);
        List<KnowledgeBaseVO> records = found.records().stream()
                .map(base -> render(base, traceId))
                .toList();
        PageMetaRecord meta = found.page();
        return PageResultRecord.success(records, meta.total(), page, size);
    }

    @GetMapping(path = "/{knowledgeBaseId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(operationId = "getKnowledgeBase", summary = "Get a knowledge base",
            description = "Returns the full representation of one knowledge base.")
    @SecurityRequirement(name = "researchApiKey")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The knowledge base"),
            @ApiResponse(responseCode = "400", description = "Validation failure",
                    content = @Content(schema = @Schema(implementation = DeepResearchErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "API key failure",
                    content = @Content(schema = @Schema(implementation = DeepResearchErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "The knowledge base does not exist",
                    content = @Content(schema = @Schema(implementation = DeepResearchErrorResponse.class)))
    })
    public KnowledgeBaseVO getKnowledgeBase(
            @PathVariable("knowledgeBaseId")
            @Pattern(regexp = IDENTIFIER_PATTERN, message = "knowledgeBaseId must be 1 to 64 characters of [A-Za-z0-9._-]")
            String knowledgeBaseId,
            @Parameter(in = ParameterIn.HEADER, name = ResearchTraceFilter.TRACE_HEADER,
                    description = "Optional safe request correlation id")
            @RequestAttribute(ResearchTraceFilter.TRACE_ATTRIBUTE) String traceId) {
        return render(knowledgeBaseManage.get(identifier(knowledgeBaseId, traceId)), traceId);
    }

    @PutMapping(path = "/{knowledgeBaseId}", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(operationId = "updateKnowledgeBase", summary = "Update a knowledge base",
            description = "Replaces the name and description; the frozen indexing configuration is not editable.")
    @SecurityRequirement(name = "researchApiKey")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The updated knowledge base"),
            @ApiResponse(responseCode = "400", description = "Validation failure or a create-only field was sent",
                    content = @Content(schema = @Schema(implementation = DeepResearchErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "API key failure",
                    content = @Content(schema = @Schema(implementation = DeepResearchErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "The knowledge base does not exist",
                    content = @Content(schema = @Schema(implementation = DeepResearchErrorResponse.class)))
    })
    public KnowledgeBaseVO updateKnowledgeBase(
            @PathVariable("knowledgeBaseId")
            @Pattern(regexp = IDENTIFIER_PATTERN, message = "knowledgeBaseId must be 1 to 64 characters of [A-Za-z0-9._-]")
            String knowledgeBaseId,
            @Valid @RequestBody UpdateKnowledgeBaseRequest request,
            @Parameter(in = ParameterIn.HEADER, name = ResearchTraceFilter.TRACE_HEADER,
                    description = "Optional safe request correlation id")
            @RequestAttribute(ResearchTraceFilter.TRACE_ATTRIBUTE) String traceId) {
        Long identifier = identifier(knowledgeBaseId, traceId);
        KnowledgeBaseBO updated = knowledgeBaseManage.update(
                KnowledgeCommandConverter.INSTANCE.toUpdateCommand(request, identifier, traceId));
        log.info("knowledge base update knowledgeBaseId={} outcome=UPDATED", updated.knowledgeBaseId());
        return render(updated, traceId);
    }

    @DeleteMapping(path = "/{knowledgeBaseId}")
    @Operation(operationId = "deleteKnowledgeBase", summary = "Delete a knowledge base",
            description = "Removes the base with its documents, stored files and vectors; it is not recoverable.")
    @SecurityRequirement(name = "researchApiKey")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "The knowledge base is gone"),
            @ApiResponse(responseCode = "400", description = "Validation failure",
                    content = @Content(schema = @Schema(implementation = DeepResearchErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "API key failure",
                    content = @Content(schema = @Schema(implementation = DeepResearchErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "The knowledge base does not exist",
                    content = @Content(schema = @Schema(implementation = DeepResearchErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "A document of the base is still in progress",
                    content = @Content(schema = @Schema(implementation = DeepResearchErrorResponse.class)))
    })
    public ResponseEntity<Void> deleteKnowledgeBase(
            @PathVariable("knowledgeBaseId")
            @Pattern(regexp = IDENTIFIER_PATTERN, message = "knowledgeBaseId must be 1 to 64 characters of [A-Za-z0-9._-]")
            String knowledgeBaseId,
            @Parameter(in = ParameterIn.HEADER, name = ResearchTraceFilter.TRACE_HEADER,
                    description = "Optional safe request correlation id")
            @RequestAttribute(ResearchTraceFilter.TRACE_ATTRIBUTE) String traceId) {
        knowledgeBaseManage.delete(identifier(knowledgeBaseId, traceId));
        return ResponseEntity.noContent().build();
    }

    /** Renders one base with the document count the response contract carries beside it. */
    private KnowledgeBaseVO render(KnowledgeBaseBO base, String traceId) {
        long documentCount = knowledgeBaseManage.documentCount(base.knowledgeBaseId());
        return KnowledgeVoConverter.INSTANCE.toVO(base, documentCount, traceId);
    }

    /**
     * Reads the path identifier.
     *
     * <p>The published pattern admits an opaque token, while this version stores a numeric identifier:
     * a token that cannot denote one is a validation failure, not a lookup that would answer "not
     * found" for a request that never named a base.
     */
    private static Long identifier(String knowledgeBaseId, String traceId) {
        try {
            long parsed = Long.parseLong(knowledgeBaseId);
            if (parsed <= 0) {
                throw new NumberFormatException("identifier must be positive");
            }
            return parsed;
        } catch (NumberFormatException invalid) {
            throw new KnowledgeApplicationException(KnowledgeErrorCodeEnum.KNOWLEDGE_VALIDATION_ERROR, traceId,
                    Map.of("knowledgeBaseId", List.of("knowledgeBaseId must be a positive number")), invalid);
        }
    }
}
