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
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import top.egon.cola.archetype.source.agent.adapter.filter.ResearchTraceFilter;
import top.egon.cola.archetype.source.agent.adapter.handler.DeepResearchErrorResponse;
import top.egon.cola.archetype.source.agent.adapter.knowledge.converter.KnowledgeDocumentVoConverter;
import top.egon.cola.archetype.source.agent.adapter.knowledge.vo.KnowledgeDocumentVO;
import top.egon.cola.archetype.source.agent.application.knowledge.command.UploadKnowledgeDocumentCommand;
import top.egon.cola.archetype.source.agent.application.knowledge.exception.KnowledgeApplicationException;
import top.egon.cola.archetype.source.agent.application.knowledge.manage.KnowledgeDocumentManage;
import top.egon.cola.archetype.source.agent.common.error.KnowledgeErrorCodeEnum;
import top.egon.cola.archetype.source.agent.domain.knowledge.model.DocumentIngestStatusEnum;
import top.egon.cola.archetype.source.agent.domain.knowledge.model.KnowledgeDocumentBO;
import top.egon.cola.component.common.core.pojo.PageMetaRecord;
import top.egon.cola.component.common.core.pojo.PageResultRecord;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * The document lifecycle of one knowledge base over HTTP (Spec B §9.2.6 to §9.2.10).
 *
 * <p>The upload is the only endpoint that reads a body it cannot validate field by field: a multipart
 * request is bound by hand here, so a missing, empty or unnamed part is answered with the same
 * validation body as any other rejected field instead of a container error. The bytes are handed to
 * the use case in memory, which is where the size limit and the extractor routing live — the boundary
 * does not own either rule.
 *
 * <p>The file part is bound with {@code @RequestPart}; the display name is a form field and arrives,
 * like any other, as a request parameter — Spring resolves a non-file part through neither annotation,
 * so binding it as a part would silently drop it.
 */
@Tag(name = "Knowledge Document")
@RestController("knowledgeDocumentController")
@RequiredArgsConstructor
@Slf4j
public class KnowledgeDocumentController {

    /** The identifier pattern the contract publishes for every path variable of this resource. */
    private static final String IDENTIFIER_PATTERN = "[A-Za-z0-9._-]{1,64}";

    private static final String FILE_PART = "file";

    private static final String DISPLAY_NAME_PART = "displayName";

    private static final int MAX_DISPLAY_NAME_LENGTH = 255;

    @Qualifier("knowledgeDocumentManage")
    private final KnowledgeDocumentManage knowledgeDocumentManage;

    @PostMapping(path = "/api/v1/knowledge-bases/{knowledgeBaseId}/documents",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(operationId = "uploadKnowledgeDocument", summary = "Upload a document",
            description = "Stores the original, extracts its text and queues the ingest; the status "
                    + "starts at PENDING and is polled through getKnowledgeDocument.")
    @SecurityRequirement(name = "researchApiKey")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "The document is stored and queued"),
            @ApiResponse(responseCode = "400", description = "Validation failure or no extractor for the format",
                    content = @Content(schema = @Schema(implementation = DeepResearchErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "API key failure",
                    content = @Content(schema = @Schema(implementation = DeepResearchErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "The knowledge base does not exist",
                    content = @Content(schema = @Schema(implementation = DeepResearchErrorResponse.class))),
            @ApiResponse(responseCode = "413", description = "The file exceeds the size limit",
                    content = @Content(schema = @Schema(implementation = DeepResearchErrorResponse.class))),
            @ApiResponse(responseCode = "429", description = "The base holds as many documents as it may",
                    content = @Content(schema = @Schema(implementation = DeepResearchErrorResponse.class)))
    })
    public ResponseEntity<KnowledgeDocumentVO> uploadKnowledgeDocument(
            @PathVariable("knowledgeBaseId")
            @Pattern(regexp = IDENTIFIER_PATTERN, message = "knowledgeBaseId must be 1 to 64 characters of [A-Za-z0-9._-]")
            String knowledgeBaseId,
            @Parameter(description = "The file to process; the format must have a registered extractor")
            @RequestPart(value = FILE_PART, required = false) MultipartFile file,
            @Parameter(description = "Display name, defaulting to the original file name")
            @RequestParam(name = DISPLAY_NAME_PART, required = false) String displayName,
            @Parameter(in = ParameterIn.HEADER, name = ResearchTraceFilter.TRACE_HEADER,
                    description = "Optional safe request correlation id")
            @RequestAttribute(ResearchTraceFilter.TRACE_ATTRIBUTE) String traceId) {
        Long identifier = identifier(knowledgeBaseId, "knowledgeBaseId", traceId);
        KnowledgeDocumentBO uploaded = knowledgeDocumentManage.upload(
                uploadCommand(identifier, file, displayName, traceId));
        log.info("knowledge document upload knowledgeBaseId={} documentId={} outcome=QUEUED",
                identifier, uploaded.documentId());
        return ResponseEntity.accepted().body(render(uploaded, traceId));
    }

    @GetMapping(path = "/api/v1/knowledge-bases/{knowledgeBaseId}/documents",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(operationId = "listKnowledgeDocuments", summary = "List the documents of a base",
            description = "Returns one page of documents, newest first, optionally filtered by status and name.")
    @SecurityRequirement(name = "researchApiKey")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "One page of documents"),
            @ApiResponse(responseCode = "400", description = "Validation failure",
                    content = @Content(schema = @Schema(implementation = DeepResearchErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "API key failure",
                    content = @Content(schema = @Schema(implementation = DeepResearchErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "The knowledge base does not exist",
                    content = @Content(schema = @Schema(implementation = DeepResearchErrorResponse.class)))
    })
    public PageResultRecord<KnowledgeDocumentVO> listKnowledgeDocuments(
            @PathVariable("knowledgeBaseId")
            @Pattern(regexp = IDENTIFIER_PATTERN, message = "knowledgeBaseId must be 1 to 64 characters of [A-Za-z0-9._-]")
            String knowledgeBaseId,
            @Parameter(description = "One-based page number")
            @RequestParam(name = "page", defaultValue = "1") @Min(1) int page,
            @Parameter(description = "Page size, at most 100")
            @RequestParam(name = "size", defaultValue = "20") @Min(1) @Max(100) int size,
            @Parameter(description = "Exact ingest status filter")
            @RequestParam(name = "status", required = false) DocumentIngestStatusEnum status,
            @Parameter(description = "Case-insensitive match on the display name")
            @RequestParam(name = "keyword", required = false) @Size(max = 64) String keyword,
            @Parameter(in = ParameterIn.HEADER, name = ResearchTraceFilter.TRACE_HEADER,
                    description = "Optional safe request correlation id")
            @RequestAttribute(ResearchTraceFilter.TRACE_ATTRIBUTE) String traceId) {
        PageResultRecord<KnowledgeDocumentBO> found = knowledgeDocumentManage.page(
                identifier(knowledgeBaseId, "knowledgeBaseId", traceId), page, size, status, keyword);
        List<KnowledgeDocumentVO> records = found.records().stream()
                .map(document -> render(document, traceId))
                .toList();
        PageMetaRecord meta = found.page();
        return PageResultRecord.success(records, meta.total(), page, size);
    }

    @GetMapping(path = "/api/v1/knowledge-documents/{documentId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(operationId = "getKnowledgeDocument", summary = "Get a document",
            description = "Returns the status and the diagnostics of one document; the stored text is "
                    + "reported as a length, never as a body.")
    @SecurityRequirement(name = "researchApiKey")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The document"),
            @ApiResponse(responseCode = "400", description = "Validation failure",
                    content = @Content(schema = @Schema(implementation = DeepResearchErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "API key failure",
                    content = @Content(schema = @Schema(implementation = DeepResearchErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "The document does not exist",
                    content = @Content(schema = @Schema(implementation = DeepResearchErrorResponse.class)))
    })
    public KnowledgeDocumentVO getKnowledgeDocument(
            @PathVariable("documentId")
            @Pattern(regexp = IDENTIFIER_PATTERN, message = "documentId must be 1 to 64 characters of [A-Za-z0-9._-]")
            String documentId,
            @Parameter(in = ParameterIn.HEADER, name = ResearchTraceFilter.TRACE_HEADER,
                    description = "Optional safe request correlation id")
            @RequestAttribute(ResearchTraceFilter.TRACE_ATTRIBUTE) String traceId) {
        return render(knowledgeDocumentManage.get(identifier(documentId, "documentId", traceId)), traceId);
    }

    @PostMapping(path = "/api/v1/knowledge-documents/{documentId}/reingest",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(operationId = "reingestKnowledgeDocument", summary = "Reprocess a document",
            description = "Queues another ingest attempt from the already stored text; the original is "
                    + "neither re-uploaded nor re-parsed.")
    @SecurityRequirement(name = "researchApiKey")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "The document is queued again"),
            @ApiResponse(responseCode = "400", description = "Validation failure or a request body was sent",
                    content = @Content(schema = @Schema(implementation = DeepResearchErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "API key failure",
                    content = @Content(schema = @Schema(implementation = DeepResearchErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "The document does not exist",
                    content = @Content(schema = @Schema(implementation = DeepResearchErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "The document is still in progress or has no text",
                    content = @Content(schema = @Schema(implementation = DeepResearchErrorResponse.class)))
    })
    public ResponseEntity<KnowledgeDocumentVO> reingestKnowledgeDocument(
            @PathVariable("documentId")
            @Pattern(regexp = IDENTIFIER_PATTERN, message = "documentId must be 1 to 64 characters of [A-Za-z0-9._-]")
            String documentId,
            @RequestBody(required = false) String body,
            @Parameter(in = ParameterIn.HEADER, name = ResearchTraceFilter.TRACE_HEADER,
                    description = "Optional safe request correlation id")
            @RequestAttribute(ResearchTraceFilter.TRACE_ATTRIBUTE) String traceId) {
        requireEmptyBody(body, traceId);
        Long identifier = identifier(documentId, "documentId", traceId);
        KnowledgeDocumentBO reingested = knowledgeDocumentManage.reingest(identifier);
        log.info("knowledge document reingest documentId={} outcome=QUEUED", identifier);
        return ResponseEntity.accepted().body(render(reingested, traceId));
    }

    @DeleteMapping(path = "/api/v1/knowledge-documents/{documentId}")
    @Operation(operationId = "deleteKnowledgeDocument", summary = "Delete a document",
            description = "Removes the document with its stored original and its chunks; it is not recoverable.")
    @SecurityRequirement(name = "researchApiKey")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "The document is gone"),
            @ApiResponse(responseCode = "400", description = "Validation failure",
                    content = @Content(schema = @Schema(implementation = DeepResearchErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "API key failure",
                    content = @Content(schema = @Schema(implementation = DeepResearchErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "The document does not exist",
                    content = @Content(schema = @Schema(implementation = DeepResearchErrorResponse.class)))
    })
    public ResponseEntity<Void> deleteKnowledgeDocument(
            @PathVariable("documentId")
            @Pattern(regexp = IDENTIFIER_PATTERN, message = "documentId must be 1 to 64 characters of [A-Za-z0-9._-]")
            String documentId,
            @Parameter(in = ParameterIn.HEADER, name = ResearchTraceFilter.TRACE_HEADER,
                    description = "Optional safe request correlation id")
            @RequestAttribute(ResearchTraceFilter.TRACE_ATTRIBUTE) String traceId) {
        Long identifier = identifier(documentId, "documentId", traceId);
        knowledgeDocumentManage.delete(identifier);
        log.info("knowledge document delete documentId={} outcome=DELETED", identifier);
        return ResponseEntity.noContent().build();
    }

    /** Renders one document with the correlation id the response contract carries beside it. */
    private KnowledgeDocumentVO render(KnowledgeDocumentBO document, String traceId) {
        return KnowledgeDocumentVoConverter.INSTANCE.toVO(document, traceId);
    }

    /**
     * Binds the multipart parts to the upload command.
     *
     * <p>The part is read here because a byte array is what the use case takes; an unreadable part is
     * this server's failure, not the caller's, so it is answered as an internal error rather than as a
     * rejected field. A blank display name is rejected rather than defaulted: the contract accepts a
     * name of 1 to 255 characters after trimming, so a part that carries none is malformed.
     */
    private static UploadKnowledgeDocumentCommand uploadCommand(Long knowledgeBaseId, MultipartFile file,
                                                                String displayName, String traceId) {
        if (file == null || file.isEmpty()) {
            throw KnowledgeApplicationException.onField(KnowledgeErrorCodeEnum.KNOWLEDGE_VALIDATION_ERROR,
                    traceId, FILE_PART, "the file part must carry a non-empty file", null);
        }
        String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.isBlank()) {
            throw KnowledgeApplicationException.onField(KnowledgeErrorCodeEnum.KNOWLEDGE_VALIDATION_ERROR,
                    traceId, FILE_PART, "the file part must name the file it carries", null);
        }
        String name = displayName == null ? null : displayName.trim();
        if (name != null && (name.isEmpty() || name.length() > MAX_DISPLAY_NAME_LENGTH)) {
            throw KnowledgeApplicationException.onField(KnowledgeErrorCodeEnum.KNOWLEDGE_VALIDATION_ERROR,
                    traceId, DISPLAY_NAME_PART, "displayName must be 1 to 255 characters after trimming", null);
        }
        return new UploadKnowledgeDocumentCommand(knowledgeBaseId, fileName, file.getContentType(), name,
                content(file, traceId), traceId);
    }

    private static byte[] content(MultipartFile file, String traceId) {
        try {
            return file.getBytes();
        } catch (IOException unreadable) {
            throw new KnowledgeApplicationException(KnowledgeErrorCodeEnum.KNOWLEDGE_INTERNAL_ERROR, traceId,
                    Map.of(), unreadable);
        }
    }

    private static void requireEmptyBody(String body, String traceId) {
        if (body != null && !body.isBlank()) {
            throw KnowledgeApplicationException.onField(KnowledgeErrorCodeEnum.KNOWLEDGE_VALIDATION_ERROR,
                    traceId, "body", "this action carries no request body", null);
        }
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
