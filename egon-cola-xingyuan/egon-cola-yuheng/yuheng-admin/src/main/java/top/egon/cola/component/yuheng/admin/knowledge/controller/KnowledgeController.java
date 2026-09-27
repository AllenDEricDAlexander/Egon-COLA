package top.egon.cola.component.yuheng.admin.knowledge.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.groups.Default;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import top.egon.cola.component.yuheng.admin.knowledge.domain.dto.KnowledgeAnswerCommandDTO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.dto.KnowledgeBaseCommandDTO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.dto.KnowledgeMembersCommandDTO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.dto.KnowledgePageQueryDTO;
import top.egon.cola.component.yuheng.admin.knowledge.converter.KnowledgeMembersConverter;
import top.egon.cola.component.yuheng.admin.knowledge.domain.vo.KnowledgeAnswerVO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.vo.KnowledgeBaseVO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.vo.KnowledgeMembersVO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.vo.KnowledgePageVO;
import top.egon.cola.component.yuheng.admin.knowledge.service.KnowledgeService;
import top.egon.cola.component.yuheng.admin.shared.domain.AdminActor;
import top.egon.cola.component.yuheng.admin.shared.domain.validation.UpdateGroup;
import top.egon.cola.component.yuheng.openapi.annotation.EgonApiCatalog;
import top.egon.cola.component.yuheng.openapi.annotation.EgonGatewayPolicy;

/**
 * 中文说明：{@code KnowledgeController} 是原业务 Spec §9.2.8–§9.2.13 与 §9.2.22（API-008–013、API-022）的接口控制器，
 * 只负责把既有 HTTP 路径、operationId、状态码与裸 JSON shape 交付给 {@link KnowledgeService}：
 * 知识库的分页只读返回 {@code items/page/size/total}，单个读取与完整替换返回已提交投影，
 * 成员读写返回含权威revision的 {@code {members, revision}} 快照，接地问答返回带引用的答案投影。
 * 身份由已认证主体解析为 {@link AdminActor} 并作为业务合同的第一个入参，租户不进入任何参数；
 * READER/EDITOR/OWNER 的角色判定、{@code KB_CREATE} 权限、幂等意图复用与 revision CAS 全部在业务合同层完成，
 * 本类不含任何权限分支、仓储访问或错误包装，也不投影原文、提示词、向量与密钥。
 * English summary: {@code KnowledgeController} is the interface controller of §9.2.8–§9.2.13 and §9.2.22
 * (API-008–013, API-022) of the primary business Spec and only delivers the original paths, operationIds, status codes and
 * bare JSON shapes through {@link KnowledgeService}: the paged read answers {@code items/page/size/total}, the single read
 * and the full replace answer with the committed projection, the member operations answer with the authoritative
 * {@code {members, revision}} snapshot, and the grounded question answers with the cited answer projection. The identity is
 * resolved from the authenticated principal into an {@link AdminActor} passed as the first business argument, tenancy never
 * becomes a parameter; the READER/EDITOR/OWNER decision, the {@code KB_CREATE} capability, idempotency reuse and the
 * revision CAS all live in the business contract, so this class holds no permission branch, no repository access, no error
 * wrapper and projects no document body, prompt, vector or secret.
 *
 * 用法 / Usage: 通过 Spring MVC 暴露的 {@code GET|POST /api/v1/yuheng/admin/knowledge-bases}、
 * {@code GET|PUT /api/v1/yuheng/admin/knowledge-bases/{kbId}}、
 * {@code GET|PUT /api/v1/yuheng/admin/knowledge-bases/{kbId}/members} 与
 * {@code POST /api/v1/yuheng/admin/knowledge-bases/{kbId}/answers} 调用；读需 {@code yuheng:knowledge:read}，
 * 创建需 {@code yuheng:knowledge:write}，成员与知识库配置替换需 {@code yuheng:knowledge:admin}。
 * 分页参数在本类补齐 {@code page=1, size=20} 默认值（载体字段是原始 {@code int}，缺省会变成 0 并被 422 误拒），
 * 其余参数与请求体的约束由被调用的业务合同统一复核，本类不重复声明校验分组之外的规则、不下沉任何业务判断。
 * / Invoke it through the exposed entry points; reads require {@code yuheng:knowledge:read}, creation requires
 * {@code yuheng:knowledge:write} and the member plus configuration replaces require {@code yuheng:knowledge:admin}. Paging
 * defaults are filled in here because the carrier fields are primitive {@code int}s and an absent value would reach the
 * contract as zero, while every other constraint is checked once by the business contract instead of being duplicated here.
 */
@Slf4j
@Validated
@RestController("knowledgeController")
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
public class KnowledgeController {

    /** 知识库资源绝对路径前缀，仅用于拼装 201 的 {@code Location}，不猜测网关前缀 / the knowledge base absolute path prefix used only to build the 201 {@code Location} without guessing a gateway prefix. */
    private static final String KNOWLEDGE_BASE_PATH = "/api/v1/yuheng/admin/knowledge-bases/";

    /**
     * 中文说明：保存 知识库管理业务合同 对应的依赖值；字段类型为 {@code KnowledgeService}，由
     * {@code KnowledgeController} 在其生命周期内读取或更新。
     * English summary: Holds the dependency represented by the knowledge management business contract; its type is
     * {@code KnowledgeService}, and {@code KnowledgeController} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code KnowledgeController} 的公开入口使用；/ Access it through the public entry
     * points of {@code KnowledgeController}.
     */
    @Qualifier("knowledgeServiceImpl")
    private final KnowledgeService knowledgeService;

    @Qualifier("knowledgeMembersConverter")
    private final KnowledgeMembersConverter knowledgeMembersConverter;

    /**
     * 中文说明：执行 listKnowledgeBases 操作（API-008）；page 从 1 开始、size 有效范围 1–100，越界由业务合同按
     * 422 拒绝，次序固定 {@code createdAt DESC, id DESC}，空页返回 {@code []} 且 {@code total=0}，
     * 每行的 {@code myRole} 是当前主体的成员角色；未声明的 {@code search} 参数刻意不绑定，也不参与任何过滤。
     * English summary: Executes the listKnowledgeBases operation (API-008); page starts at one and size stays within 1–100,
     * out-of-range values being rejected as 422 by the business contract, the order fixed to
     * {@code createdAt DESC, id DESC}, an empty page answering {@code []} with {@code total = 0} and each row's
     * {@code myRole} projecting the current subject's member role; the undeclared {@code search} parameter stays unbound and
     * filters nothing.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code knowledgeController.listKnowledgeBases(actor, page, size)}。
     * @param actor 参数 已验证的管理身份；parameter the verified management actor.
     * @param page 参数 页码，默认 1；parameter one-based page number, defaulting to one.
     * @param size 参数 页大小，默认 20；parameter page size, defaulting to 20.
     * @return 返回 知识库分页投影；returns the paged knowledge base projection.
     */
    @Operation(operationId = "listKnowledgeBases")
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @GetMapping("/knowledge-bases")
    public KnowledgePageVO<KnowledgeBaseVO> listKnowledgeBases(
            AdminActor actor,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return knowledgeService.listBases(
                actor,
                pageQuery(page, size)
        );
    }

    /**
     * 中文说明：执行 createKnowledgeBase 操作（API-009）；仅 {@code yuheng:knowledge:write} 能力可发起（KB_CREATE 是
     * 逻辑权限标签而非成员判定，创建时知识库尚不存在），201 返回 {@code KnowledgeBaseVO} 并以新身份路径作为
     * {@code Location}；请求体只有 5 个业务字段、不含 {@code expectedRevision}，{@code ownerActorId} 与初始成员由
     * 服务端从可信身份派生，嵌入空间与维度按 alias 冻结，创建过程不发生任何模型网络调用。
     * {@code Idempotency-Key} 按原合同为必带项，但绑定为可选 Header 后交业务合同复核：命中同一意图返回原结果，
     * 同 key 不同摘要为 409 {@code KNOWLEDGE_IDEMPOTENCY_CONFLICT}。
     * English summary: Executes the createKnowledgeBase operation (API-009); only the {@code yuheng:knowledge:write}
     * capability may call it (KB_CREATE is a logical capability label rather than a membership decision, since the
     * knowledge base does not exist yet), answering 201 with the {@code KnowledgeBaseVO} and a {@code Location} of the new
     * identity path. The body carries only the five business fields and no {@code expectedRevision}, while
     * {@code ownerActorId} and the seeded member list are derived from the trusted identity and the embedding space plus
     * dimensions are frozen from the aliases, with no model call during creation. {@code Idempotency-Key} is mandatory per
     * the contract yet is bound as an optional header and re-checked by the business contract: the same intent returns the
     * original outcome while the same key with a diverged digest is a 409 {@code KNOWLEDGE_IDEMPOTENCY_CONFLICT}.
     *
     * 用法 / Usage: 调用方式 / Usage:
     * {@code knowledgeController.createKnowledgeBase(command, actor, idempotencyKey)}。
     * @param command 参数 创建命令；parameter the create command.
     * @param actor 参数 已验证的管理身份；parameter the verified management actor.
     * @param idempotencyKey 参数 可选 {@code Idempotency-Key} 请求头，缺省与复核由业务合同决定；parameter the
     *                       optional {@code Idempotency-Key} header whose presence rules stay with the business contract.
     * @return 返回 已提交的知识库投影与 201 状态；returns the committed knowledge base projection carried in a 201.
     */
    @Operation(operationId = "createKnowledgeBase")
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL,
            idempotency = EgonGatewayPolicy.Idempotency.TRUE)
    @PostMapping("/knowledge-bases")
    @PreAuthorize("hasAnyAuthority('CAP_yuheng:knowledge:write','CAP_*')")
    public ResponseEntity<KnowledgeBaseVO> createKnowledgeBase(
            @Valid @RequestBody KnowledgeBaseCommandDTO command,
            AdminActor actor,
            @RequestHeader(value = "Idempotency-Key",
                    required = false) String idempotencyKey) {
        KnowledgeBaseVO view = knowledgeService.createBase(
                actor,
                command,
                idempotencyKey
        );
        log.debug("YUHENG_KNOWLEDGE_BASE_CREATED kbId={} revision={}", view.getId(), view.getRevision());
        return ResponseEntity.status(HttpStatus.CREATED)
                .location(knowledgeBaseLocation(view.getId()))
                .body(view);
    }

    /**
     * 中文说明：执行 getKnowledgeBase 操作（API-010）；READER 及以上（owner 或成员）可见，
     * 不可见按 403 {@code KNOWLEDGE_FORBIDDEN} 而不是 404，避免把存在性泄漏给无权限身份，
     * 本租户确实不存在或已软删的行才是 404 {@code KNOWLEDGE_RESOURCE_NOT_FOUND}；
     * 响应携带服务端当前 {@code revision}，供后续替换作为期望值。
     * English summary: Executes the getKnowledgeBase operation (API-010); visible at READER or above (owner or member), an
     * invisible one becoming 403 {@code KNOWLEDGE_FORBIDDEN} rather than 404 so existence never leaks to an unauthorized
     * actor while a row genuinely absent or soft-deleted inside this tenant is 404
     * {@code KNOWLEDGE_RESOURCE_NOT_FOUND}; the answer carries the server-side {@code revision} to be used as the next
     * expectation.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code knowledgeController.getKnowledgeBase(kbId, actor)}。
     * @param kbId 参数 知识库十进制字符串 id；parameter decimal-string knowledge base id.
     * @param actor 参数 已验证的管理身份；parameter the verified management actor.
     * @return 返回 知识库投影；returns the knowledge base projection.
     */
    @Operation(operationId = "getKnowledgeBase")
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @GetMapping("/knowledge-bases/{kbId}")
    public KnowledgeBaseVO getKnowledgeBase(
            @NotBlank
            @Pattern(regexp = "^[1-9][0-9]{0,19}$")
            @PathVariable String kbId,
            AdminActor actor) {
        return knowledgeService.getBase(
                actor,
                kbId
        );
    }

    /**
     * 中文说明：执行 replaceKnowledgeBase 操作（API-011）；仅 OWNER 可为，200 返回推进后的权威投影；
     * 请求体是同一命令载体加 {@code expectedRevision}，因此按 DTO 声明的 {@code UpdateGroup} 分组复核
     * （该分组里 {@code expectedRevision} 必填且非负），不引入第二条创建语义；
     * 版本不匹配为 409 {@code KNOWLEDGE_REVISION_CONFLICT} 并携带库中现值，
     * 已冻结的嵌入空间与维度不得原地改变，alias 复核不成立为 422 {@code KNOWLEDGE_VALIDATION_FAILED}，
     * 本地嵌入别名不可用为 503 {@code KNOWLEDGE_MODEL_UNAVAILABLE}，PUT 并发完全由 {@code expectedRevision} 控制。
     * English summary: Executes the replaceKnowledgeBase operation (API-011); OWNER only, answering 200 with the advanced
     * authoritative projection. The body is the same command carrier plus {@code expectedRevision}, so it is validated in
     * the {@code UpdateGroup} the DTO declares (where {@code expectedRevision} is required and non-negative) instead of
     * inventing a second create shape. A revision mismatch is 409 {@code KNOWLEDGE_REVISION_CONFLICT} carrying the stored
     * value, the frozen embedding space and dimensions may not change in place, a failed alias re-check is 422
     * {@code KNOWLEDGE_VALIDATION_FAILED} and an unavailable local embedding alias is 503
     * {@code KNOWLEDGE_MODEL_UNAVAILABLE}; PUT concurrency stays governed by {@code expectedRevision} alone.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code knowledgeController.replaceKnowledgeBase(kbId, command, actor)}。
     * @param kbId 参数 知识库十进制字符串 id；parameter decimal-string knowledge base id.
     * @param command 参数 完整替换命令；parameter the full replace command.
     * @param actor 参数 已验证的管理身份；parameter the verified management actor.
     * @return 返回 已提交的知识库投影；returns the committed knowledge base projection.
     */
    @Operation(operationId = "replaceKnowledgeBase")
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @PutMapping("/knowledge-bases/{kbId}")
    @PreAuthorize("hasAnyAuthority('CAP_yuheng:knowledge:admin','CAP_*')")
    public KnowledgeBaseVO replaceKnowledgeBase(
            @NotBlank
            @Pattern(regexp = "^[1-9][0-9]{0,19}$")
            @PathVariable String kbId,
            @Validated({Default.class, UpdateGroup.class})
            @RequestBody KnowledgeBaseCommandDTO command,
            AdminActor actor) {
        KnowledgeBaseVO view = knowledgeService.replaceBase(
                actor,
                kbId,
                command
        );
        log.debug("YUHENG_KNOWLEDGE_BASE_REPLACED kbId={} revision={}", kbId, view.getRevision());
        return view;
    }

    /**
     * 中文说明：执行 listKnowledgeMembers 操作（API-012）；OWNER 可见，返回稳定排序的 {@code {members, revision}}
     * 快照，使成员集合与用于CAS的知识库业务版本来自同一读取。
     * English summary: Executes the listKnowledgeMembers operation (API-012) for an OWNER and returns the stable
     * {@code {members, revision}} snapshot so the member list and its CAS revision come from one read.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code knowledgeController.listKnowledgeMembers(kbId, actor)}。
     * @param kbId 参数 知识库十进制字符串 id；parameter decimal-string knowledge base id.
     * @param actor 参数 已验证的管理身份；parameter the verified management actor.
     * @return 返回 成员与权威版本；returns members with the authoritative revision.
     */
    @Operation(operationId = "listKnowledgeMembers")
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @GetMapping("/knowledge-bases/{kbId}/members")
    public KnowledgeMembersVO listKnowledgeMembers(
            @NotBlank
            @Pattern(regexp = "^[1-9][0-9]{0,19}$")
            @PathVariable String kbId,
            AdminActor actor) {
        return knowledgeMembersConverter.toTarget(knowledgeService.listMembers(actor, kbId));
    }

    /**
     * 中文说明：执行 replaceKnowledgeMembers 操作（API-013）；仅 OWNER 能力可发起，200 返回替换后的权威成员集合与版本；
     * 请求体为 {@code {members[], expectedRevision}}，元素唯一、总数受限、角色落在 {@code READER/EDITOR/OWNER} 词汇表内，
     * owner 自身可见性不得被移除；替换以期望 revision 做 CAS，不匹配为 409 并携带现值，
     * 0 行写入绝不返回成功；成员变化会提升 revision，使正在执行的输出需再次授权。
     * English summary: Executes the replaceKnowledgeMembers operation (API-013) under the OWNER capability, answering 200
     * with the authoritative member set after the replace. The body is {@code {members[], expectedRevision}} whose entries
     * stay unique and bounded with roles inside the {@code READER/EDITOR/OWNER} vocabulary and whose owner visibility may
     * never be removed; the write is a CAS on the expected revision — a mismatch is 409 carrying the stored value and a
     * zero-row effect is never reported as success — while a member change raises the revision so an in-flight output needs
     * re-authorization.
     *
     * 用法 / Usage: 调用方式 / Usage:
     * {@code knowledgeController.replaceKnowledgeMembers(kbId, command, actor)}。
     * @param kbId 参数 知识库十进制字符串 id；parameter decimal-string knowledge base id.
     * @param command 参数 成员完整替换命令；parameter the full members replacement command.
     * @param actor 参数 已验证的管理身份；parameter the verified management actor.
     * @return 返回 已提交成员与权威版本；returns the committed members and authoritative revision.
     */
    @Operation(operationId = "replaceKnowledgeMembers")
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @PutMapping("/knowledge-bases/{kbId}/members")
    @PreAuthorize("hasAnyAuthority('CAP_yuheng:knowledge:admin','CAP_*')")
    public KnowledgeMembersVO replaceKnowledgeMembers(
            @NotBlank
            @Pattern(regexp = "^[1-9][0-9]{0,19}$")
            @PathVariable String kbId,
            @Valid @RequestBody KnowledgeMembersCommandDTO command,
            AdminActor actor) {
        KnowledgeMembersVO view = knowledgeMembersConverter.toTarget(
                knowledgeService.replaceMembers(actor, kbId, command));
        log.debug("YUHENG_KNOWLEDGE_MEMBERS_REPLACED kbId={} revision={}", kbId, view.getRevision());
        return view;
    }

    /**
     * 中文说明：执行 createKnowledgeAnswer 操作（API-022）；READER 及以上方可为，200 返回
     * {@code {outcome, answer, citations[], model}}；本声明刻意不带 {@code Idempotency-Key}，
     * 因为原合同把问答列为幂等例外；{@code question}/{@code sourceMode}/{@code topK}/{@code searchMode} 的值域
     * 由命令载体与业务合同复核，权限与出域判定先于检索执行，无证据时如实返回
     * {@code NO_EVIDENCE} 加空引用而不是编造，依赖故障为 503 而不是成功的空字符串。
     * English summary: Executes the createKnowledgeAnswer operation (API-022) at READER or above, answering 200 with
     * {@code {outcome, answer, citations[], model}}. This declaration deliberately carries no {@code Idempotency-Key}
     * because the contract exempts the question answer from any idempotency promise. The value ranges of
     * {@code question}, {@code sourceMode}, {@code topK} and {@code searchMode} are re-checked by the command carrier and
     * the business contract, authorization plus egress precede retrieval, an empty evidence set honestly answers
     * {@code NO_EVIDENCE} with no citations instead of a fabrication, and a dependency failure is a 503 rather than a
     * successful empty string.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code knowledgeController.createKnowledgeAnswer(kbId, command, actor)}。
     * @param kbId 参数 知识库十进制字符串 id；parameter decimal-string knowledge base id.
     * @param command 参数 问答命令；parameter the answer command.
     * @param actor 参数 已验证的管理身份；parameter the verified management actor.
     * @return 返回 带引用的答案投影；returns the grounded answer projection with its citations.
     */
    @Operation(operationId = "createKnowledgeAnswer")
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @PostMapping("/knowledge-bases/{kbId}/answers")
    public KnowledgeAnswerVO createKnowledgeAnswer(
            @NotBlank
            @Pattern(regexp = "^[1-9][0-9]{0,19}$")
            @PathVariable String kbId,
            @Valid @RequestBody KnowledgeAnswerCommandDTO command,
            AdminActor actor) {
        KnowledgeAnswerVO view = knowledgeService.answer(
                actor,
                kbId,
                command
        );
        log.debug("YUHENG_KNOWLEDGE_ANSWERED kbId={} outcome={} citationCount={}",
                kbId,
                view.getOutcome(),
                view.getCitations()
                        .size()
        );
        return view;
    }

    /**
     * 中文说明：把已补齐默认值的分页参数装配成查询载体；只有原合同声明过的 {@code page} 与 {@code size} 会被绑定，
     * 载体上的 {@code search} 与 {@code status} 在本资源保持未启用，避免接入任何未声明的检索行为。
     * English summary: Assembles the query carrier from the defaulted paging parameters, binding only the
     * contract-declared {@code page} and {@code size} for this resource and leaving the carrier's {@code search} and
     * {@code status} unpowered so no undeclared search behaviour is wired in.
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
     * 中文说明：为 201 创建结果拼装新身份的绝对 {@code Location}，只取当前部署的 contextPath 与合同路径，
     * 不猜测网关前缀；知识库 id 已由业务合同按十进制正整数投影，因此无需再做转义。
     * English summary: Builds the absolute {@code Location} of the freshly created identity for a 201 answer from the
     * current deployment context path plus the contract path, never guessing a gateway prefix; the knowledge base id is
     * already projected as a decimal positive integer by the business contract, so no further escaping is needed.
     * @param kbId 参数 新建知识库 id；parameter the created knowledge base id.
     * @return 返回 该知识库的可寻址位置；returns the addressable location of that knowledge base.
     */
    private static URI knowledgeBaseLocation(String kbId) {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(KNOWLEDGE_BASE_PATH)
                .path(kbId)
                .build()
                .toUri();
    }
}
