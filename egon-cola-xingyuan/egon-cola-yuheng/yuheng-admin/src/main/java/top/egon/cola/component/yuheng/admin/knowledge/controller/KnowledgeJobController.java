package top.egon.cola.component.yuheng.admin.knowledge.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import top.egon.cola.component.yuheng.admin.knowledge.domain.dto.KnowledgePageQueryDTO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.dto.KnowledgeRetryCommandDTO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.vo.KnowledgeJobVO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.vo.KnowledgePageVO;
import top.egon.cola.component.yuheng.admin.knowledge.service.KnowledgeJobService;
import top.egon.cola.component.yuheng.admin.shared.domain.AdminActor;
import top.egon.cola.component.yuheng.openapi.annotation.EgonApiCatalog;
import top.egon.cola.component.yuheng.openapi.annotation.EgonGatewayPolicy;

/**
 * 中文说明：{@code KnowledgeJobController} 是原业务 Spec §9.2.19–§9.2.21（API-019–021）的接口控制器，
 * 只负责把既有 HTTP 路径、operationId、状态码与裸 JSON shape 交付给 {@link KnowledgeJobService}：
 * 单个作业读取与分页返回 {@code id/kbId/type/resourceId/status/stage/attempt/errorCode/retryOfJobId/revision}
 * 与安全时间戳（分页为 {@code items/page/size/total}），显式重试返回 {@code 202 Accepted} 并指向后继作业的轮询地址。
 * 身份由已认证主体解析为 {@link AdminActor} 作为业务合同的第一个入参，租户不进入任何参数；
 * 先解析作业所属知识库再按该库成员关系判权限、租约与 fencing、至多三次尝试与 5s/30s 退避、
 * 幂等意图复用与后继行创建全部在业务合同与 worker 侧完成，本类不含权限分支、不访问仓储、
 * 绝不推进任何作业状态，也不投影载荷原文、提示词、租约持有者、向量或密钥。
 * English summary: {@code KnowledgeJobController} is the interface controller of §9.2.19–§9.2.21 (API-019–021) of the
 * primary business Spec and only delivers the original paths, operationIds, status codes and bare JSON shapes through
 * {@link KnowledgeJobService}: the single job read and the page project
 * {@code id/kbId/type/resourceId/status/stage/attempt/errorCode/retryOfJobId/revision} plus the safe timestamps (the page as
 * {@code items/page/size/total}), and an explicit retry answers {@code 202 Accepted} pointing at the successor job's polling
 * address. The identity is resolved from the authenticated principal into an {@link AdminActor} passed as the first business
 * argument and tenancy never becomes a parameter; resolving the owning knowledge base before the membership decision, the
 * lease and its fencing, the three attempts with 5s/30s backoff, idempotency reuse and successor-row creation all belong to
 * the business contract and the worker side, so this class holds no permission branch, touches no repository, never advances
 * a job state, and projects no payload body, prompt, lease owner, vector or secret.
 *
 * 用法 / Usage: 通过 Spring MVC 暴露的 {@code GET /api/v1/yuheng/admin/knowledge-jobs/{jobId}}、
 * {@code GET /api/v1/yuheng/admin/knowledge-bases/{kbId}/jobs} 与
 * {@code POST /api/v1/yuheng/admin/knowledge-jobs/{jobId}/retries} 调用；读需 {@code yuheng:knowledge:read}，
 * 重试需 {@code yuheng:knowledge:write}，READER/EDITOR 角色由知识库成员判定在业务合同内完成。
 * 分页参数在本类补齐 {@code page=1, size=20} 默认值（载体字段是原始 {@code int}），{@code status} 保持可选并交
 * 查询载体的值域复核；本类不做健康探测、不做检索、不生成作业。
 * / Invoke it through the exposed entry points; reads require {@code yuheng:knowledge:read} and a retry
 * {@code yuheng:knowledge:write}, while the READER/EDITOR role itself comes from the knowledge base membership decided
 * inside the business contract. Paging defaults are filled in here because the carrier fields are primitive {@code int}s
 * and {@code status} stays optional and is re-checked by the query carrier's value domain; this class probes nothing,
 * retrieves nothing and creates no job.
 */
@Slf4j
@Validated
@RestController("knowledgeJobController")
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
public class KnowledgeJobController {

    /** 作业轮询绝对路径前缀，仅用于 202 的 {@code Location}，与 API-019 的映射保持一致 / the job polling path prefix used only for the 202 {@code Location}, aligned with the API-019 mapping. */
    private static final String KNOWLEDGE_JOB_PATH = "/api/v1/yuheng/admin/knowledge-jobs/";

    /** 原合同要求的 202 轮询间隔秒数 / the polling interval the contract attaches to a 202 answer. */
    private static final String RETRY_AFTER_SECONDS = "2";

    /**
     * 中文说明：保存 知识作业业务合同 对应的依赖值；字段类型为 {@code KnowledgeJobService}，由
     * {@code KnowledgeJobController} 在其生命周期内读取或更新。
     * English summary: Holds the dependency represented by the knowledge job business contract; its type is
     * {@code KnowledgeJobService}, and {@code KnowledgeJobController} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code KnowledgeJobController} 的公开入口使用；/ Access it through the public entry
     * points of {@code KnowledgeJobController}.
     */
    @Qualifier("knowledgeJobServiceImpl")
    private final KnowledgeJobService knowledgeJobService;

    /**
     * 中文说明：执行 getKnowledgeJob 操作（API-019）；无请求体，业务合同先解析作业所属知识库再按该库成员关系判权限，
     * 不可见为 403 {@code KNOWLEDGE_FORBIDDEN}，非本租户、已软删或不存在的作业为 404
     * {@code KNOWLEDGE_RESOURCE_NOT_FOUND}（跨知识库不泄漏存在性）；200 只投影安全状态字段与时刻，
     * 不含载荷、幂等键、请求摘要、租约持有者、到期时刻与令牌；读取本身绝不推进任何任务状态。
     * English summary: Executes the getKnowledgeJob operation (API-019) with no body: the business contract first resolves
     * the knowledge base owning the job and then applies that base's membership, so an invisible one is 403
     * {@code KNOWLEDGE_FORBIDDEN} while a foreign-tenant, soft-deleted or absent job is 404
     * {@code KNOWLEDGE_RESOURCE_NOT_FOUND} (a cross-knowledge-base row leaks no existence). The 200 projects only the safe
     * status fields and instants — never the payload, idempotency key, request digest, lease owner, lease expiry or token —
     * and the read itself never advances any task state.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code knowledgeJobController.getKnowledgeJob(jobId, actor)}。
     * @param jobId 参数 作业十进制字符串 id；parameter decimal-string job id.
     * @param actor 参数 已验证的管理身份；parameter the verified management actor.
     * @return 返回 作业投影；returns the job projection.
     */
    @Operation(operationId = "getKnowledgeJob")
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @GetMapping("/knowledge-jobs/{jobId}")
    public KnowledgeJobVO getKnowledgeJob(
            @NotBlank
            @Pattern(regexp = "^[1-9][0-9]{0,19}$")
            @PathVariable String jobId,
            AdminActor actor) {
        return knowledgeJobService.getJob(
                actor,
                jobId
        );
    }

    /**
     * 中文说明：执行 listKnowledgeJobs 操作（API-020）；分页读取该知识库的作业，查询参数只有原合同声明的
     * {@code page}、{@code size} 与可选 {@code status}（值域为作业与修订状态词汇表，非法值 422），
     * 次序固定 {@code createdAt DESC, id DESC}、总数与当页同谓词、空页返回 {@code []} 且 {@code total=0}；
     * 未声明的 {@code search} 参数刻意不绑定，也不参与任何过滤；列表查询绝不推进任何任务状态。
     * English summary: Executes the listKnowledgeJobs operation (API-020), paging one knowledge base's jobs with only the
     * contract-declared {@code page}, {@code size} and optional {@code status} (whose domain is the job and revision state
     * vocabulary, an illegal value being 422), in the fixed {@code createdAt DESC, id DESC} order, the total sharing the page
     * predicate and an empty page answering {@code []} with {@code total = 0}. The undeclared {@code search} parameter stays
     * unbound and filters nothing, and the list query never advances any task state.
     *
     * 用法 / Usage: 调用方式 / Usage:
     * {@code knowledgeJobController.listKnowledgeJobs(kbId, actor, page, size, status)}。
     * @param kbId 参数 知识库十进制字符串 id；parameter decimal-string knowledge base id.
     * @param actor 参数 已验证的管理身份；parameter the verified management actor.
     * @param page 参数 页码，默认 1；parameter one-based page number, defaulting to one.
     * @param size 参数 页大小，默认 20；parameter page size, defaulting to 20.
     * @param status 参数 可选状态过滤，缺省不过滤；parameter the optional status filter, absent meaning unfiltered.
     * @return 返回 作业分页投影；returns the paged job projection.
     */
    @Operation(operationId = "listKnowledgeJobs")
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @GetMapping("/knowledge-bases/{kbId}/jobs")
    public KnowledgePageVO<KnowledgeJobVO> listKnowledgeJobs(
            @NotBlank
            @Pattern(regexp = "^[1-9][0-9]{0,19}$")
            @PathVariable String kbId,
            AdminActor actor,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {
        return knowledgeJobService.listJobs(
                actor,
                kbId,
                pageQuery(page, size, status)
        );
    }

    /**
     * 中文说明：执行 retryKnowledgeJob 操作（API-021）；EDITOR 及以上方可为，
     * 请求体为 {@code {expectedRevision}} 且与 {@code Idempotency-Key} 同时携带（两者都不可省略），
     * 202 返回完整 {@code KnowledgeJobVO}、{@code Location} 指向后继作业的 API-019 地址并带 {@code Retry-After: 2}，
     * 前端必须提示上游可能再次计费。只有 {@code FAILED} 与 {@code STALE} 可建后继行，重试重新冻结有效来源与权限；
     * 旧行的状态、错误码与结果原样保留，绝不被改写或复活；期望 revision 不匹配为 409
     * {@code KNOWLEDGE_REVISION_CONFLICT} 并携带现值，同 key 指向不同原 job 为 409
     * {@code KNOWLEDGE_IDEMPOTENCY_CONFLICT}，同 key 同原 job 返回同一后继作业，非本租户或不存在为 404。
     * English summary: Executes the retryKnowledgeJob operation (API-021), EDITOR or above, whose body is
     * {@code {expectedRevision}} and which carries {@code Idempotency-Key} as well (neither may be omitted), answering 202
     * with the full {@code KnowledgeJobVO}, a {@code Location} of the successor's API-019 address and {@code Retry-After: 2},
     * while the client must surface that the vendor may bill again. Only {@code FAILED} and {@code STALE} may spawn a
     * successor and the retry re-freezes the effective source and authorization; the old row's status, error code and result
     * are preserved untouched, never rewritten nor revived. A mismatching expected revision is 409
     * {@code KNOWLEDGE_REVISION_CONFLICT} carrying the stored value, the same key aimed at a different original job is 409
     * {@code KNOWLEDGE_IDEMPOTENCY_CONFLICT}, the same key on the same original job returns that same successor, and a
     * foreign-tenant or absent row is 404.
     *
     * 用法 / Usage: 调用方式 / Usage:
     * {@code knowledgeJobController.retryKnowledgeJob(jobId, command, actor, idempotencyKey)}。
     * @param jobId 参数 被重试作业的十进制字符串 id；parameter decimal-string id of the job being retried.
     * @param command 参数 重试命令，携带期望 revision；parameter the retry command carrying the expected revision.
     * @param actor 参数 已验证的管理身份；parameter the verified management actor.
     * @param idempotencyKey 参数 可选 {@code Idempotency-Key} 请求头，缺省与复核由业务合同决定；parameter the
     *                       optional {@code Idempotency-Key} header whose presence rules stay with the business contract.
     * @return 返回 后继作业投影与 202 受理状态；returns the successor job projection in a 202 acceptance.
     */
    @Operation(operationId = "retryKnowledgeJob")
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL,
            idempotency = EgonGatewayPolicy.Idempotency.TRUE)
    @PostMapping("/knowledge-jobs/{jobId}/retries")
    @PreAuthorize("hasAnyAuthority('CAP_yuheng:knowledge:write','CAP_*')")
    public ResponseEntity<KnowledgeJobVO> retryKnowledgeJob(
            @NotBlank
            @Pattern(regexp = "^[1-9][0-9]{0,19}$")
            @PathVariable String jobId,
            @Valid @RequestBody KnowledgeRetryCommandDTO command,
            AdminActor actor,
            @RequestHeader(value = "Idempotency-Key",
                    required = false) String idempotencyKey) {
        KnowledgeJobVO view = knowledgeJobService.retryJob(
                actor,
                jobId,
                command,
                idempotencyKey
        );
        log.debug("YUHENG_KNOWLEDGE_JOB_RETRIED retryOfJobId={} jobId={} type={} status={}",
                jobId,
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
     * 中文说明：把已补齐默认值的分页参数装配成查询载体，并按原合同只绑定可选的 {@code status} 过滤；
     * 载体上的 {@code search} 刻意不接入任何未声明的检索行为。
     * English summary: Assembles the query carrier from the defaulted paging parameters and binds only the optional
     * {@code status} filter the contract declares, leaving the carrier's {@code search} unwired so no undeclared search
     * behaviour is reached.
     * @param page 参数 页码；parameter the one-based page number.
     * @param size 参数 页大小；parameter the page size.
     * @param status 参数 可选状态过滤；parameter the optional status filter.
     * @return 返回 分页查询载体；returns the page query carrier.
     */
    private static KnowledgePageQueryDTO pageQuery(
            int page,
            int size,
            String status) {
        return KnowledgePageQueryDTO.builder()
                .page(page)
                .size(size)
                .status(status)
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
