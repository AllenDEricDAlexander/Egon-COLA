package top.egon.cola.component.yuheng.admin.llm.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import top.egon.cola.component.yuheng.admin.knowledge.domain.vo.KnowledgePageVO;
import top.egon.cola.component.yuheng.admin.llm.domain.dto.LlmChannelCommandDTO;
import top.egon.cola.component.yuheng.admin.llm.domain.dto.LlmModelCommandDTO;
import top.egon.cola.component.yuheng.admin.llm.domain.vo.LlmChannelVO;
import top.egon.cola.component.yuheng.admin.llm.domain.vo.LlmModelVO;
import top.egon.cola.component.yuheng.admin.llm.service.LlmConfigurationService;
import top.egon.cola.component.yuheng.admin.shared.domain.AdminActor;
import top.egon.cola.component.yuheng.admin.shared.domain.RequestAuditContext;
import top.egon.cola.component.yuheng.openapi.annotation.EgonApiCatalog;
import top.egon.cola.component.yuheng.openapi.annotation.EgonGatewayPolicy;

/**
 * 中文说明：{@code LlmConfigurationController} 是原业务 Spec §9.2.4–§9.2.7（API-004–007）的接口控制器，
 * 只负责把既有 HTTP 路径、状态码与直接 JSON shape 交付给 {@link LlmConfigurationService}：
 * 渠道与模型的分页只读返回 {@code items/page/size/total}，完整保存返回单个已提交投影，
 * 创建意图（{@code expectedRevision=0}）为 201 并以同一路径作为 {@code Location}，替换为 200。
 * 身份由已认证主体解析为 {@link AdminActor}，租户不进入任何参数，密钥解析值永不出现在任何响应里。
 * English summary: {@code LlmConfigurationController} is the interface controller of §9.2.4–§9.2.7
 * (API-004–007) of the primary Spec and only delivers the original paths, status codes and bare JSON shapes through
 * {@link LlmConfigurationService}: the paged reads answer with {@code items/page/size/total}, the full saves with the
 * committed projection, a create intent ({@code expectedRevision=0}) with 201 plus a {@code Location} of the same path
 * and a replace with 200. The identity is resolved from the authenticated principal into an {@link AdminActor}, tenancy
 * never becomes a parameter, and a resolved secret never appears in a response.
 *
 * 用法 / Usage: 通过 Spring MVC 暴露的 {@code GET|PUT /api/v1/yuheng/admin/llm/channels[/{channelKey}]} 与
 * {@code GET|PUT /api/v1/yuheng/admin/llm/models[/{modelKey}]} 调用；读需 {@code yuheng:llm:read}，
 * 写需 {@code yuheng:llm:write}。参数与请求体的约束由被调用的业务合同统一复核，本类不重复声明，
 * 也不做健康探测、不解析密钥、不落业务分支。/ Invoke it through the exposed entry points; reads require
 * {@code yuheng:llm:read} and writes {@code yuheng:llm:write}. Parameter and body constraints are checked once by the
 * business contract instead of being duplicated here, and this class probes nothing, resolves no secret and holds no
 * business branch.
 */
@Slf4j
@Validated
@RestController("llmConfigurationController")
@RequestMapping("/api/v1/yuheng/admin/llm")
@PreAuthorize("hasAnyAuthority('CAP_yuheng:llm:read','CAP_*')")
@Tag(name = "yuheng-admin")
@EgonApiCatalog(
        businessDomainCode = "xingyuan",
        businessDomainName = "平台治理域",
        entityDomainCode = "yuheng-admin",
        entityDomainName = "Gateway Admin 管理实体域",
        interfaceGroupCode = "yuheng-admin")
@RequiredArgsConstructor
public class LlmConfigurationController {

    /** 创建意图的乐观版本哨兵值，与业务合同一致 / the create-intent revision sentinel shared with the business contract. */
    private static final long CREATE_REVISION = 0L;

    /**
     * 中文说明：保存 渠道与模型配置业务合同 对应的依赖值；字段类型为 {@code LlmConfigurationService}，由
     * {@code LlmConfigurationController} 在其生命周期内读取或更新。
     * English summary: Holds the dependency represented by the channel and model configuration contract; its type is
     * {@code LlmConfigurationService}, and {@code LlmConfigurationController} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code LlmConfigurationController} 的公开入口使用；/ Access it through the public entry
     * points of {@code LlmConfigurationController}.
     */
    @Qualifier("llmConfigurationServiceImpl")
    private final LlmConfigurationService llmConfigurationService;

    /**
     * 中文说明：执行 listLlmChannels 操作（API-004）；page 从 1 开始、size 有效范围 1–100，越界由业务合同按
     * 422 拒绝，空页返回 {@code []}，只输出 secretRef 引用名。
     * English summary: Executes the listLlmChannels operation (API-004); page starts at one and size stays within 1–100,
     * out-of-range values being rejected as 422 by the business contract, an empty page answering {@code []} and only the
     * secretRef name being projected.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code llmConfigurationController.listLlmChannels(page, size)}。
     * @param page 参数 页码，默认 1；parameter one-based page number, defaulting to one.
     * @param size 参数 页大小，默认 20；parameter page size, defaulting to 20.
     * @return 返回 渠道分页投影；returns the paged channel projection.
     */
    @Operation(operationId = "listLlmChannels")
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @GetMapping("/channels")
    public KnowledgePageVO<LlmChannelVO> listLlmChannels(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return llmConfigurationService.listChannels(page, size);
    }

    /**
     * 中文说明：执行 replaceLlmChannel 操作（API-005）；完整保存渠道，缺失且 {@code expectedRevision=0} 为
     * 201＋Location（同一路径），已存在替换为 200，缺失但 expected 为正为 404，版本/key/状态冲突为 409，
     * 字段不合法为 422；PUT 的并发完全由 {@code expectedRevision} 控制，不引入新幂等键。
     * English summary: Executes the replaceLlmChannel operation (API-005); a full channel save answers 201 plus a
     * {@code Location} of the same path when absent with {@code expectedRevision=0}, 200 when replaced, 404 when absent
     * with a positive expectation, 409 for revision/key/state conflicts and 422 for invalid fields; PUT concurrency stays
     * governed by {@code expectedRevision} alone without a new idempotency key.
     *
     * 用法 / Usage: 调用方式 / Usage:
     * {@code llmConfigurationController.replaceLlmChannel(channelKey, command, actor, requestId)}。
     * @param channelKey 参数 路径渠道稳定 key；parameter channel stable key from the path.
     * @param command 参数 完整保存命令；parameter the full replace command.
     * @param actor 参数 已验证的管理身份；parameter the verified management actor.
     * @param requestId 参数 可选请求标识，仅用于审计关联；parameter the optional request correlation identifier.
     * @return 返回 已提交渠道投影与创建/替换状态；returns the committed channel projection with its create-or-replace status.
     */
    @Operation(operationId = "replaceLlmChannel")
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @PutMapping("/channels/{channelKey}")
    @PreAuthorize("hasAnyAuthority('CAP_yuheng:llm:write','CAP_*')")
    public ResponseEntity<LlmChannelVO> replaceLlmChannel(
            @PathVariable String channelKey,
            @Valid @RequestBody LlmChannelCommandDTO command,
            AdminActor actor,
            @RequestHeader(value = "X-Request-Id",
                    required = false) String requestId) {
        LlmChannelVO view = llmConfigurationService.replaceChannel(
                channelKey,
                command,
                actor,
                audit(requestId)
        );
        log.debug("YUHENG_LLM_CHANNEL_REPLACED channelKey={} revision={}", channelKey, view.getRevision());
        return saved(command.getExpectedRevision(), view);
    }

    /**
     * 中文说明：执行 listLlmModelConfigurations 操作（API-006）；分页只读模型 alias 与能力映射，
     * 枚举按原 wire 字符串输出，routes 复用同一条顶层载体。
     * English summary: Executes the listLlmModelConfigurations operation (API-006); the paged read of model aliases and
     * their capability mappings keeps wire-string enums and reuses the same top-level route carrier.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code llmConfigurationController.listLlmModelConfigurations(page, size)}。
     * @param page 参数 页码，默认 1；parameter one-based page number, defaulting to one.
     * @param size 参数 页大小，默认 20；parameter page size, defaulting to 20.
     * @return 返回 模型分页投影；returns the paged model projection.
     */
    @Operation(operationId = "listLlmModelConfigurations")
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @GetMapping("/models")
    public KnowledgePageVO<LlmModelVO> listLlmModelConfigurations(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return llmConfigurationService.listModels(page, size);
    }

    /**
     * 中文说明：执行 replaceLlmModel 操作（API-007）；完整保存模型 alias，状态码与版本规则与渠道一致，
     * 渠道存在性、协议集合与 EMBEDDING 的 LOCAL 约束由业务层在事务内按库内数据复核。
     * English summary: Executes the replaceLlmModel operation (API-007); a full model alias save keeps the same status and
     * revision rules as the channel operation while channel existence, the protocol set and the EMBEDDING LOCAL constraint
     * are re-checked against the stored rows inside the business transaction.
     *
     * 用法 / Usage: 调用方式 / Usage:
     * {@code llmConfigurationController.replaceLlmModel(modelKey, command, actor, requestId)}。
     * @param modelKey 参数 路径模型稳定 key；parameter model stable key from the path.
     * @param command 参数 完整保存命令；parameter the full replace command.
     * @param actor 参数 已验证的管理身份；parameter the verified management actor.
     * @param requestId 参数 可选请求标识，仅用于审计关联；parameter the optional request correlation identifier.
     * @return 返回 已提交模型投影与创建/替换状态；returns the committed model projection with its create-or-replace status.
     */
    @Operation(operationId = "replaceLlmModel")
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @PutMapping("/models/{modelKey}")
    @PreAuthorize("hasAnyAuthority('CAP_yuheng:llm:write','CAP_*')")
    public ResponseEntity<LlmModelVO> replaceLlmModel(
            @PathVariable String modelKey,
            @Valid @RequestBody LlmModelCommandDTO command,
            AdminActor actor,
            @RequestHeader(value = "X-Request-Id",
                    required = false) String requestId) {
        LlmModelVO view = llmConfigurationService.replaceModel(
                modelKey,
                command,
                actor,
                audit(requestId)
        );
        log.debug("YUHENG_LLM_MODEL_REPLACED modelKey={} revision={}", modelKey, view.getRevision());
        return saved(command.getExpectedRevision(), view);
    }

    /**
     * 中文说明：按原合同把完整保存的结果状态化为 201＋Location（同一路径）或 200；创建意图只由
     * {@code expectedRevision=0} 表达，业务层已保证该意图与库内状态冲突时抛 409，因此这里不存在“谎报创建”。
     * English summary: Applies the contract's 201 plus a {@code Location} of the same path versus 200 outcome; the create
     * intent is expressed only by {@code expectedRevision=0}, and because the business layer already raises 409 when that
     * intent collides with the stored state, no created answer can be a lie.
     * @param expectedRevision 参数 调用方期望 revision；parameter the caller reported expectation.
     * @param view 参数 已提交投影；parameter the committed projection.
     * @return 返回 带状态的完整保存响应；returns the full save response carrying its status.
     */
    private static <T> ResponseEntity<T> saved(
            Long expectedRevision,
            T view) {
        if (expectedRevision != null && expectedRevision.longValue() == CREATE_REVISION) {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .location(samePath())
                    .body(view);
        }
        return ResponseEntity.ok(view);
    }

    /**
     * 中文说明：{@code Location} 取当前请求的同一绝对路径，符合原合同“Location为同一路径”，不猜测网关前缀。
     * English summary: Takes the absolute path of the current request as the {@code Location}, matching the contract's
     * “Location is the same path” without guessing a gateway prefix.
     * @return 返回 同一路径；returns the same request path.
     */
    private static URI samePath() {
        return ServletUriComponentsBuilder.fromCurrentRequest()
                .build()
                .toUri();
    }

    /**
     * 中文说明：构造请求审计上下文；requestId 缺省时由跟踪上下文补齐，本类不接受任何身份或租户 Header。
     * English summary: Builds the request audit context, the absent requestId being filled from the trace context, and no
     * identity or tenancy header is ever accepted by this class.
     * @param requestId 参数 可选请求标识；parameter the optional request identifier.
     * @return 返回 请求审计上下文；returns the request audit context.
     */
    private RequestAuditContext audit(String requestId) {
        return RequestAuditContext.current(requestId);
    }
}
