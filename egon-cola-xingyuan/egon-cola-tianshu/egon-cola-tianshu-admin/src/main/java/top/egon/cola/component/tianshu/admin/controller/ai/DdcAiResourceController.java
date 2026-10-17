package top.egon.cola.component.tianshu.admin.controller.ai;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.common.core.pojo.PageResultRecord;
import top.egon.cola.component.common.core.pojo.ResultRecord;
import top.egon.cola.component.tianshu.admin.model.command.DdcAiCreateCommand;
import top.egon.cola.component.tianshu.admin.model.command.DdcAiDeleteCommand;
import top.egon.cola.component.tianshu.admin.model.command.DdcAiDraftCommand;
import top.egon.cola.component.tianshu.admin.model.command.DdcAiOptimizationPolicyCommand;
import top.egon.cola.component.tianshu.admin.model.command.DdcAiOptimizationTriggerCommand;
import top.egon.cola.component.tianshu.admin.model.command.DdcAiPromotionCommand;
import top.egon.cola.component.tianshu.admin.model.command.DdcAiPublishCommand;
import top.egon.cola.component.tianshu.admin.model.command.DdcAiRollbackCommand;
import top.egon.cola.component.tianshu.admin.model.dto.DdcAiResourceSummaryDTO;
import top.egon.cola.component.tianshu.admin.model.dto.DdcAiVersionSummaryDTO;
import top.egon.cola.component.tianshu.admin.model.enums.DdcAiOutcomePersistenceEnum;
import top.egon.cola.component.tianshu.admin.model.enums.DdcAiRunStatePersistenceEnum;
import top.egon.cola.component.tianshu.admin.model.query.DdcAiIdQuery;
import top.egon.cola.component.tianshu.admin.model.query.DdcAiOptimizationRunPageQuery;
import top.egon.cola.component.tianshu.admin.model.query.DdcAiPageQuery;
import top.egon.cola.component.tianshu.admin.model.query.DdcAiPublicationPageQuery;
import top.egon.cola.component.tianshu.admin.model.query.DdcAiUsageFeedbackPageQuery;
import top.egon.cola.component.tianshu.admin.model.query.DdcAiVersionPageQuery;
import top.egon.cola.component.tianshu.admin.model.query.DdcAiVersionQuery;
import top.egon.cola.component.tianshu.admin.model.vo.DdcAiOptimizationPolicyResult;
import top.egon.cola.component.tianshu.admin.model.vo.DdcAiOptimizationRunResult;
import top.egon.cola.component.tianshu.admin.model.vo.DdcAiPublicationResult;
import top.egon.cola.component.tianshu.admin.model.vo.DdcAiResourceResult;
import top.egon.cola.component.tianshu.admin.model.vo.DdcAiUsageFeedbackResult;
import top.egon.cola.component.tianshu.admin.model.vo.DdcAiVersionResult;
import top.egon.cola.component.tianshu.admin.model.vo.DdcPageResult;
import top.egon.cola.component.tianshu.admin.service.ai.DdcAiFeedbackService;
import top.egon.cola.component.tianshu.admin.service.ai.DdcAiOptimizationService;
import top.egon.cola.component.tianshu.admin.service.ai.DdcAiResourceService;
import top.egon.cola.component.tianshu.admin.support.DdcAdminPageSupport;
import top.egon.cola.component.tianshu.model.ai.DdcAiKindEnum;
import top.egon.cola.component.yuheng.openapi.annotation.EgonApiCatalog;
import top.egon.cola.component.yuheng.openapi.annotation.EgonGatewayPolicy;

import java.net.URI;

@RestController
@RequestMapping(path = "/api/v1/tianshu/ai/resources", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Tianshu AI Resources", description = "Prompt 与 Skill 多租户管理接口")
@EgonApiCatalog(
        businessDomainCode = "xingyuan",
        businessDomainName = "平台治理域",
        entityDomainCode = "tianshu-admin",
        entityDomainName = "Dynamic Config Center 管理实体域",
        interfaceGroupCode = "tianshu"
)
@RequiredArgsConstructor
@Validated
public class DdcAiResourceController {

    private static final String TENANT_HEADER = "X-Egon-Tianshu-Tenant";
    private static final String IDEMPOTENCY_HEADER = "Idempotency-Key";
    private static final String BASE_PATH = "/api/v1/tianshu/ai/resources";
    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;
    private static final String LONG_ID_PATTERN = "^[1-9][0-9]{0,18}$";
    private static final String NON_NEGATIVE_LONG_PATTERN = "^(0|[1-9][0-9]{0,18})$";
    private static final String IDEMPOTENCY_PATTERN = "^[A-Za-z0-9_-]{16,64}$";

    @Qualifier("ddcAiResourceService")
    private final DdcAiResourceService aiResourceService;

    @Qualifier("ddcAiOptimizationService")
    private final DdcAiOptimizationService optimizationService;

    @Qualifier("ddcAiFeedbackService")
    private final DdcAiFeedbackService feedbackService;

    @Operation(operationId = "pageAiResources", summary = "浏览范围内资源",
            security = @SecurityRequirement(name = "tianshuBearerAuth"))
    @Parameters({
            @Parameter(name = TENANT_HEADER, in = ParameterIn.HEADER, required = true,
                    description = "经认证的身份租户期望值；服务端只比较，不用它选择租户")
    })
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paged resources",
                    headers = @io.swagger.v3.oas.annotations.headers.Header(name = "Cache-Control",
                            schema = @Schema(type = "string", example = "no-store"))),
            @ApiResponse(responseCode = "400", description = "Invalid query", content = @Content(
                    mediaType = JSON, schema = @Schema(implementation = ResultRecord.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(
                    mediaType = JSON, schema = @Schema(oneOf = {DdcAdminSecurityErrorResult.class, ResultRecord.class}))),
            @ApiResponse(responseCode = "403", description = "Scope permission denied", content = @Content(
                    mediaType = JSON, schema = @Schema(oneOf = {DdcAdminSecurityErrorResult.class, ResultRecord.class}))),
            @ApiResponse(responseCode = "404", description = "Scope not found", content = @Content(
                    mediaType = JSON, schema = @Schema(implementation = ResultRecord.class))),
            @ApiResponse(responseCode = "503", description = "Tenant or persistence unavailable", content = @Content(
                    mediaType = JSON, schema = @Schema(implementation = ResultRecord.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected failure", content = @Content(
                    mediaType = JSON, schema = @Schema(implementation = ResultRecord.class)))
    })
    @EgonGatewayPolicy(exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @GetMapping
    public ResponseEntity<PageResultRecord<DdcAiResourceSummaryDTO>> page(
            @Parameter(description = "Business code", required = true,
                    schema = @Schema(type = "string", minLength = 1, maxLength = 128, pattern = "^[^\\p{Cntrl}]+$"))
            @RequestParam("bizCode") @NotBlank @Size(max = 128)
            @Pattern(regexp = "^[^\\p{Cntrl}]+$") String bizCode,
            @Parameter(description = "Namespace code", required = true,
                    schema = @Schema(type = "string", minLength = 1, maxLength = 128, pattern = "^[^\\p{Cntrl}]+$"))
            @RequestParam("namespaceCode") @NotBlank @Size(max = 128)
            @Pattern(regexp = "^[^\\p{Cntrl}]+$") String namespaceCode,
            @Parameter(description = "Environment code", required = true,
                    schema = @Schema(type = "string", minLength = 1, maxLength = 32, pattern = "^[^\\p{Cntrl}]+$"))
            @RequestParam("env") @NotBlank @Size(max = 32)
            @Pattern(regexp = "^[^\\p{Cntrl}]+$") String env,
            @Parameter(description = "Application code", required = true,
                    schema = @Schema(type = "string", minLength = 1, maxLength = 128, pattern = "^[^\\p{Cntrl}]+$"))
            @RequestParam("appCode") @NotBlank @Size(max = 128)
            @Pattern(regexp = "^[^\\p{Cntrl}]+$") String appCode,
            @Parameter(description = "Resource kind", required = true,
                    schema = @Schema(type = "string", allowableValues = {"PROMPT", "SKILL"}))
            @RequestParam("kind") @NotNull DdcAiKindEnum kind,
            @Parameter(description = "Optional literal resource-key prefix", schema = @Schema(type = "string"))
            @RequestParam(value = "resourceKeyPrefix", required = false) @Size(max = 128) String resourceKeyPrefix,
            @Parameter(description = "One-based page number", schema = @Schema(type = "integer", minimum = "1"))
            @RequestParam(value = "pageNo", defaultValue = "1") @Min(1) int pageNo,
            @Parameter(description = "Page size, 1..100", schema = @Schema(type = "integer", minimum = "1", maximum = "100"))
            @RequestParam(value = "pageSize", defaultValue = "20") @Min(1) @Max(100) int pageSize) {
        DdcAiPageQuery query = DdcAiPageQuery.builder().bizCode(bizCode).namespaceCode(namespaceCode)
                .env(env).appCode(appCode).kind(kind).resourceKeyPrefix(resourceKeyPrefix)
                .pageNo(pageNo).pageSize(pageSize).build();
        return noStore(DdcAdminPageSupport.result(aiResourceService.page(query)));
    }

    @Operation(operationId = "createAiResource", summary = "创建草稿资源",
            security = @SecurityRequirement(name = "tianshuBearerAuth"))
    @Parameters({
            @Parameter(name = TENANT_HEADER, in = ParameterIn.HEADER, required = true)
    })
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Resource created",
                    headers = {
                            @io.swagger.v3.oas.annotations.headers.Header(name = "Location", schema = @Schema(type = "string")),
                            @io.swagger.v3.oas.annotations.headers.Header(name = "Cache-Control", schema = @Schema(type = "string", example = "no-store"))
                    }),
            @ApiResponse(responseCode = "400", description = "Invalid resource or content", content = @Content(
                    mediaType = JSON, schema = @Schema(implementation = ResultRecord.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(
                    mediaType = JSON, schema = @Schema(oneOf = {DdcAdminSecurityErrorResult.class, ResultRecord.class}))),
            @ApiResponse(responseCode = "403", description = "Scope permission denied", content = @Content(
                    mediaType = JSON, schema = @Schema(oneOf = {DdcAdminSecurityErrorResult.class, ResultRecord.class}))),
            @ApiResponse(responseCode = "404", description = "Scope not found", content = @Content(
                    mediaType = JSON, schema = @Schema(implementation = ResultRecord.class))),
            @ApiResponse(responseCode = "409", description = "Resource key conflict", content = @Content(
                    mediaType = JSON, schema = @Schema(implementation = ResultRecord.class))),
            @ApiResponse(responseCode = "413", description = "Request body too large", content = @Content(
                    mediaType = JSON, schema = @Schema(implementation = ResultRecord.class))),
            @ApiResponse(responseCode = "503", description = "Tenant or persistence unavailable", content = @Content(
                    mediaType = JSON, schema = @Schema(implementation = ResultRecord.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected failure", content = @Content(
                    mediaType = JSON, schema = @Schema(implementation = ResultRecord.class)))
    })
    @EgonGatewayPolicy(exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResultRecord<DdcAiResourceResult>> create(
            @Valid @RequestBody DdcAiCreateCommand command) {
        DdcAiResourceResult result = aiResourceService.create(command);
        URI location = resourceLocation(result.getResource().getId());
        return ResponseEntity.created(location).header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(ResultRecord.success(result));
    }

    @Operation(operationId = "getAiResource", summary = "读取草稿和当前状态",
            security = @SecurityRequirement(name = "tianshuBearerAuth"))
    @Parameters({
            @Parameter(name = TENANT_HEADER, in = ParameterIn.HEADER, required = true)
    })
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resource and current draft",
                    headers = @io.swagger.v3.oas.annotations.headers.Header(name = "Cache-Control", schema = @Schema(type = "string", example = "no-store"))),
            @ApiResponse(responseCode = "400", description = "Invalid resource id", content = @Content(mediaType = JSON, schema = @Schema(implementation = ResultRecord.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(mediaType = JSON, schema = @Schema(oneOf = {DdcAdminSecurityErrorResult.class, ResultRecord.class}))),
            @ApiResponse(responseCode = "403", description = "Scope permission denied", content = @Content(mediaType = JSON, schema = @Schema(oneOf = {DdcAdminSecurityErrorResult.class, ResultRecord.class}))),
            @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content(mediaType = JSON, schema = @Schema(implementation = ResultRecord.class))),
            @ApiResponse(responseCode = "503", description = "Tenant or persistence unavailable", content = @Content(mediaType = JSON, schema = @Schema(implementation = ResultRecord.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected failure", content = @Content(mediaType = JSON, schema = @Schema(implementation = ResultRecord.class)))
    })
    @EgonGatewayPolicy(exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @GetMapping(path = "/{id}")
    public ResponseEntity<ResultRecord<DdcAiResourceResult>> get(
            @Parameter(description = "Resource ID, positive decimal string", required = true,
                    schema = @Schema(type = "string", pattern = LONG_ID_PATTERN))
            @PathVariable("id") Long id) {
        return noStore(ResultRecord.success(aiResourceService.get(new DdcAiIdQuery(id))));
    }

    @Operation(operationId = "replaceAiDraft", summary = "完整保存草稿",
            security = @SecurityRequirement(name = "tianshuBearerAuth"))
    @Parameters({
            @Parameter(name = TENANT_HEADER, in = ParameterIn.HEADER, required = true)
    })
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Draft replaced",
                    headers = @io.swagger.v3.oas.annotations.headers.Header(name = "Cache-Control", schema = @Schema(type = "string", example = "no-store"))),
            @ApiResponse(responseCode = "400", description = "Invalid resource or content", content = @Content(mediaType = JSON, schema = @Schema(implementation = ResultRecord.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(mediaType = JSON, schema = @Schema(oneOf = {DdcAdminSecurityErrorResult.class, ResultRecord.class}))),
            @ApiResponse(responseCode = "403", description = "Scope permission denied", content = @Content(mediaType = JSON, schema = @Schema(oneOf = {DdcAdminSecurityErrorResult.class, ResultRecord.class}))),
            @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content(mediaType = JSON, schema = @Schema(implementation = ResultRecord.class))),
            @ApiResponse(responseCode = "409", description = "Revision conflict", content = @Content(mediaType = JSON, schema = @Schema(implementation = ResultRecord.class))),
            @ApiResponse(responseCode = "413", description = "Request body too large", content = @Content(mediaType = JSON, schema = @Schema(implementation = ResultRecord.class))),
            @ApiResponse(responseCode = "503", description = "Tenant or persistence unavailable", content = @Content(mediaType = JSON, schema = @Schema(implementation = ResultRecord.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected failure", content = @Content(mediaType = JSON, schema = @Schema(implementation = ResultRecord.class)))
    })
    @EgonGatewayPolicy(exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @PutMapping(path = "/{id}/draft", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResultRecord<DdcAiResourceResult>> replaceDraft(
            @Parameter(description = "Resource ID, positive decimal string", required = true,
                    schema = @Schema(type = "string", pattern = LONG_ID_PATTERN))
            @PathVariable("id") Long id,
            @Valid @RequestBody DdcAiDraftCommand command) {
        return noStore(ResultRecord.success(aiResourceService.replaceDraft(new DdcAiIdQuery(id), command)));
    }

    @Operation(operationId = "deleteAiResource", summary = "删除资源可见性",
            security = @SecurityRequirement(name = "tianshuBearerAuth"))
    @Parameters({
            @Parameter(name = TENANT_HEADER, in = ParameterIn.HEADER, required = true),
            @Parameter(name = "expectedRevision", in = ParameterIn.QUERY, required = true,
                    description = "Expected non-negative resource revision", schema = @Schema(type = "string", pattern = NON_NEGATIVE_LONG_PATTERN))
    })
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Resource deletion acknowledged",
                    headers = @io.swagger.v3.oas.annotations.headers.Header(name = "Cache-Control", schema = @Schema(type = "string", example = "no-store"))),
            @ApiResponse(responseCode = "400", description = "Invalid resource id or revision", content = @Content(mediaType = JSON, schema = @Schema(implementation = ResultRecord.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(mediaType = JSON, schema = @Schema(oneOf = {DdcAdminSecurityErrorResult.class, ResultRecord.class}))),
            @ApiResponse(responseCode = "403", description = "Scope permission denied", content = @Content(mediaType = JSON, schema = @Schema(oneOf = {DdcAdminSecurityErrorResult.class, ResultRecord.class}))),
            @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content(mediaType = JSON, schema = @Schema(implementation = ResultRecord.class))),
            @ApiResponse(responseCode = "409", description = "Revision conflict", content = @Content(mediaType = JSON, schema = @Schema(implementation = ResultRecord.class))),
            @ApiResponse(responseCode = "503", description = "Tenant or persistence unavailable", content = @Content(mediaType = JSON, schema = @Schema(implementation = ResultRecord.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected failure", content = @Content(mediaType = JSON, schema = @Schema(implementation = ResultRecord.class)))
    })
    @EgonGatewayPolicy(exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Resource ID, positive decimal string", required = true,
                    schema = @Schema(type = "string", pattern = LONG_ID_PATTERN))
            @PathVariable("id") Long id,
            @RequestParam("expectedRevision") @NotBlank @Size(max = 19)
            @Pattern(regexp = NON_NEGATIVE_LONG_PATTERN) String expectedRevision) {
        aiResourceService.delete(new DdcAiIdQuery(id), DdcAiDeleteCommand.builder()
                .expectedRevision(expectedRevision).build());
        return ResponseEntity.noContent().header(HttpHeaders.CACHE_CONTROL, "no-store").build();
    }

    @Operation(operationId = "pageAiVersions", summary = "浏览不可变版本",
            security = @SecurityRequirement(name = "tianshuBearerAuth"))
    @Parameters({
            @Parameter(name = TENANT_HEADER, in = ParameterIn.HEADER, required = true)
    })
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paged versions",
                    headers = @io.swagger.v3.oas.annotations.headers.Header(name = "Cache-Control", schema = @Schema(type = "string", example = "no-store"))),
            @ApiResponse(responseCode = "400", description = "Invalid resource id or page", content = @Content(mediaType = JSON, schema = @Schema(implementation = ResultRecord.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(mediaType = JSON, schema = @Schema(oneOf = {DdcAdminSecurityErrorResult.class, ResultRecord.class}))),
            @ApiResponse(responseCode = "403", description = "Scope permission denied", content = @Content(mediaType = JSON, schema = @Schema(oneOf = {DdcAdminSecurityErrorResult.class, ResultRecord.class}))),
            @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content(mediaType = JSON, schema = @Schema(implementation = ResultRecord.class))),
            @ApiResponse(responseCode = "503", description = "Tenant or persistence unavailable", content = @Content(mediaType = JSON, schema = @Schema(implementation = ResultRecord.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected failure", content = @Content(mediaType = JSON, schema = @Schema(implementation = ResultRecord.class)))
    })
    @EgonGatewayPolicy(exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @GetMapping("/{id}/versions")
    public ResponseEntity<PageResultRecord<DdcAiVersionSummaryDTO>> pageVersions(
            @Parameter(description = "Resource ID, positive decimal string", required = true,
                    schema = @Schema(type = "string", pattern = LONG_ID_PATTERN))
            @PathVariable("id") Long id,
            @Parameter(description = "One-based page number", schema = @Schema(type = "integer", minimum = "1"))
            @RequestParam(value = "pageNo", defaultValue = "1") @Min(1) int pageNo,
            @Parameter(description = "Page size, 1..100", schema = @Schema(type = "integer", minimum = "1", maximum = "100"))
            @RequestParam(value = "pageSize", defaultValue = "20") @Min(1) @Max(100) int pageSize) {
        DdcAiVersionPageQuery query = DdcAiVersionPageQuery.builder().resourceId(id)
                .pageNo(pageNo).pageSize(pageSize).build();
        return noStore(DdcAdminPageSupport.result(aiResourceService.pageVersions(query)));
    }

    @Operation(operationId = "getAiVersion", summary = "读取完整历史快照",
            security = @SecurityRequirement(name = "tianshuBearerAuth"))
    @Parameters({
            @Parameter(name = TENANT_HEADER, in = ParameterIn.HEADER, required = true)
    })
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Exact immutable version and full content",
                    headers = @io.swagger.v3.oas.annotations.headers.Header(name = "Cache-Control", schema = @Schema(type = "string", example = "no-store"))),
            @ApiResponse(responseCode = "400", description = "Invalid resource or version id", content = @Content(mediaType = JSON, schema = @Schema(implementation = ResultRecord.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(mediaType = JSON, schema = @Schema(oneOf = {DdcAdminSecurityErrorResult.class, ResultRecord.class}))),
            @ApiResponse(responseCode = "403", description = "Scope permission denied", content = @Content(mediaType = JSON, schema = @Schema(oneOf = {DdcAdminSecurityErrorResult.class, ResultRecord.class}))),
            @ApiResponse(responseCode = "404", description = "Resource or version not found", content = @Content(mediaType = JSON, schema = @Schema(implementation = ResultRecord.class))),
            @ApiResponse(responseCode = "503", description = "Tenant or persistence unavailable", content = @Content(mediaType = JSON, schema = @Schema(implementation = ResultRecord.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected failure", content = @Content(mediaType = JSON, schema = @Schema(implementation = ResultRecord.class)))
    })
    @EgonGatewayPolicy(exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @GetMapping("/{id}/versions/{versionNo}")
    public ResponseEntity<ResultRecord<DdcAiVersionResult>> getVersion(
            @Parameter(description = "Resource ID, positive decimal string", required = true,
                    schema = @Schema(type = "string", pattern = LONG_ID_PATTERN))
            @PathVariable("id") Long id,
            @Parameter(description = "Exact positive version number", required = true,
                    schema = @Schema(type = "string", pattern = LONG_ID_PATTERN))
            @PathVariable("versionNo") Long versionNo) {
        return noStore(ResultRecord.success(aiResourceService.getVersion(new DdcAiVersionQuery(id, versionNo))));
    }

    @Operation(operationId = "publishAiDraft", summary = "发布当前草稿",
            security = @SecurityRequirement(name = "tianshuBearerAuth"))
    @Parameters({
            @Parameter(name = TENANT_HEADER, in = ParameterIn.HEADER, required = true),
            @Parameter(name = IDEMPOTENCY_HEADER, in = ParameterIn.HEADER, required = true,
                    schema = @Schema(type = "string", pattern = IDEMPOTENCY_PATTERN, minLength = 16, maxLength = 64))
    })
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Immutable version published",
                    headers = {
                            @io.swagger.v3.oas.annotations.headers.Header(name = "Location", schema = @Schema(type = "string")),
                            @io.swagger.v3.oas.annotations.headers.Header(name = "Cache-Control", schema = @Schema(type = "string", example = "no-store"))
                    }),
            @ApiResponse(responseCode = "400", description = "Invalid command", content = @Content(mediaType = JSON, schema = @Schema(implementation = ResultRecord.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(mediaType = JSON, schema = @Schema(oneOf = {DdcAdminSecurityErrorResult.class, ResultRecord.class}))),
            @ApiResponse(responseCode = "403", description = "Scope permission denied", content = @Content(mediaType = JSON, schema = @Schema(oneOf = {DdcAdminSecurityErrorResult.class, ResultRecord.class}))),
            @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content(mediaType = JSON, schema = @Schema(implementation = ResultRecord.class))),
            @ApiResponse(responseCode = "409", description = "Revision or idempotency conflict", content = @Content(mediaType = JSON, schema = @Schema(implementation = ResultRecord.class))),
            @ApiResponse(responseCode = "413", description = "Request body too large", content = @Content(mediaType = JSON, schema = @Schema(implementation = ResultRecord.class))),
            @ApiResponse(responseCode = "503", description = "Tenant or persistence unavailable", content = @Content(mediaType = JSON, schema = @Schema(implementation = ResultRecord.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected failure", content = @Content(mediaType = JSON, schema = @Schema(implementation = ResultRecord.class)))
    })
    @EgonGatewayPolicy(exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @PostMapping(path = "/{id}/publications", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResultRecord<DdcAiPublicationResult>> publish(
            @Parameter(description = "Resource ID, positive decimal string", required = true,
                    schema = @Schema(type = "string", pattern = LONG_ID_PATTERN))
            @PathVariable("id") Long id,
            @org.springframework.web.bind.annotation.RequestHeader(IDEMPOTENCY_HEADER)
            @NotBlank @Size(min = 16, max = 64) @Pattern(regexp = IDEMPOTENCY_PATTERN) String requestId,
            @Valid @RequestBody DdcAiPublishCommand command) {
        DdcAiPublicationResult result = aiResourceService.publish(new DdcAiIdQuery(id), requestId, command);
        return createdVersion(result, result.getResourceId());
    }

    @Operation(operationId = "rollbackAiPublication", summary = "回滚当前发布指针",
            security = @SecurityRequirement(name = "tianshuBearerAuth"))
    @Parameters({
            @Parameter(name = TENANT_HEADER, in = ParameterIn.HEADER, required = true),
            @Parameter(name = IDEMPOTENCY_HEADER, in = ParameterIn.HEADER, required = true,
                    schema = @Schema(type = "string", pattern = IDEMPOTENCY_PATTERN, minLength = 16, maxLength = 64))
    })
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Current version pointer changed",
                    headers = @io.swagger.v3.oas.annotations.headers.Header(name = "Cache-Control", schema = @Schema(type = "string", example = "no-store"))),
            @ApiResponse(responseCode = "400", description = "Invalid command", content = @Content(mediaType = JSON, schema = @Schema(implementation = ResultRecord.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(mediaType = JSON, schema = @Schema(oneOf = {DdcAdminSecurityErrorResult.class, ResultRecord.class}))),
            @ApiResponse(responseCode = "403", description = "Scope permission denied", content = @Content(mediaType = JSON, schema = @Schema(oneOf = {DdcAdminSecurityErrorResult.class, ResultRecord.class}))),
            @ApiResponse(responseCode = "404", description = "Resource or version not found", content = @Content(mediaType = JSON, schema = @Schema(implementation = ResultRecord.class))),
            @ApiResponse(responseCode = "409", description = "Revision or idempotency conflict", content = @Content(mediaType = JSON, schema = @Schema(implementation = ResultRecord.class))),
            @ApiResponse(responseCode = "413", description = "Request body too large", content = @Content(mediaType = JSON, schema = @Schema(implementation = ResultRecord.class))),
            @ApiResponse(responseCode = "503", description = "Tenant or persistence unavailable", content = @Content(mediaType = JSON, schema = @Schema(implementation = ResultRecord.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected failure", content = @Content(mediaType = JSON, schema = @Schema(implementation = ResultRecord.class)))
    })
    @EgonGatewayPolicy(exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @PostMapping(path = "/{id}/rollbacks", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResultRecord<DdcAiPublicationResult>> rollback(
            @Parameter(description = "Resource ID, positive decimal string", required = true,
                    schema = @Schema(type = "string", pattern = LONG_ID_PATTERN))
            @PathVariable("id") Long id,
            @org.springframework.web.bind.annotation.RequestHeader(IDEMPOTENCY_HEADER)
            @NotBlank @Size(min = 16, max = 64) @Pattern(regexp = IDEMPOTENCY_PATTERN) String requestId,
            @Valid @RequestBody DdcAiRollbackCommand command) {
        return noStore(ResultRecord.success(aiResourceService.rollback(new DdcAiIdQuery(id), requestId, command)));
    }

    @Operation(operationId = "promoteAiVersion", summary = "跨namespace复制并发布",
            security = @SecurityRequirement(name = "tianshuBearerAuth"))
    @Parameters({
            @Parameter(name = TENANT_HEADER, in = ParameterIn.HEADER, required = true),
            @Parameter(name = IDEMPOTENCY_HEADER, in = ParameterIn.HEADER, required = true,
                    schema = @Schema(type = "string", pattern = IDEMPOTENCY_PATTERN, minLength = 16, maxLength = 64))
    })
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Exact source version promoted to an independent target version",
                    headers = {
                            @io.swagger.v3.oas.annotations.headers.Header(name = "Location", schema = @Schema(type = "string")),
                            @io.swagger.v3.oas.annotations.headers.Header(name = "Cache-Control", schema = @Schema(type = "string", example = "no-store"))
                    }),
            @ApiResponse(responseCode = "400", description = "Invalid command", content = @Content(mediaType = JSON, schema = @Schema(implementation = ResultRecord.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(mediaType = JSON, schema = @Schema(oneOf = {DdcAdminSecurityErrorResult.class, ResultRecord.class}))),
            @ApiResponse(responseCode = "403", description = "Scope permission denied", content = @Content(mediaType = JSON, schema = @Schema(oneOf = {DdcAdminSecurityErrorResult.class, ResultRecord.class}))),
            @ApiResponse(responseCode = "404", description = "Source or scope not found", content = @Content(mediaType = JSON, schema = @Schema(implementation = ResultRecord.class))),
            @ApiResponse(responseCode = "409", description = "Target or idempotency conflict", content = @Content(mediaType = JSON, schema = @Schema(implementation = ResultRecord.class))),
            @ApiResponse(responseCode = "413", description = "Request body too large", content = @Content(mediaType = JSON, schema = @Schema(implementation = ResultRecord.class))),
            @ApiResponse(responseCode = "503", description = "Tenant or persistence unavailable", content = @Content(mediaType = JSON, schema = @Schema(implementation = ResultRecord.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected failure", content = @Content(mediaType = JSON, schema = @Schema(implementation = ResultRecord.class)))
    })
    @EgonGatewayPolicy(exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @PostMapping(path = "/{id}/promotions", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResultRecord<DdcAiPublicationResult>> promote(
            @Parameter(description = "Source resource ID, positive decimal string", required = true,
                    schema = @Schema(type = "string", pattern = LONG_ID_PATTERN))
            @PathVariable("id") Long id,
            @org.springframework.web.bind.annotation.RequestHeader(IDEMPOTENCY_HEADER)
            @NotBlank @Size(min = 16, max = 64) @Pattern(regexp = IDEMPOTENCY_PATTERN) String requestId,
            @Valid @RequestBody DdcAiPromotionCommand command) {
        DdcAiPublicationResult result = aiResourceService.promote(new DdcAiIdQuery(id), command, requestId);
        return createdVersion(result, result.getResourceId());
    }

    @Operation(operationId = "pageAiPublications", summary = "浏览发布与推广审计",
            security = @SecurityRequirement(name = "tianshuBearerAuth"))
    @Parameters({
            @Parameter(name = TENANT_HEADER, in = ParameterIn.HEADER, required = true)
    })
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paged publication and promotion receipts",
                    headers = @io.swagger.v3.oas.annotations.headers.Header(name = "Cache-Control", schema = @Schema(type = "string", example = "no-store"))),
            @ApiResponse(responseCode = "400", description = "Invalid query", content = @Content(mediaType = JSON, schema = @Schema(implementation = ResultRecord.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(mediaType = JSON, schema = @Schema(oneOf = {DdcAdminSecurityErrorResult.class, ResultRecord.class}))),
            @ApiResponse(responseCode = "403", description = "Scope permission denied", content = @Content(mediaType = JSON, schema = @Schema(oneOf = {DdcAdminSecurityErrorResult.class, ResultRecord.class}))),
            @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content(mediaType = JSON, schema = @Schema(implementation = ResultRecord.class))),
            @ApiResponse(responseCode = "503", description = "Tenant or persistence unavailable", content = @Content(mediaType = JSON, schema = @Schema(implementation = ResultRecord.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected failure", content = @Content(mediaType = JSON, schema = @Schema(implementation = ResultRecord.class)))
    })
    @EgonGatewayPolicy(exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @GetMapping("/{id}/publications")
    public ResponseEntity<PageResultRecord<DdcAiPublicationResult>> pagePublications(
            @Parameter(description = "Resource ID, positive decimal string", required = true,
                    schema = @Schema(type = "string", pattern = LONG_ID_PATTERN))
            @PathVariable("id") Long id,
            @Parameter(description = "Optional exact idempotency request ID",
                    schema = @Schema(type = "string", pattern = IDEMPOTENCY_PATTERN, minLength = 16, maxLength = 64))
            @RequestParam(value = "requestId", required = false) @Size(min = 16, max = 64)
            @Pattern(regexp = IDEMPOTENCY_PATTERN) String requestId,
            @Parameter(description = "One-based page number", schema = @Schema(type = "integer", minimum = "1"))
            @RequestParam(value = "pageNo", defaultValue = "1") @Min(1) int pageNo,
            @Parameter(description = "Page size, 1..100", schema = @Schema(type = "integer", minimum = "1", maximum = "100"))
            @RequestParam(value = "pageSize", defaultValue = "20") @Min(1) @Max(100) int pageSize) {
        DdcAiPublicationPageQuery query = DdcAiPublicationPageQuery.builder().resourceId(id)
                .requestId(requestId).pageNo(pageNo).pageSize(pageSize).build();
        return noStore(DdcAdminPageSupport.result(aiResourceService.pagePublications(query)));
    }

    @Operation(operationId = "getAiOptimizationPolicy", summary = "读取优化策略与评估快照",
            security = @SecurityRequirement(name = "tianshuBearerAuth"))
    @Parameters({
            @Parameter(name = TENANT_HEADER, in = ParameterIn.HEADER, required = true)
    })
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Optimization policy, or defaults when not persisted",
                    headers = @io.swagger.v3.oas.annotations.headers.Header(name = "Cache-Control", schema = @Schema(type = "string", example = "no-store"))),
            @ApiResponse(responseCode = "400", description = "Invalid resource id", content = @Content(mediaType = JSON, schema = @Schema(implementation = ResultRecord.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(mediaType = JSON, schema = @Schema(oneOf = {DdcAdminSecurityErrorResult.class, ResultRecord.class}))),
            @ApiResponse(responseCode = "403", description = "Scope permission denied", content = @Content(mediaType = JSON, schema = @Schema(oneOf = {DdcAdminSecurityErrorResult.class, ResultRecord.class}))),
            @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content(mediaType = JSON, schema = @Schema(implementation = ResultRecord.class))),
            @ApiResponse(responseCode = "503", description = "Tenant or persistence unavailable", content = @Content(mediaType = JSON, schema = @Schema(implementation = ResultRecord.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected failure", content = @Content(mediaType = JSON, schema = @Schema(implementation = ResultRecord.class)))
    })
    @EgonGatewayPolicy(exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @GetMapping("/{id}/optimization-policy")
    public ResponseEntity<ResultRecord<DdcAiOptimizationPolicyResult>> getOptimizationPolicy(
            @Parameter(description = "Resource ID, positive decimal string", required = true,
                    schema = @Schema(type = "string", pattern = LONG_ID_PATTERN))
            @PathVariable("id") Long id) {
        return noStore(ResultRecord.success(optimizationService.getPolicy(new DdcAiIdQuery(id))));
    }

    @Operation(operationId = "replaceAiOptimizationPolicy", summary = "完整替换优化策略",
            security = @SecurityRequirement(name = "tianshuBearerAuth"))
    @Parameters({
            @Parameter(name = TENANT_HEADER, in = ParameterIn.HEADER, required = true)
    })
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Optimization policy replaced",
                    headers = @io.swagger.v3.oas.annotations.headers.Header(name = "Cache-Control", schema = @Schema(type = "string", example = "no-store"))),
            @ApiResponse(responseCode = "400", description = "Invalid policy", content = @Content(mediaType = JSON, schema = @Schema(implementation = ResultRecord.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(mediaType = JSON, schema = @Schema(oneOf = {DdcAdminSecurityErrorResult.class, ResultRecord.class}))),
            @ApiResponse(responseCode = "403", description = "Scope permission denied", content = @Content(mediaType = JSON, schema = @Schema(oneOf = {DdcAdminSecurityErrorResult.class, ResultRecord.class}))),
            @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content(mediaType = JSON, schema = @Schema(implementation = ResultRecord.class))),
            @ApiResponse(responseCode = "409", description = "Stale revision or rearm while a run is active", content = @Content(mediaType = JSON, schema = @Schema(implementation = ResultRecord.class))),
            @ApiResponse(responseCode = "413", description = "Request body too large", content = @Content(mediaType = JSON, schema = @Schema(implementation = ResultRecord.class))),
            @ApiResponse(responseCode = "503", description = "Tenant or persistence unavailable", content = @Content(mediaType = JSON, schema = @Schema(implementation = ResultRecord.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected failure", content = @Content(mediaType = JSON, schema = @Schema(implementation = ResultRecord.class)))
    })
    @EgonGatewayPolicy(exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @PutMapping(path = "/{id}/optimization-policy", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResultRecord<DdcAiOptimizationPolicyResult>> replaceOptimizationPolicy(
            @Parameter(description = "Resource ID, positive decimal string", required = true,
                    schema = @Schema(type = "string", pattern = LONG_ID_PATTERN))
            @PathVariable("id") Long id,
            @Valid @RequestBody DdcAiOptimizationPolicyCommand command) {
        return noStore(ResultRecord.success(optimizationService.replacePolicy(new DdcAiIdQuery(id), command)));
    }

    @Operation(operationId = "triggerAiOptimizationRun", summary = "手动触发一次优化运行",
            security = @SecurityRequirement(name = "tianshuBearerAuth"))
    @Parameters({
            @Parameter(name = TENANT_HEADER, in = ParameterIn.HEADER, required = true),
            @Parameter(name = IDEMPOTENCY_HEADER, in = ParameterIn.HEADER, required = true,
                    schema = @Schema(type = "string", pattern = IDEMPOTENCY_PATTERN, minLength = 16, maxLength = 64))
    })
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Optimization run accepted",
                    headers = {
                            @io.swagger.v3.oas.annotations.headers.Header(name = "Location", schema = @Schema(type = "string")),
                            @io.swagger.v3.oas.annotations.headers.Header(name = "Cache-Control", schema = @Schema(type = "string", example = "no-store"))
                    }),
            @ApiResponse(responseCode = "400", description = "Invalid command or resource not published", content = @Content(mediaType = JSON, schema = @Schema(implementation = ResultRecord.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(mediaType = JSON, schema = @Schema(oneOf = {DdcAdminSecurityErrorResult.class, ResultRecord.class}))),
            @ApiResponse(responseCode = "403", description = "Scope permission denied", content = @Content(mediaType = JSON, schema = @Schema(oneOf = {DdcAdminSecurityErrorResult.class, ResultRecord.class}))),
            @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content(mediaType = JSON, schema = @Schema(implementation = ResultRecord.class))),
            @ApiResponse(responseCode = "409", description = "Active run or idempotency conflict", content = @Content(mediaType = JSON, schema = @Schema(implementation = ResultRecord.class))),
            @ApiResponse(responseCode = "413", description = "Request body too large", content = @Content(mediaType = JSON, schema = @Schema(implementation = ResultRecord.class))),
            @ApiResponse(responseCode = "503", description = "Optimization channel disabled or persistence unavailable", content = @Content(mediaType = JSON, schema = @Schema(implementation = ResultRecord.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected failure", content = @Content(mediaType = JSON, schema = @Schema(implementation = ResultRecord.class)))
    })
    @EgonGatewayPolicy(exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @PostMapping(path = "/{id}/optimization-runs", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResultRecord<DdcAiOptimizationRunResult>> triggerOptimizationRun(
            @Parameter(description = "Resource ID, positive decimal string", required = true,
                    schema = @Schema(type = "string", pattern = LONG_ID_PATTERN))
            @PathVariable("id") Long id,
            @org.springframework.web.bind.annotation.RequestHeader(IDEMPOTENCY_HEADER)
            @NotBlank @Size(min = 16, max = 64) @Pattern(regexp = IDEMPOTENCY_PATTERN) String requestId,
            @Valid @RequestBody DdcAiOptimizationTriggerCommand command) {
        DdcAiOptimizationRunResult run = optimizationService.trigger(new DdcAiIdQuery(id), requestId, command);
        return ResponseEntity.accepted()
                .location(runLocation(id, run.getId()))
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(ResultRecord.success(run));
    }

    @Operation(operationId = "pageAiOptimizationRuns", summary = "分页浏览优化运行",
            security = @SecurityRequirement(name = "tianshuBearerAuth"))
    @Parameters({
            @Parameter(name = TENANT_HEADER, in = ParameterIn.HEADER, required = true)
    })
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paged optimization runs, newest first",
                    headers = @io.swagger.v3.oas.annotations.headers.Header(name = "Cache-Control", schema = @Schema(type = "string", example = "no-store"))),
            @ApiResponse(responseCode = "400", description = "Invalid query", content = @Content(mediaType = JSON, schema = @Schema(implementation = ResultRecord.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(mediaType = JSON, schema = @Schema(oneOf = {DdcAdminSecurityErrorResult.class, ResultRecord.class}))),
            @ApiResponse(responseCode = "403", description = "Scope permission denied", content = @Content(mediaType = JSON, schema = @Schema(oneOf = {DdcAdminSecurityErrorResult.class, ResultRecord.class}))),
            @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content(mediaType = JSON, schema = @Schema(implementation = ResultRecord.class))),
            @ApiResponse(responseCode = "503", description = "Tenant or persistence unavailable", content = @Content(mediaType = JSON, schema = @Schema(implementation = ResultRecord.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected failure", content = @Content(mediaType = JSON, schema = @Schema(implementation = ResultRecord.class)))
    })
    @EgonGatewayPolicy(exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @GetMapping("/{id}/optimization-runs")
    public ResponseEntity<PageResultRecord<DdcAiOptimizationRunResult>> pageOptimizationRuns(
            @Parameter(description = "Resource ID, positive decimal string", required = true,
                    schema = @Schema(type = "string", pattern = LONG_ID_PATTERN))
            @PathVariable("id") Long id,
            @Parameter(description = "Optional run state filter", schema = @Schema(type = "string",
                    allowableValues = {"PENDING", "RUNNING", "SUCCEEDED", "FAILED", "SKIPPED"}))
            @RequestParam(value = "state", required = false) DdcAiRunStatePersistenceEnum state,
            @Parameter(description = "One-based page number", schema = @Schema(type = "integer", minimum = "1"))
            @RequestParam(value = "pageNo", defaultValue = "1") @Min(1) int pageNo,
            @Parameter(description = "Page size, 1..100", schema = @Schema(type = "integer", minimum = "1", maximum = "100"))
            @RequestParam(value = "pageSize", defaultValue = "20") @Min(1) @Max(100) int pageSize) {
        DdcAiOptimizationRunPageQuery query = DdcAiOptimizationRunPageQuery.builder().resourceId(id).state(state)
                .pageNo(pageNo).pageSize(pageSize).build();
        return noStore(DdcAdminPageSupport.result(optimizationService.pageRuns(query)));
    }

    @Operation(operationId = "getAiOptimizationRun", summary = "读取单个优化运行",
            security = @SecurityRequirement(name = "tianshuBearerAuth"))
    @Parameters({
            @Parameter(name = TENANT_HEADER, in = ParameterIn.HEADER, required = true)
    })
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Optimization run",
                    headers = @io.swagger.v3.oas.annotations.headers.Header(name = "Cache-Control", schema = @Schema(type = "string", example = "no-store"))),
            @ApiResponse(responseCode = "400", description = "Invalid resource or run id", content = @Content(mediaType = JSON, schema = @Schema(implementation = ResultRecord.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(mediaType = JSON, schema = @Schema(oneOf = {DdcAdminSecurityErrorResult.class, ResultRecord.class}))),
            @ApiResponse(responseCode = "403", description = "Scope permission denied", content = @Content(mediaType = JSON, schema = @Schema(oneOf = {DdcAdminSecurityErrorResult.class, ResultRecord.class}))),
            @ApiResponse(responseCode = "404", description = "Resource or run not found", content = @Content(mediaType = JSON, schema = @Schema(implementation = ResultRecord.class))),
            @ApiResponse(responseCode = "503", description = "Tenant or persistence unavailable", content = @Content(mediaType = JSON, schema = @Schema(implementation = ResultRecord.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected failure", content = @Content(mediaType = JSON, schema = @Schema(implementation = ResultRecord.class)))
    })
    @EgonGatewayPolicy(exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @GetMapping("/{id}/optimization-runs/{runId}")
    public ResponseEntity<ResultRecord<DdcAiOptimizationRunResult>> getOptimizationRun(
            @Parameter(description = "Resource ID, positive decimal string", required = true,
                    schema = @Schema(type = "string", pattern = LONG_ID_PATTERN))
            @PathVariable("id") Long id,
            @Parameter(description = "Run ID, positive decimal string", required = true,
                    schema = @Schema(type = "string", pattern = LONG_ID_PATTERN))
            @PathVariable("runId") Long runId) {
        return noStore(ResultRecord.success(optimizationService.getRun(new DdcAiIdQuery(id), runId)));
    }

    @Operation(operationId = "pageAiUsageFeedback", summary = "分页浏览使用反馈",
            security = @SecurityRequirement(name = "tianshuBearerAuth"))
    @Parameters({
            @Parameter(name = TENANT_HEADER, in = ParameterIn.HEADER, required = true)
    })
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paged usage feedback, newest first",
                    headers = @io.swagger.v3.oas.annotations.headers.Header(name = "Cache-Control", schema = @Schema(type = "string", example = "no-store"))),
            @ApiResponse(responseCode = "400", description = "Invalid query", content = @Content(mediaType = JSON, schema = @Schema(implementation = ResultRecord.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(mediaType = JSON, schema = @Schema(oneOf = {DdcAdminSecurityErrorResult.class, ResultRecord.class}))),
            @ApiResponse(responseCode = "403", description = "Scope permission denied", content = @Content(mediaType = JSON, schema = @Schema(oneOf = {DdcAdminSecurityErrorResult.class, ResultRecord.class}))),
            @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content(mediaType = JSON, schema = @Schema(implementation = ResultRecord.class))),
            @ApiResponse(responseCode = "503", description = "Tenant or persistence unavailable", content = @Content(mediaType = JSON, schema = @Schema(implementation = ResultRecord.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected failure", content = @Content(mediaType = JSON, schema = @Schema(implementation = ResultRecord.class)))
    })
    @EgonGatewayPolicy(exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @GetMapping("/{id}/usage-feedback")
    public ResponseEntity<PageResultRecord<DdcAiUsageFeedbackResult>> pageUsageFeedback(
            @Parameter(description = "Resource ID, positive decimal string", required = true,
                    schema = @Schema(type = "string", pattern = LONG_ID_PATTERN))
            @PathVariable("id") Long id,
            @Parameter(description = "Optional exact version number", schema = @Schema(type = "string", pattern = LONG_ID_PATTERN))
            @RequestParam(value = "versionNo", required = false) @Positive Long versionNo,
            @Parameter(description = "Optional outcome filter", schema = @Schema(type = "string",
                    allowableValues = {"SUCCESS", "FAILURE", "UNKNOWN"}))
            @RequestParam(value = "outcome", required = false) DdcAiOutcomePersistenceEnum outcome,
            @Parameter(description = "Optional score source filter", schema = @Schema(type = "string",
                    allowableValues = {"EXPLICIT", "JUDGE", "NONE"}))
            @RequestParam(value = "scoreSource", required = false) @Pattern(regexp = "^(EXPLICIT|JUDGE|NONE)$")
            String scoreSource,
            @Parameter(description = "Only samples whose effective score is at most this value",
                    schema = @Schema(type = "integer", minimum = "0", maximum = "100"))
            @RequestParam(value = "maxScore", required = false) @Min(0) @Max(100) Integer maxScore,
            @Parameter(description = "One-based page number", schema = @Schema(type = "integer", minimum = "1"))
            @RequestParam(value = "pageNo", defaultValue = "1") @Min(1) int pageNo,
            @Parameter(description = "Page size, 1..100", schema = @Schema(type = "integer", minimum = "1", maximum = "100"))
            @RequestParam(value = "pageSize", defaultValue = "20") @Min(1) @Max(100) int pageSize) {
        DdcAiUsageFeedbackPageQuery query = DdcAiUsageFeedbackPageQuery.builder().resourceId(id).versionNo(versionNo)
                .outcome(outcome).scoreSource(scoreSource).maxScore(maxScore).pageNo(pageNo).pageSize(pageSize)
                .build();
        return noStore(DdcAdminPageSupport.result(feedbackService.pageFeedback(query)));
    }

    private ResponseEntity<ResultRecord<DdcAiPublicationResult>> createdVersion(
            DdcAiPublicationResult result, Long resourceId) {
        return ResponseEntity.created(versionLocation(resourceId, result.getTargetVersionNo()))
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(ResultRecord.success(result));
    }

    private URI resourceLocation(String resourceId) {
        return URI.create(BASE_PATH + "/" + resourceId);
    }

    private URI runLocation(Long resourceId, Long runId) {
        return URI.create(BASE_PATH + "/" + resourceId + "/optimization-runs/" + runId);
    }

    private URI versionLocation(Long resourceId, Long versionNo) {
        return URI.create(BASE_PATH + "/" + resourceId + "/versions/" + versionNo);
    }

    private <T> ResponseEntity<T> noStore(T body) {
        return ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL, "no-store").body(body);
    }
}
