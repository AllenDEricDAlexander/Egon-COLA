package top.egon.cola.component.yuheng.admin.knowledge.service.impl;

import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.component.yuheng.admin.knowledge.converter.KnowledgeCitationConverter;
import top.egon.cola.component.yuheng.admin.knowledge.domain.bo.KnowledgeBaseBO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.bo.KnowledgeDocumentBO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.bo.KnowledgeRetrievalHitBO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.bo.KnowledgeSearchQueryBO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.dto.KnowledgeAnswerCommandDTO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.dto.KnowledgeMemberDTO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.enums.KnowledgeMemberRoleEnum;
import top.egon.cola.component.yuheng.admin.knowledge.domain.enums.KnowledgeSearchModeEnum;
import top.egon.cola.component.yuheng.admin.knowledge.domain.enums.KnowledgeSourceModeEnum;
import top.egon.cola.component.yuheng.admin.knowledge.domain.vo.KnowledgeAnswerVO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.vo.KnowledgeCitationVO;
import top.egon.cola.component.yuheng.admin.knowledge.repository.KnowledgeRepository;
import top.egon.cola.component.yuheng.admin.knowledge.service.KnowledgeModelClientService;
import top.egon.cola.component.yuheng.admin.knowledge.service.KnowledgeRetrievalService;
import top.egon.cola.component.yuheng.admin.knowledge.service.KnowledgeSearchStrategy;
import top.egon.cola.component.yuheng.admin.shared.domain.AdminActor;
import top.egon.cola.component.yuheng.admin.shared.domain.exception.GatewayAdminNotFoundException;

/**
 * 中文说明：{@code KnowledgeRetrievalServiceImpl} 是 API-022 授权检索与可追溯接地问答的实现，把一次问答固定为
 * 「命令解析 -> 角色复核 -> 冻结取证范围校验 -> 按需本地嵌入 -> 注册表选路召回 -> 交付前再授权 -> 有证据才生成」
 * 七段，且顺序不可调换：
 * <ol>
 *   <li>模式与条数只按字面量解析，未知识别符是 {@code 422} 而不是回落到默认算法；缺省为
 *       {@code DOCUMENTS + VECTOR + 8} 条。</li>
 *   <li>角色复核复用与 {@code KnowledgeServiceImpl} 同一口径（不可见 {@code 404}、可见但角色不足 {@code 403}，顺序固定以免泄漏存在性）。</li>
 *   <li>知识库未冻结嵌入空间或维度时按 {@code 422} 失败：那意味着活动索引还不存在，检索无 scope 可依，
 *       绝不能悄悄换成「不过滤的全表召回」。</li>
 *   <li>只有 {@code VECTOR}/{@code HYBRID} 才发一次本地嵌入（无云端兜底），且返回向量长度必须等于冻结维度，
 *       否则按依赖不可用 {@code 503} 收束，不截断、不补零。</li>
 *   <li>召回模式经 {@code knowledgeSearchStrategyRegistry} 查表分发（Rule 9），本类没有算法 {@code switch}；
 *       授权谓词全部在 SQL 内，本类不拼 SQL、不算余弦。</li>
 *   <li>生成之前重做一次成员复核，并逐条确认候选所属文档的当前活动修订仍然等于候选的冻结修订——
 *       期间发生过发布就丢弃该条证据，宁缺不混。因此交付的每条引用都同时满足「本身份当前可读」与「来源仍是当前版本」。</li>
 *   <li>零证据走 {@code NO_EVIDENCE}（HTTP 成功、引用为空、生成调用为零），依赖失败走 {@code 503}，
 *       两者绝不混同；无证据时绝不生成无引用答案。</li>
 * </ol>
 * 出站形态只经 {@code KnowledgeCitationConverter} 单向投影，且日志只到 id、计数、状态码与耗时为止：
 * 提示词、分块正文、向量与密钥一律不落日志、不出响应。本方法不写任何业务行，故不加事务——
 * 模型调用必须在锁与事务之外。
 * English summary: {@code KnowledgeRetrievalServiceImpl} implements API-022's authorized retrieval and traceable grounded
 * answer as seven ordered stages, parse command -> re-check role -> validate the frozen evidence scope -> embed locally
 * when the mode needs it -> recall through the registry-selected strategy -> re-authorize before delivery -> generate only
 * on surviving evidence, an order that cannot be rearranged: mode and count parse by token so an unknown token is
 * {@code 422} rather than a fall-back to a different algorithm, defaulting to {@code DOCUMENTS + VECTOR + eight}; the role
 * check reuses the same reading as {@code KnowledgeServiceImpl} (invisible is {@code 404}, visible-but-insufficient is
 * {@code 403}, in that fixed order so existence never leaks); a knowledge base with no frozen embedding space or
 * dimensions fails as {@code 422} because that means no active index exists and an unscoped whole-table recall is never a
 * substitute; only {@code VECTOR} and {@code HYBRID} issue one local embedding, with no cloud fallback, and a returned
 * vector whose length differs from the frozen dimensions closes out as {@code 503} rather than a truncated or
 * zero-padded one; recall dispatches through {@code knowledgeSearchStrategyRegistry} under Rule 9 with no algorithm
 * {@code switch} here, every authorization predicate living in SQL; before generation membership is read again and each
 * candidate's document must still carry the frozen revision, so a publication during the request drops that piece of
 * evidence instead of mixing bases and every citation delivered is both currently readable by this identity and still
 * current in its source; zero evidence is {@code NO_EVIDENCE} (a successful response, no citations and zero generation
 * calls) while a dependency failure stays {@code 503}, never conflated, and an unsupported answer is never generated
 * without citations. The outbound shape passes only through the one-way {@code KnowledgeCitationConverter} and logging
 * stops at identifiers, counts, status codes and elapsed time, so no prompt, chunk body, vector or key reaches a log or a
 * response. This method writes no business row and therefore carries no transaction, keeping model calls outside any
 * lock.
 *
 * 用法 / Usage: bean 名 {@code knowledgeRetrievalServiceImpl}，由 {@code KnowledgeServiceImpl#answer} 在角色复核后委托；
 * 检索预算固定 5 秒（Spec §7.3.4），超时按 {@code 503} 失败而不是静默放宽过滤。
 * 每路候选上限 50、最终 topK 上限 20、提示词上限 60000 字符与生成长度上界都是本类的显式常量：
 * 用户指令本期不写配置文件，故未进 {@code KnowledgeProperties}，接入配置时只需替换常量读取点。
 * Injected under the bean name {@code knowledgeRetrievalServiceImpl} and delegated to by {@code KnowledgeServiceImpl#answer}
 * after its role check; the five-second retrieval budget (Spec §7.3.4) is enforced here and a breach is {@code 503} rather
 * than a silently relaxed filter. The per-leg candidate bound of fifty, the topK ceiling of twenty, the
 * sixty-thousand-character prompt ceiling and the generation length bound are explicit constants in this class: the user
 * deferred all configuration work, so they are not in {@code KnowledgeProperties} yet and wiring configuration later only
 * replaces where these are read.
 */
@Slf4j
@Validated
@Service("knowledgeRetrievalServiceImpl")
@RequiredArgsConstructor
public class KnowledgeRetrievalServiceImpl implements KnowledgeRetrievalService {

    /** 未显式给条数时的引用条数 / the citation count when the caller states none. */
    private static final int DEFAULT_TOP_K = 8;

    /** Spec §7.3.4 的 topK 上限 / the Spec's topK ceiling. */
    private static final int MAX_TOP_K = 20;

    /** Spec §7.3.4 的每路候选上限（混合路两腿各半，各自不超过此值）/ the per-leg candidate ceiling, each hybrid leg staying under it. */
    private static final int MAX_CANDIDATES_PER_LEG = 50;

    /** {@code generate} 端口的硬上限，也是提示词裁剪目标 / the port's prompt ceiling, which the assembly trims to. */
    private static final int MAX_PROMPT_CHARACTERS = 60_000;

    /** 单次生成的 token 上界 / the completion token bound of one generation. */
    private static final int GENERATION_MAX_TOKENS = 1_024;

    /** Spec §7.3.4 的检索预算，超时不放宽过滤 / the retrieval budget, a breach never relaxing the filters. */
    private static final Duration RETRIEVAL_BUDGET = Duration.ofSeconds(5);

    /** 无证据时的固定回答 / the fixed answer when no evidence survives. */
    private static final String NO_EVIDENCE_ANSWER = "未找到可引用资料";

    /** READER 及以上可读取与问答 / the roles allowed to read and ask. */
    private static final Set<KnowledgeMemberRoleEnum> READER_OR_ABOVE = EnumSet.of(
            KnowledgeMemberRoleEnum.READER,
            KnowledgeMemberRoleEnum.EDITOR,
            KnowledgeMemberRoleEnum.OWNER
    );

    @Qualifier("knowledgeRepository")
    private final KnowledgeRepository knowledgeRepository;

    @Qualifier("knowledgeModelClientService")
    private final KnowledgeModelClientService knowledgeModelClientService;

    @Qualifier("knowledgeSearchStrategyRegistry")
    private final Map<KnowledgeSearchModeEnum, KnowledgeSearchStrategy> knowledgeSearchStrategyRegistry;

    @Qualifier("knowledgeCitationConverter")
    private final KnowledgeCitationConverter knowledgeCitationConverter;

    /**
     * 中文说明：执行 answer 操作，按类注释的七段顺序完成一次授权问答；
     * 所有对外调用（嵌入与生成）都在取数与授权之后、且在事务之外发起。
     * English summary: Executes the answer operation through the seven ordered stages described on the class, issuing every
     * external call, embedding and generation alike, after reading and authorization and outside any transaction.
     *
     * 用法 / Usage: {@code knowledgeRetrievalServiceImpl.answer(actor, kbId, command)}。
     * @param actor 参数 已验证的管理身份；parameter the verified management actor.
     * @param kbId 参数 知识库十进制字符串 id；parameter decimal-string knowledge base id.
     * @param command 参数 问答命令；parameter the answer command.
     * @return 返回 带引用的答案投影；returns the grounded answer projection with its citations.
     */
    @Override
    public KnowledgeAnswerVO answer(AdminActor actor, String kbId, KnowledgeAnswerCommandDTO command) {
        long budgetDeadline = System.nanoTime() + RETRIEVAL_BUDGET.toNanos();
        KnowledgeSourceModeEnum sourceMode = sourceModeOf(command.getSourceMode());
        KnowledgeSearchModeEnum searchMode = searchModeOf(command.getSearchMode());
        int topK = command.getTopK() == null ? DEFAULT_TOP_K : Math.min(command.getTopK(), MAX_TOP_K);

        KnowledgeBaseBO base = requireReadableBase(actor, kbId);
        if (StringUtils.isBlank(base.getEmbeddingSpaceId()) || base.getDimensions() == null) {
            throw new CommonException(
                    422,
                    "KNOWLEDGE_VALIDATION_FAILED",
                    "the knowledge base has no frozen embedding space, so no active index can be searched"
            );
        }
        KnowledgeSearchQueryBO query = new KnowledgeSearchQueryBO()
                .setKbId(kbId)
                .setActorId(actor.actorId())
                .setEmbeddingSpaceId(base.getEmbeddingSpaceId())
                .setDimensions(base.getDimensions())
                .setKeyword(command.getQuestion())
                .setCandidateLimit(MAX_CANDIDATES_PER_LEG)
                .setSourceMode(sourceMode);
        if (searchMode.requiresQueryVector()) {
            checkBudget(budgetDeadline);
            query.setQueryVector(queryVector(kbId, command.getQuestion(), base.getDimensions()));
        }

        List<KnowledgeRetrievalHitBO> candidates = strategyOf(searchMode).search(query);
        checkBudget(budgetDeadline);

        requireReadableBase(actor, kbId);
        List<KnowledgeRetrievalHitBO> evidence = retainCurrentSources(candidates, kbId, topK);
        if (evidence.isEmpty()) {
            log.info("knowledge answer closed without evidence kbId={} searchMode={} sourceMode={} candidates={}",
                    kbId, searchMode.wireValue(), sourceMode.wireValue(), candidates.size());
            return projection("NO_EVIDENCE", NO_EVIDENCE_ANSWER, List.of(), base);
        }
        checkBudget(budgetDeadline);
        String answer = knowledgeModelClientService.generate(
                kbId, prompt(command.getQuestion(), evidence), GENERATION_MAX_TOKENS);
        if (StringUtils.isBlank(answer)) {
            throw new CommonException(
                    503,
                    "KNOWLEDGE_MODEL_UNAVAILABLE",
                    "the chat model returned no content for authorized evidence"
            );
        }
        log.info("knowledge answer generated kbId={} searchMode={} sourceMode={} candidates={} citations={}",
                kbId, searchMode.wireValue(), sourceMode.wireValue(), candidates.size(), evidence.size());
        return projection("ANSWERED", answer, knowledgeCitationConverter.toTargetList(evidence), base);
    }

    /**
     * 中文说明：读取知识库并复核角色，不可见 {@code 404}、角色不足 {@code 403}，顺序固定；
     * 交付前的第二次调用即是撤权屏障，也是「不得把无权限内容送进提示词」的唯一执行点。
     * English summary: Reads the knowledge base and settles the role, {@code 404} when invisible and {@code 403} when the role
     * falls short, in that fixed order; the second call before delivery is both the revocation barrier and the only place
     * where "no unauthorized content reaches the prompt" is enforced.
     * @param actor 参数 已验证的管理身份；parameter the verified management actor.
     * @param kbId 参数 知识库十进制字符串 id；parameter decimal-string knowledge base id.
     * @return 返回 知识库业务载体；returns the knowledge base carrier.
     */
    private KnowledgeBaseBO requireReadableBase(AdminActor actor, String kbId) {
        KnowledgeBaseBO base = knowledgeRepository.findBase(kbId)
                .orElseThrow(() -> new GatewayAdminNotFoundException(
                        "KNOWLEDGE_RESOURCE_NOT_FOUND knowledge base was not found"
                ));
        KnowledgeMemberRoleEnum role = deriveRole(base, actor.actorId());
        if (role == null || !READER_OR_ABOVE.contains(role)) {
            throw new CommonException(
                    403,
                    "KNOWLEDGE_FORBIDDEN",
                    "the presenting identity holds no sufficient role on this knowledge base"
            );
        }
        return base;
    }

    /**
     * 中文说明：交付前的当前性复核：候选所属文档必须仍可见，且其当前活动修订仍等于候选冻结的修订；
     * 任一不成立即丢弃该条证据（并发发布、文档软删、跨库同 id），只保留前 {@code topK} 条存活证据。
     * English summary: The currency check before delivery: a candidate's document must still be visible and its active
     * revision must still equal the frozen revision on the candidate, otherwise that evidence is dropped (a concurrent
     * publication, a soft-deleted document, a same-id document under another base) and only the first {@code topK}
     * survivors are kept.
     * @param candidates 参数 召回候选；parameter the recalled candidates.
     * @param kbId 参数 知识库十进制字符串 id；parameter decimal-string knowledge base id.
     * @param topK 参数 引用条数上限；parameter the citation ceiling.
     * @return 返回 仍然成立的证据；returns the evidence that still holds.
     */
    private List<KnowledgeRetrievalHitBO> retainCurrentSources(
            List<KnowledgeRetrievalHitBO> candidates,
            String kbId,
            int topK) {
        List<KnowledgeRetrievalHitBO> survivors = new ArrayList<>(Math.min(topK, candidates.size()));
        for (KnowledgeRetrievalHitBO candidate : candidates) {
            if (survivors.size() >= topK) {
                break;
            }
            KnowledgeDocumentBO document = knowledgeRepository
                    .findDocument(kbId, candidate.getDocumentId())
                    .orElse(null);
            if (document == null || !Objects.equals(document.getActiveRevisionId(), candidate.getDocumentRevisionId())) {
                log.debug("knowledge citation candidate dropped as no longer current kbId={} chunkId={}",
                        kbId, candidate.getChunkId());
                continue;
            }
            survivors.add(candidate);
        }
        return survivors;
    }

    /**
     * 中文说明：只经本地嵌入别名产出一次查询向量，并要求返回结果恰为一个、长度等于冻结维度；
     * 任何不符都是依赖不可用，绝不截断、补零或换用云端兜底。
     * English summary: Produces one query vector through the local embedding alias only, requiring exactly one result whose
     * length equals the frozen dimensions; any divergence is an unavailable dependency and is never truncated, padded or
     * served by a cloud fallback.
     * @param kbId 参数 知识库十进制字符串 id；parameter decimal-string knowledge base id.
     * @param question 参数 用户问题；parameter the question.
     * @param dimensions 参数 冻结维度；parameter the frozen dimensions.
     * @return 返回 查询向量；returns the query vector.
     */
    private float[] queryVector(String kbId, String question, int dimensions) {
        List<float[]> vectors = knowledgeModelClientService.embed(kbId, List.of(question), dimensions);
        float[] vector = vectors == null || vectors.size() != 1 ? null : vectors.get(0);
        if (vector == null || vector.length != dimensions) {
            throw new CommonException(
                    503,
                    "KNOWLEDGE_MODEL_UNAVAILABLE",
                    "the embedding model returned no query vector in the knowledge base's frozen dimensions"
            );
        }
        return vector;
    }

    /**
     * 中文说明：按注册表选出召回策略；注册表在启动期已保证三个模式键齐备，缺键属于装配被破坏，
     * 直接失败关闭而不退回另一种算法。
     * English summary: Selects the recall strategy from the registry; the registry guarantees all three mode keys at
     * startup, so a missing key means the wiring was broken and fails closed instead of switching algorithm.
     * @param searchMode 参数 召回模式；parameter the recall mode.
     * @return 返回 对应策略；returns the matching strategy.
     */
    private KnowledgeSearchStrategy strategyOf(KnowledgeSearchModeEnum searchMode) {
        KnowledgeSearchStrategy strategy = knowledgeSearchStrategyRegistry.get(searchMode);
        if (strategy == null) {
            throw new IllegalStateException("No knowledge search strategy is registered for mode " + searchMode);
        }
        return strategy;
    }

    /**
     * 中文说明：把问题与存活证据的节选装配为不超过端口上限的提示词，逐条带上文件名与分块标识以便模型指向引用；
     * 超出上限时停止追加证据而不是截断某条正文或压缩成不可追溯的摘要。
     * English summary: Assembles the question and the surviving excerpts into a prompt inside the port's ceiling, labelling
     * each entry with the file name and chunk identifier so the model can point at citations; once the ceiling is reached
     * further evidence is dropped rather than one body truncated or the set collapsed into an untraceable summary.
     * @param question 参数 用户问题；parameter the question.
     * @param evidence 参数 存活证据；parameter the surviving evidence.
     * @return 返回 提示词；returns the prompt.
     */
    private static String prompt(String question, List<KnowledgeRetrievalHitBO> evidence) {
        StringBuilder prompt = new StringBuilder("只能依据下列资料回答，并在结论处标注所用资料编号；资料不足时明确说明未找到依据。\n")
                .append("问题：").append(question).append('\n');
        int index = 1;
        for (KnowledgeRetrievalHitBO hit : evidence) {
            String entry = "资料" + index + "（" + hit.getFileName() + " / 分块 " + hit.getChunkId() + "）："
                    + hit.getContent() + '\n';
            if (prompt.length() + entry.length() > MAX_PROMPT_CHARACTERS) {
                break;
            }
            prompt.append(entry);
            index++;
        }
        return prompt.toString();
    }

    /**
     * 中文说明：装配出站投影；引用为空时答案固定为无依据文案，两种结局都携带本知识库的 chat 别名。
     * English summary: Assembles the outbound projection; an empty citation set carries the fixed unsupported answer text and
     * both outcomes name this knowledge base's chat alias.
     * @param outcome 参数 结局标识；parameter the outcome token.
     * @param answer 参数 回答文案；parameter the answer text.
     * @param citations 参数 引用投影；parameter the citation projections.
     * @param base 参数 知识库业务载体；parameter the knowledge base carrier.
     * @return 返回 答案投影；returns the answer projection.
     */
    private static KnowledgeAnswerVO projection(
            String outcome,
            String answer,
            List<KnowledgeCitationVO> citations,
            KnowledgeBaseBO base) {
        return new KnowledgeAnswerVO()
                .setOutcome(outcome)
                .setAnswer(answer)
                .setCitations(citations)
                .setModel(base.getChatModel());
    }

    /**
     * 中文说明：按字面量解析取证范围，未知值转 {@code 422}；缺省 {@code DOCUMENTS} 由调用方显式决定。
     * English summary: Parses the evidence scope by token, turning an unknown value into {@code 422}; the
     * {@code DOCUMENTS} default is decided here explicitly.
     * @param wireValue 参数 请求里的字面量或 {@code null}；parameter the request token or {@code null}.
     * @return 返回 取证范围；returns the evidence scope.
     */
    private static KnowledgeSourceModeEnum sourceModeOf(String wireValue) {
        if (wireValue == null) {
            return KnowledgeSourceModeEnum.DOCUMENTS;
        }
        try {
            return KnowledgeSourceModeEnum.fromWire(wireValue);
        } catch (IllegalArgumentException invalid) {
            throw invalidSourceMode("sourceMode");
        }
    }

    /**
     * 中文说明：按字面量解析召回模式，未知值转 {@code 422}；缺省 {@code VECTOR}。
     * English summary: Parses the recall mode by token, turning an unknown value into {@code 422} and defaulting to
     * {@code VECTOR}.
     * @param wireValue 参数 请求里的字面量或 {@code null}；parameter the request token or {@code null}.
     * @return 返回 召回模式；returns the recall mode.
     */
    private static KnowledgeSearchModeEnum searchModeOf(String wireValue) {
        if (wireValue == null) {
            return KnowledgeSearchModeEnum.VECTOR;
        }
        try {
            return KnowledgeSearchModeEnum.fromWire(wireValue);
        } catch (IllegalArgumentException invalid) {
            throw invalidSourceMode("searchMode");
        }
    }

    /**
     * 中文说明：未知模式字面量的统一失败，指明字段而不回落到默认算法。
     * English summary: The uniform failure of an unknown mode token, naming the field instead of falling back to a default
     * algorithm.
     * @param field 参数 字段名；parameter the field name.
     * @return 返回 校验异常；returns the validation failure.
     */
    private static CommonException invalidSourceMode(String field) {
        return new CommonException(
                422,
                "KNOWLEDGE_VALIDATION_FAILED",
                "the " + field + " token is not a declared knowledge retrieval mode"
        );
    }

    /**
     * 中文说明：核对检索预算（单调时钟），超时按依赖不可用收束；不放宽授权谓词、不做全表兜底。
     * English summary: Checks the retrieval budget against the monotonic clock, closing out as an unavailable dependency on a
     * breach without relaxing an authorization predicate or scanning the whole table.
     * @param deadlineAt 参数 预算终点；parameter the deadline.
     */
    private static void checkBudget(long deadlineAt) {
        if (System.nanoTime() >= deadlineAt) {
            throw new CommonException(
                    503,
                    "KNOWLEDGE_MODEL_UNAVAILABLE",
                    "the retrieval budget was exceeded"
            );
        }
    }

    /**
     * 中文说明：派生当前主体的角色，口径与 {@code KnowledgeServiceImpl} 一致：owner 命中即 OWNER，
     * 否则取该主体在 {@code members} 里角色最高的一条，都不匹配返回 {@code null} 表示完全无权限。
     * English summary: Derives the presenting identity's role on the same reading as {@code KnowledgeServiceImpl}: the owner
     * yields OWNER, otherwise the highest listed role held by this actor applies and no match yields {@code null}, meaning
     * no access at all.
     * @param base 参数 知识库业务载体；parameter the knowledge base carrier.
     * @param actorId 参数 主体标识；parameter the actor identifier.
     * @return 返回 角色或 {@code null}；returns the role or {@code null}.
     */
    private static KnowledgeMemberRoleEnum deriveRole(KnowledgeBaseBO base, String actorId) {
        if (StringUtils.equals(base.getOwnerActorId(), actorId)) {
            return KnowledgeMemberRoleEnum.OWNER;
        }
        KnowledgeMemberRoleEnum derived = null;
        for (KnowledgeMemberDTO member : membersOf(base)) {
            if (!StringUtils.equals(member.getActorId(), actorId) || member.getRole() == null) {
                continue;
            }
            if (derived == null || member.getRole().getCode() > derived.getCode()) {
                derived = member.getRole();
            }
        }
        return derived;
    }

    /**
     * 中文说明：安全读取成员列表，缺失按空集处理（空集即无成员授权，仍会因角色为 {@code null} 而 403）。
     * English summary: Reads the member list defensively, an absent list behaving as empty, which still yields {@code 403}
     * because no role can be derived.
     * @param base 参数 知识库业务载体；parameter the knowledge base carrier.
     * @return 返回 成员列表；returns the members.
     */
    private static List<KnowledgeMemberDTO> membersOf(KnowledgeBaseBO base) {
        List<KnowledgeMemberDTO> members = base.getMembers();
        return members == null ? List.of() : members;
    }
}
