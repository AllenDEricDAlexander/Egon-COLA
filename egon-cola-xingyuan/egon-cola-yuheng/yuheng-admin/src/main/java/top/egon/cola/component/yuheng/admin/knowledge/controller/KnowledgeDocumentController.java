package top.egon.cola.component.yuheng.admin.knowledge.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.io.IOException;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import top.egon.cola.component.yuheng.admin.knowledge.domain.dto.KnowledgePageQueryDTO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.dto.KnowledgeReindexCommandDTO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.dto.KnowledgeUploadCommandDTO;
import top.egon.cola.component.yuheng.admin.knowledge.converter.KnowledgeUploadReceiptConverter;
import top.egon.cola.component.yuheng.admin.knowledge.domain.vo.KnowledgeDocumentRevisionVO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.vo.KnowledgeDocumentVO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.vo.KnowledgeJobVO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.vo.KnowledgePageVO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.vo.KnowledgeUploadReceiptVO;
import top.egon.cola.component.yuheng.admin.knowledge.service.KnowledgeService;
import top.egon.cola.component.yuheng.admin.shared.domain.AdminActor;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.component.yuheng.openapi.annotation.EgonApiCatalog;
import top.egon.cola.component.yuheng.openapi.annotation.EgonGatewayPolicy;

/**
 * 中文说明：{@code KnowledgeDocumentController} 是原业务 Spec §9.2.14–§9.2.18（API-014–018）的接口控制器，
 * 只负责把既有 HTTP 路径、operationId、状态码与裸 JSON shape 交付给 {@link KnowledgeService}：
 * 文档分页与修订读取返回已提交投影，上传与重投索引作业返回 {@code 202 Accepted} 并指向该作业的轮询地址，
 * 删除返回 {@code 204 No Content} 且不带任何响应体。
 * 身份由已认证主体解析为 {@link AdminActor} 作为业务合同的第一个入参，租户不进入任何参数；
 * READER/EDITOR 角色判定、原件与 revision 与作业的同事务写入、内容 hash 与字节上限、媒体类型复核、
 * 幂等意图复用与活动版本 CAS 全部在业务合同与摄取侧完成，本类不含权限分支、不读取文件内容、不访问仓储。
 * 任何响应都不投影 {@code rawBytes}、提取正文、提示词、向量或服务器路径，日志亦只记稳定 id 与结果码。
 * English summary: {@code KnowledgeDocumentController} is the interface controller of §9.2.14–§9.2.18 (API-014–018) of the
 * primary business Spec and only delivers the original paths, operationIds, status codes and bare JSON shapes through
 * {@link KnowledgeService}: the document page and the revision read answer with committed projections, the upload and the
 * reingest request answer {@code 202 Accepted} pointing at that job's polling address, and the delete answers
 * {@code 204 No Content} with no body at all. The identity is resolved from the authenticated principal into an
 * {@link AdminActor} passed as the first business argument and tenancy never becomes a parameter; the READER/EDITOR
 * decision, the one-transaction write of document plus revision plus job, the content-hash and byte bounds, the media-type
 * re-check, idempotency reuse and the active-revision CAS all belong to the business contract and the ingestion side, so
 * this class holds no permission branch, reads no file content and touches no repository. No response projects
 * {@code rawBytes}, extracted text, prompts, vectors or server paths, and the logs keep only stable ids and result codes.
 *
 * 用法 / Usage: 通过 Spring MVC 暴露的 {@code GET|POST /api/v1/yuheng/admin/knowledge-bases/{kbId}/documents}、
 * {@code GET .../documents/{documentId}/revisions/{revisionId}}、{@code DELETE .../documents/{documentId}} 与
 * {@code POST .../documents/{documentId}/reindex-jobs} 调用；读需 {@code yuheng:knowledge:read}，
 * 写需 {@code yuheng:knowledge:write}，角色（READER/EDITOR）由知识库成员判定在业务合同内完成。
 * 分页参数在本类补齐 {@code page=1, size=20} 默认值，因为载体字段是原始 {@code int}；
 * 其余约束由被调用的业务合同统一复核，本类不重复声明，也不做检索、不做摄取、不推进任何作业状态。
 * / Invoke it through the exposed entry points; reads require {@code yuheng:knowledge:read} and writes
 * {@code yuheng:knowledge:write}, while the READER/EDITOR role itself comes from the knowledge base membership decided
 * inside the business contract. Paging defaults are filled in here because the carrier fields are primitive {@code int}s,
 * every other constraint is checked once by the business contract, and this class retrieves nothing, ingests nothing and
 * advances no job state.
 */
@Slf4j
@Validated
@RestController("knowledgeDocumentController")
@RequestMapping("/api/v1/yuheng/admin")
@PreAuthorize("hasAnyAuthority('CAP_yuheng:knowledge:read','CAP_*')")
@Tag(name = "yuheng-admin")
@EgonApiCatalog(
        businessDomainCode = "xingyuan",
        businessDomainName = "平台治理域",
        entityDomainCode = "yuheng-admin",
        entityDomainName = "Gateway Admin 管理实体域",
        interfaceGroupCode = "yuheng-admin")
@RequiredArgsConstructor
public class KnowledgeDocumentController {

    /** 作业轮询绝对路径前缀，仅用于 202 的 {@code Location}，与 API-019 的映射保持一致 / the job polling path prefix used only for the 202 {@code Location}, aligned with the API-019 mapping. */
    private static final String KNOWLEDGE_JOB_PATH = "/api/v1/yuheng/admin/knowledge-jobs/";

    /** 原合同要求的 202 轮询间隔秒数 / the polling interval the contract attaches to a 202 answer. */
    private static final String RETRY_AFTER_SECONDS = "2";

    /**
     * 中文说明：保存 知识库管理业务合同 对应的依赖值；字段类型为 {@code KnowledgeService}，由
     * {@code KnowledgeDocumentController} 在其生命周期内读取或更新。
     * English summary: Holds the dependency represented by the knowledge management business contract; its type is
     * {@code KnowledgeService}, and {@code KnowledgeDocumentController} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code KnowledgeDocumentController} 的公开入口使用；/ Access it through the public
     * entry points of {@code KnowledgeDocumentController}.
     */
    @Qualifier("knowledgeServiceImpl")
    private final KnowledgeService knowledgeService;

    @Qualifier("knowledgeUploadReceiptConverter")
    private final KnowledgeUploadReceiptConverter knowledgeUploadReceiptConverter;

    /**
     * 中文说明：执行 listKnowledgeDocuments 操作（API-014）；READER 及以上可见，仅 {@code page}、{@code size}
     * 两个查询参数，返回活跃文档的 {@code items/page/size/total}，每行只有稳定身份、文件名、活动修订指针、
     * 最近作业 id、revision、派生显示态 {@code READY|PROCESSING|FAILED} 与安全时间戳，绝不返回 {@code rawBytes}
     * 或提取正文；知识库不可见为 403，空页返回 {@code []}。
     * English summary: Executes the listKnowledgeDocuments operation (API-014) at READER or above with only the
     * {@code page} and {@code size} query parameters, answering the active documents as {@code items/page/size/total} where
     * each row carries the stable identity, file name, active revision pointer, latest job id, revision, the derived
     * display state {@code READY|PROCESSING|FAILED} and the safe timestamps, never {@code rawBytes} nor the extracted text;
     * an invisible knowledge base is 403 and an empty page answers {@code []}.
     *
     * 用法 / Usage: 调用方式 / Usage:
     * {@code knowledgeDocumentController.listKnowledgeDocuments(kbId, actor, page, size)}。
     * @param kbId 参数 知识库十进制字符串 id；parameter decimal-string knowledge base id.
     * @param actor 参数 已验证的管理身份；parameter the verified management actor.
     * @param page 参数 页码，默认 1；parameter one-based page number, defaulting to one.
     * @param size 参数 页大小，默认 20；parameter page size, defaulting to 20.
     * @return 返回 文档分页投影；returns the paged document projection.
     */
    @Operation(operationId = "listKnowledgeDocuments")
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @GetMapping("/knowledge-bases/{kbId}/documents")
    public KnowledgePageVO<KnowledgeDocumentVO> listKnowledgeDocuments(
            @NotBlank
            @Pattern(regexp = "^[1-9][0-9]{0,19}$")
            @PathVariable String kbId,
            AdminActor actor,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return knowledgeService.listDocuments(
                actor,
                kbId,
                pageQuery(page, size)
        );
    }

    /**
     * 中文说明：执行 uploadKnowledgeDocument 操作（API-015）；EDITOR 及以上方可为，202 只表示已持久、不表示已索引，
     * {@code Location} 指向回执 {@code jobId} 的 API-019 轮询地址并带 {@code Retry-After: 2}。
     * 业务合同在同一事务内写入文档行、{@code STAGING} 修订（原件字节、字节数、内容 hash）与
     * {@code DOCUMENT_INGEST} 作业行，失败即整体回滚且不留孤立作业，解析与本地嵌入都在事务与锁之外由 worker 执行，
     * 因此在作业成功前旧活动修订始终可检索。文件过大为 413、媒体类型未支持为 415、字节与字段不合法为 422、
     * 嵌入别名不可用为 503；{@code Idempotency-Key} 必带；multipart仅包含file及可选documentId/expectedRevision，
     * 202回执不暴露文档详情。
     * English summary: Executes the uploadKnowledgeDocument operation (API-015), EDITOR or above, where a 202 only means
     * durably persisted and not yet indexed, with a {@code Location} of the API-019 polling address of the receipt's
     * {@code jobId} plus {@code Retry-After: 2}. The business contract writes the document row, the {@code STAGING}
     * revision (raw bytes, byte count, content hash) and the {@code DOCUMENT_INGEST} job row inside one transaction,
     * rolling back entirely with no orphan job on failure, while parsing and local embedding run outside the transaction and
     * the locks on the worker side — so the previous active revision stays searchable until the job succeeds. An oversized
     * file is 413, an unsupported media type 415, invalid bytes or fields 422 and an unavailable embedding alias 503;
     * {@code Idempotency-Key} is mandatory per the contract and re-checked by the business layer.
     *
     * 用法 / Usage: 调用方式 / Usage:
     * {@code knowledgeDocumentController.uploadKnowledgeDocument(kbId, file, documentId, expectedRevision, actor, idempotencyKey)}。
     * @param kbId 参数 知识库十进制字符串 id；parameter decimal-string knowledge base id.
     * @param file 参数 原始 multipart 文件；parameter the original multipart file.
     * @param documentId 参数 可选的已有文档 ID；parameter the optional existing document id.
     * @param expectedRevision 参数 更新现有文档时必须提供；parameter the required current revision when updating.
     * @param actor 参数 已验证的管理身份；parameter the verified management actor.
     * @param idempotencyKey 参数 必填 {@code Idempotency-Key} 请求头；parameter the required {@code Idempotency-Key} header.
     * @return 返回 documentId/revisionId/jobId/status 202 回执；returns the four-field 202 receipt.
     */
    @Operation(operationId = "uploadKnowledgeDocument")
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL,
            idempotency = EgonGatewayPolicy.Idempotency.TRUE)
    @PostMapping(value = "/knowledge-bases/{kbId}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('CAP_yuheng:knowledge:write','CAP_*')")
    public ResponseEntity<KnowledgeUploadReceiptVO> uploadKnowledgeDocument(
            @NotBlank
            @Pattern(regexp = "^[1-9][0-9]{0,19}$")
            @PathVariable String kbId,
            @Parameter(description = "原始文件，1–20MiB；支持类型由文件名扩展名与内容共同验证", required = true)
            @RequestPart("file") MultipartFile file,
            @Parameter(description = "追加已有文档版本时提供；创建文档时省略")
            @RequestPart(value = "documentId", required = false) String documentId,
            @Parameter(description = "十进制正整数；提供documentId时必须匹配文档当前revision",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(type = "integer", format = "int64"))
            @Pattern(regexp = "^[1-9][0-9]{0,18}$")
            @RequestPart(value = "expectedRevision", required = false) String expectedRevision,
            AdminActor actor,
            @RequestHeader(value = "Idempotency-Key",
                    required = true) String idempotencyKey) throws IOException {
        KnowledgeUploadCommandDTO command = new KnowledgeUploadCommandDTO()
                .setFileName(file.getOriginalFilename())
                .setMediaType(file.getContentType())
                .setContent(file.getBytes())
                .setDocumentId(documentId)
                .setExpectedRevision(parseExpectedRevision(expectedRevision));
        KnowledgeUploadReceiptVO view = knowledgeUploadReceiptConverter.toTarget(knowledgeService.uploadDocument(
                actor,
                kbId,
                command,
                idempotencyKey
        ));
        log.debug("YUHENG_KNOWLEDGE_DOCUMENT_UPLOADED kbId={} documentId={} jobId={} status={}",
                kbId,
                view.getDocumentId(),
                view.getJobId(),
                view.getStatus()
        );
        return accepted(view.getJobId(), view);
    }

    private static Long parseExpectedRevision(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException invalid) {
            throw new CommonException(
                    422,
                    "YUHENG_ADMIN_VALIDATION_FAILED",
                    "expectedRevision must be a positive 64-bit integer");
        }
    }

    /**
     * 中文说明：执行 getKnowledgeDocumentRevision 操作（API-016）；READER 及以上可见，
     * 修订必须属于该文档且该文档属于该知识库，任一环节不成立都按不存在处理并 404，不泄漏存在性；
     * 返回体只有文件名、mediaType、字节数、内容 hash、{@code STAGING|READY|FAILED} 状态与创建时刻，
     * 绝不返回原始字节、服务器路径，也不泄漏任务内部状态。
     * English summary: Executes the getKnowledgeDocumentRevision operation (API-016) at READER or above, where the revision
     * must belong to that document and that document to this knowledge base and any broken link in that chain is treated as
     * absent and answered 404 without leaking existence. The body carries only the file name, mediaType, byte count,
     * content hash, the {@code STAGING|READY|FAILED} state and the creation instant — never the raw bytes, never a server
     * path, and never the internal state of a task.
     *
     * 用法 / Usage: 调用方式 / Usage:
     * {@code knowledgeDocumentController.getKnowledgeDocumentRevision(kbId, documentId, revisionId, actor)}。
     * @param kbId 参数 知识库十进制字符串 id；parameter decimal-string knowledge base id.
     * @param documentId 参数 文档十进制字符串 id；parameter decimal-string document id.
     * @param revisionId 参数 修订十进制字符串 id；parameter decimal-string revision id.
     * @param actor 参数 已验证的管理身份；parameter the verified management actor.
     * @return 返回 修订投影；returns the revision projection.
     */
    @Operation(operationId = "getKnowledgeDocumentRevision")
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @GetMapping("/knowledge-bases/{kbId}/documents/{documentId}/revisions/{revisionId}")
    public KnowledgeDocumentRevisionVO getKnowledgeDocumentRevision(
            @NotBlank
            @Pattern(regexp = "^[1-9][0-9]{0,19}$")
            @PathVariable String kbId,
            @NotBlank
            @Pattern(regexp = "^[1-9][0-9]{0,19}$")
            @PathVariable String documentId,
            @NotBlank
            @Pattern(regexp = "^[1-9][0-9]{0,19}$")
            @PathVariable String revisionId,
            AdminActor actor) {
        return knowledgeService.getRevision(
                actor,
                kbId,
                documentId,
                revisionId
        );
    }

    /**
     * 中文说明：执行 deleteKnowledgeDocument 操作（API-017）；EDITOR 及以上方可为，
     * {@code expectedRevision} 按原合同位于 Query 且必填非负（缺失或小于 1 为 422，避免无条件删除），
     * 成功以 204 No Content 返回且响应不含任何 body，OAS 的 204 亦不得声明 content。
     * 语义是逻辑删除已提交：历史修订与作业保留可审计、不物理删除原件，新检索与依赖的 Wiki 立即失效并取消未开始的作业；
     * 重复删除仍返回 204，版本不匹配为 409 并携带库中现值，跨知识库或非本租户行为 404。
     * English summary: Executes the deleteKnowledgeDocument operation (API-017), EDITOR or above, whose
     * {@code expectedRevision} stays in the query string as the contract requires and is mandatory positive (absent or
     * below one is 422 so no unconditional delete is possible), answering 204 No Content with an empty body where even the
     * OAS 204 response declares no content. The meaning is that the logical delete committed: historical revisions and jobs
     * stay auditable, no raw file is physically removed, new retrieval and dependent wikis are invalidated immediately and
     * not-yet-started jobs are cancelled; a repeated delete still answers 204, a revision mismatch is 409 carrying the
     * stored value, and a cross-knowledge-base or foreign-tenant row is 404.
     *
     * 用法 / Usage: 调用方式 / Usage:
     * {@code knowledgeDocumentController.deleteKnowledgeDocument(kbId, documentId, actor, expectedRevision)}。
     * @param kbId 参数 知识库十进制字符串 id；parameter decimal-string knowledge base id.
     * @param documentId 参数 文档十进制字符串 id；parameter decimal-string document id.
     * @param actor 参数 已验证的管理身份；parameter the verified management actor.
     * @param expectedRevision 参数 客户端观察到的文档 revision，必填且 ≥1；parameter the client-observed document
     *                         revision, mandatory and at least one.
     * @return 返回 无内容的 204 受理结果；returns the empty 204 outcome.
     */
    @Operation(operationId = "deleteKnowledgeDocument")
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @DeleteMapping("/knowledge-bases/{kbId}/documents/{documentId}")
    @PreAuthorize("hasAnyAuthority('CAP_yuheng:knowledge:write','CAP_*')")
    public ResponseEntity<Void> deleteKnowledgeDocument(
            @NotBlank
            @Pattern(regexp = "^[1-9][0-9]{0,19}$")
            @PathVariable String kbId,
            @NotBlank
            @Pattern(regexp = "^[1-9][0-9]{0,19}$")
            @PathVariable String documentId,
            AdminActor actor,
            @NotNull
            @Min(1)
            @RequestParam Long expectedRevision) {
        KnowledgeDocumentVO view = knowledgeService.deleteDocument(
                actor,
                kbId,
                documentId,
                expectedRevision
        );
        log.debug("YUHENG_KNOWLEDGE_DOCUMENT_DELETED kbId={} documentId={} revision={}",
                kbId,
                documentId,
                view.getRevision()
        );
        return ResponseEntity.noContent()
                .build();
    }

    /**
     * 中文说明：执行 createKnowledgeReindexJob 操作（API-018）；EDITOR 及以上方可为，
     * 请求体为 {@code {sourceRevisionId, expectedRevision}} 且与 {@code Idempotency-Key} 同时携带（两者都不可省略），
     * 202 返回完整 {@code KnowledgeJobVO}、{@code Location} 指向该作业的 API-019 地址并带 {@code Retry-After: 2}。
     * 作业类型固定 {@code DOCUMENT_INGEST}，创建作业与推进 {@code latestJobId} 同事务提交，摄取本身绝不同步执行；
     * 同 key 同摘要复用既有作业返回同一 id，摘要分歧为 409 {@code KNOWLEDGE_IDEMPOTENCY_CONFLICT}，
     * 版本不匹配为 409 {@code KNOWLEDGE_REVISION_CONFLICT}，跨知识库或不存在为 404。
     * English summary: Executes the createKnowledgeReindexJob operation (API-018), EDITOR or above, whose body is
     * {@code {sourceRevisionId, expectedRevision}} and which carries {@code Idempotency-Key} as well (neither may be
     * omitted), answering 202 with the full {@code KnowledgeJobVO}, a {@code Location} of that job's API-019 address and
     * {@code Retry-After: 2}. The job type is fixed to {@code DOCUMENT_INGEST}, job creation and the {@code latestJobId}
     * advance commit in one transaction while the ingestion itself never runs synchronously; the same key with an equal
     * digest reuses the existing job and returns the same id, a diverged digest is a 409
     * {@code KNOWLEDGE_IDEMPOTENCY_CONFLICT}, a revision mismatch a 409 {@code KNOWLEDGE_REVISION_CONFLICT} and a
     * cross-knowledge-base or absent row a 404.
     *
     * 用法 / Usage: 调用方式 / Usage:
     * {@code knowledgeDocumentController.createKnowledgeReindexJob(kbId, documentId, command, actor, idempotencyKey)}。
     * @param kbId 参数 知识库十进制字符串 id；parameter decimal-string knowledge base id.
     * @param documentId 参数 文档十进制字符串 id；parameter decimal-string document id.
     * @param command 参数 重投命令；parameter the reindex command.
     * @param actor 参数 已验证的管理身份；parameter the verified management actor.
     * @param idempotencyKey 参数 可选 {@code Idempotency-Key} 请求头，缺省与复核由业务合同决定；parameter the
     *                       optional {@code Idempotency-Key} header whose presence rules stay with the business contract.
     * @return 返回 已提交作业投影与 202 受理状态；returns the committed job projection in a 202 acceptance.
     */
    @Operation(operationId = "createKnowledgeReindexJob")
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL,
            idempotency = EgonGatewayPolicy.Idempotency.TRUE)
    @PostMapping("/knowledge-bases/{kbId}/documents/{documentId}/reindex-jobs")
    @PreAuthorize("hasAnyAuthority('CAP_yuheng:knowledge:write','CAP_*')")
    public ResponseEntity<KnowledgeJobVO> createKnowledgeReindexJob(
            @NotBlank
            @Pattern(regexp = "^[1-9][0-9]{0,19}$")
            @PathVariable String kbId,
            @NotBlank
            @Pattern(regexp = "^[1-9][0-9]{0,19}$")
            @PathVariable String documentId,
            @Valid @RequestBody KnowledgeReindexCommandDTO command,
            AdminActor actor,
            @RequestHeader(value = "Idempotency-Key",
                    required = false) String idempotencyKey) {
        KnowledgeJobVO view = knowledgeService.createReindexJob(
                actor,
                kbId,
                documentId,
                command,
                idempotencyKey
        );
        log.debug("YUHENG_KNOWLEDGE_REINDEX_JOB_CREATED jobId={} type={} status={}",
                view.getId(),
                view.getType(),
                view.getStatus()
        );
        return accepted(view.getId(), view);
    }

    /**
     * 中文说明：按原合同把异步受理结果状态化为 202，并把 {@code Location} 指向该作业的轮询地址、
     * 附带固定 {@code Retry-After: 2}；本方法不判断状态是否可以推进，也不改写任何业务字段。
     * English summary: Applies the contract's 202 acceptance outcome, pointing the {@code Location} at that job's polling
     * address and attaching the fixed {@code Retry-After: 2}; it decides nothing about whether a state may advance and
     * rewrites no business field.
     * @param jobId 参数 受理的作业 id；parameter the accepted job id.
     * @param view 参数 已提交投影；parameter the committed projection.
     * @param <T>   返回体投影类型 / the projected response body type.
     * @return 返回 带 202、Location 与 Retry-After 的受理响应；returns the acceptance carrying 202, Location and Retry-After.
     */
    private static <T> ResponseEntity<T> accepted(
            String jobId,
            T view) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .location(jobLocation(jobId))
                .header(HttpHeaders.RETRY_AFTER, RETRY_AFTER_SECONDS)
                .body(view);
    }

    /**
     * 中文说明：把已补齐默认值的分页参数装配成查询载体；本资源只绑定原合同声明过的 {@code page} 与 {@code size}，
     * 载体上的 {@code status} 与 {@code search} 刻意不接入，避免给文档列表增加未声明的过滤行为。
     * English summary: Assembles the query carrier from the defaulted paging parameters, binding only the
     * contract-declared {@code page} and {@code size} for this resource and deliberately leaving the carrier's
     * {@code status} and {@code search} unwired so no undeclared filter reaches the document page.
     * @param page 参数 页码；parameter the one-based page number.
     * @param size 参数 页大小；parameter the page size.
     * @return 返回 分页查询载体；returns the page query carrier.
     */
    private static KnowledgePageQueryDTO pageQuery(
            int page,
            int size) {
        return KnowledgePageQueryDTO.builder()
                .page(page)
                .size(size)
                .build();
    }

    /**
     * 中文说明：拼装 API-019 {@code GET /api/v1/yuheng/admin/knowledge-jobs/{jobId}} 的绝对地址，
     * 只取当前部署 contextPath 与合同路径，不猜测网关前缀；作业 id 已是十进制正整数字符串，无需再转义。
     * English summary: Builds the absolute address of API-019
     * {@code GET /api/v1/yuheng/admin/knowledge-jobs/{jobId}} from the current deployment context path plus the contract
     * path, never guessing a gateway prefix; the job id is already a decimal positive-integer string so no escaping is
     * needed.
     * @param jobId 参数 已受理的作业 id；parameter the accepted job id.
     * @return 返回 该作业的可寻址位置；returns the addressable location of that job.
     */
    private static URI jobLocation(String jobId) {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(KNOWLEDGE_JOB_PATH)
                .path(jobId)
                .build()
                .toUri();
    }
}
