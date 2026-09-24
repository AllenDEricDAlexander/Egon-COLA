package top.egon.cola.component.yuheng.llm.proxy.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.component.yuheng.llm.config.LlmGatewayProperties;
import top.egon.cola.component.yuheng.llm.proxy.domain.bo.LlmModelSnapshotBO;
import top.egon.cola.component.yuheng.llm.proxy.domain.dto.LlmInvocationCommandDTO;
import top.egon.cola.component.yuheng.llm.proxy.domain.vo.LlmInvocationResultVO;
import top.egon.cola.component.yuheng.llm.proxy.repository.LlmConfigurationRepository;
import top.egon.cola.component.yuheng.llm.proxy.service.LlmInvocationService;
import top.egon.cola.component.yuheng.llm.proxy.service.LlmRouteSelectionStrategy;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 中文说明：{@code LlmInvocationServiceImpl} 是「请求配置快照 → 选定尝试 → 输出发布」这条链的业务归属点：一次调用只在
 * 仓储侧的短只读快照事务（REPEATABLE_READ，model+channels 一致）里读一份 {@link LlmModelSnapshotBO}，事务在返回前已经
 * 结束，因此绝不会有锁或连接跨越上游 HTTP；本类自身<b>不</b>声明 {@code @Transactional}，那正是「外部调用不在事务内」的
 * 结构保证，快照事务由 {@code MpLlmConfigurationRepository} 持有。alias 缺失即 404，绝不伪造空成功；授权、启停、协议/
 * 能力、出域与本地 embedding 判定全部委托给 {@link LlmRouteSelectionStrategy}，本类只负责组合与错误语义：候选被拒时按
 * {@code CommonException} 的原生状态出网，本地 embedding 不可用就是 503，<b>不会</b>改投云端渠道；一个请求只有一个
 * deadline，至多 {@code yuheng.llm.maximum-attempts} 次安全尝试，且全程复用同一份快照（重试不重读配置），已提交之后
 * 不重试。日志只记 alias、协议、channelKey、priority、候选数与尝试预算，绝不记请求体、向量、密钥、baseUrl 或
 * upstreamModel。
 * English summary: {@code LlmInvocationServiceImpl} is where the "request configuration snapshot → selected attempt →
 * output publisher" chain lives: one call reads one {@link LlmModelSnapshotBO} inside the repository's short read-only
 * snapshot transaction (REPEATABLE_READ over model plus channels), which has already ended by the time it returns, so no
 * lock or connection can ever span the upstream HTTP; this class declares <b>no</b> {@code @Transactional} precisely as the
 * structural guarantee that external calls sit outside the transaction, while {@code MpLlmConfigurationRepository} owns the
 * snapshot transaction. A missing alias is a 404 and no empty success is ever fabricated; authorization, enablement,
 * protocol/capability, egress and the local-embedding rules are delegated to {@link LlmRouteSelectionStrategy} and this class
 * only composes them and their error semantics: a rejected candidate set leaves natively with the status carried by
 * {@code CommonException}, an unavailable local embedding is a 503 and is <b>never</b> re-routed to a cloud channel, one
 * request carries one deadline and at most {@code yuheng.llm.maximum-attempts} safe attempts while reusing the same snapshot
 * (a retry never re-reads configuration), with no retry after commit. Logs name only the alias, protocol, channelKey,
 * priority, candidate count and attempt budget — never a body, vector, secret, base URL or upstream model name.
 *
 * 用法 / Usage: 由 {@code LlmApiController}（Step 11）注入；返回的 {@link LlmInvocationResultVO#getBody()} 由同协议
 * Strategy 在首帧验证后写入有界 publisher，一旦发出任何帧就不再换模型或重试。
 */
@Slf4j
@Validated
@RequiredArgsConstructor
@Service("llmInvocationServiceImpl")
public class LlmInvocationServiceImpl implements LlmInvocationService {

    /** 中文说明：alias 的只读一致快照端口，唯一被允许的持久化入口，本类看不到任何 PO 或 DAO。 English summary: the read-only coherent alias snapshot port, the only persistence entry allowed here, exposing no PO and no DAO. */
    @Qualifier("mpLlmConfigurationRepository")
    private final LlmConfigurationRepository configurationRepository;

    /** 中文说明：路由选择策略，承载全部准入与加权顺序，取代任何按协议写死的分支。 English summary: the routing selection strategy carrying every admission test and the weighting order, replacing any protocol-specific hard-coded branch. */
    @Qualifier("llmRouteSelectionStrategy")
    private final LlmRouteSelectionStrategy routeSelectionStrategy;

    /** 中文说明：本进程的出网边界（尝试上限与字节/帧上限），不提供任何宽松默认值。 English summary: this process's egress boundary (attempt ceiling plus byte/frame caps) with no permissive defaults. */
    @Qualifier(LlmGatewayProperties.BEAN_NAME)
    private final LlmGatewayProperties gatewayProperties;

    /**
     * 中文说明：读快照（缺失即 404）→ 选择候选 route → 取首元素作为唯一尝试 → 产出原生状态与安全响应头；候选为空时由
     * 策略抛出原生错误，本方法不吞异常、不伪造空成功，也不在本地 embedding 失败后继续尝试任何云端渠道。
     * English summary: Reads the snapshot (a miss is a 404), selects candidate routes, takes the head as the single attempt and
     * produces the native status and safe headers; an empty candidate set is raised as a native error by the strategy, this
     * method swallows nothing, fabricates no empty success, and never continues to a cloud channel after a local embedding failure.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code LlmInvocationServiceImpl.invoke(command)}。
     * @param command 参数 已校验的调用命令；parameter the validated invocation command.
     * @return 返回 已路由结果，body 由同协议 Strategy 写入；returns the routed result whose body is attached by the same-protocol Strategy.
     * @throws CommonException 策略给出的 403/404/422/503，code 即原生 HTTP 状态；the 403/404/422/503 raised by the strategy, whose code is the native status.
     */
    @Override
    public LlmInvocationResultVO invoke(LlmInvocationCommandDTO command) {
        Objects.requireNonNull(command, "command");
        LlmModelSnapshotBO snapshot = configurationRepository.findSnapshot(command.getModel())
                .orElseThrow(() -> new CommonException(404, "LLM_MODEL_NOT_FOUND",
                        "No configured model alias is bound to the requested alias"));
        List<LlmModelSnapshotBO.RouteBO> candidates = routeSelectionStrategy.select(command, snapshot);
        if (candidates.isEmpty()) {
            throw new CommonException(503, "LLM_NO_ELIGIBLE_ROUTE",
                    "No eligible route was selected for the requested model alias");
        }
        LlmModelSnapshotBO.RouteBO attempt = candidates.get(0);
        int safeAttempts = Math.min(gatewayProperties.getMaximumAttempts(), candidates.size());
        log.info("llm invocation routed alias={} protocol={} channel={} priority={} candidates={} safeAttempts={}",
                command.getModel(), command.getProtocol(), attempt.getChannelKey(), attempt.getPriority(),
                candidates.size(), safeAttempts);
        return new LlmInvocationResultVO()
                .setStatus(200)
                .setCandidates(candidates)
                .setHeaders(safeHeaders(command));
    }

    /**
     * 中文说明：只生成与内容无关的安全响应头：stream 决定 SSE 或 JSON 媒体类型；上游身份、baseUrl、secretRef 与任何
     * 客户端凭据都不进入这里。
     * English summary: Produces only content-independent safe headers, where the stream flag chooses SSE or JSON media
     * type; upstream identity, base URL, secretRef and any client credential never enter here.
     */
    private Map<String, String> safeHeaders(LlmInvocationCommandDTO command) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Content-Type", Boolean.TRUE.equals(command.getStream())
                ? "text/event-stream;charset=UTF-8" : "application/json");
        headers.put("Cache-Control", "no-store");
        return headers;
    }
}
