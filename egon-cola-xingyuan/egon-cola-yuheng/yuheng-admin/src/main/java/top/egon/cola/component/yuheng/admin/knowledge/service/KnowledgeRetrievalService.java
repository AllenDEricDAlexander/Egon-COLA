package top.egon.cola.component.yuheng.admin.knowledge.service;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.yuheng.admin.knowledge.domain.dto.KnowledgeAnswerCommandDTO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.vo.KnowledgeAnswerVO;
import top.egon.cola.component.yuheng.admin.shared.domain.AdminActor;

/**
 * 中文说明：{@code KnowledgeRetrievalService} 是授权检索与可追溯接地问答的业务端口：
 * 在 actor 可见的知识库范围内、只覆盖各文档当前活动 revision 的分块做向量/关键词/混合召回，
 * 再用该知识库的 chat alias 生成带引用的答案。它把“谁能看到什么”与“证据如何成为答案”收在一处，
 * 使越权内容不可能进入提示词，也不可能出现在引用列表里。
 * English summary: {@code KnowledgeRetrievalService} is the business port for authorized retrieval and traceable grounded
 * question answering: it recalls vector, keyword or hybrid candidates only from the chunks of each document's current active
 * revision inside the knowledge bases this actor may see, then generates a cited answer through that knowledge base's chat
 * alias. It concentrates "who may see what" and "how evidence becomes an answer" in one place so unauthorized content can
 * neither reach the prompt nor appear in the citation list.
 *
 * 用法 / Usage: 本端口在 Step 12 先行声明（advance declaration），因为 Step 12 的 {@code KnowledgeController} 拥有
 * API-022 且必须编译；实现由 Step 13 提供，且不得重复创建本文件。Step 12 内由
 * {@code KnowledgeServiceImpl#answer(AdminActor, String, KnowledgeAnswerCommandDTO)} 在完成角色复核后委托调用，
 * 注入名固定 {@code knowledgeRetrievalService}。嵌入向量只经本地 alias 产生（无云端兜底），
 * 检索查询向量也必须落在知识库冻结的嵌入空间与维度上。
 * 错误契约固定：无权读取该知识库 {@code 403 KNOWLEDGE_FORBIDDEN}；知识库/文档/修订不可见
 * {@code 404 KNOWLEDGE_RESOURCE_NOT_FOUND}；命令字段或状态不成立（例如活动索引尚不存在、维度与查询不匹配）
 * {@code 422 KNOWLEDGE_VALIDATION_FAILED}；嵌入或 chat alias 不可用、上游异常
 * {@code 503 KNOWLEDGE_MODEL_UNAVAILABLE}；并发发布导致读取的活动 revision 被替换时按
 * {@code 409 KNOWLEDGE_REVISION_CONFLICT} 如实反馈而不是返回混合口径的答案。
 * / This port is advance-declared in Step 12 because the Step 12 {@code KnowledgeController} owns API-022 and must compile;
 * Step 13 supplies the implementation and must not re-create this file. Inside Step 12 it is delegated to by
 * {@code KnowledgeServiceImpl#answer(AdminActor, String, KnowledgeAnswerCommandDTO)} after the role check, injected as
 * {@code knowledgeRetrievalService}. Embedding vectors come only from the LOCAL alias with no cloud fallback and the query
 * vector must stay inside the knowledge base's frozen embedding space and dimensions. The error contract is fixed: an
 * unreadable knowledge base is {@code 403 KNOWLEDGE_FORBIDDEN}, an invisible knowledge base, document or revision
 * {@code 404 KNOWLEDGE_RESOURCE_NOT_FOUND}, an invalid command field or state (no active index yet, dimensions mismatching
 * the query) {@code 422 KNOWLEDGE_VALIDATION_FAILED}, and an unavailable embedding or chat alias, or an upstream failure,
 * {@code 503 KNOWLEDGE_MODEL_UNAVAILABLE}; when a concurrent publication replaces the active revision being read the answer
 * is honestly a {@code 409 KNOWLEDGE_REVISION_CONFLICT} rather than a mixed-basis response.
 */
@Validated
public interface KnowledgeRetrievalService {

    /**
     * 中文说明：执行 answer 操作；对单个知识库做一次授权检索并生成可追溯答案：
     * 先确认 actor 至少是 READER，再只在活动 revision 的分块上召回，按命令的
     * {@code sourceMode}/{@code searchMode}/{@code topK} 有界取证据，交给 chat alias 生成，
     * 输出的每条引用都必须能回溯到本 actor 可见的文档与修订；没有可用证据时如实返回无依据结果，
     * 绝不生成无引用答案，也不泄漏提示词、向量或原文字节。
     * English summary: Executes the answer operation; performs one authorized retrieval plus traceable answer generation over a
     * single knowledge base: the actor must be at least a READER, candidates are recalled only from active-revision chunks,
     * evidence is bounded by the command's {@code sourceMode}, {@code searchMode} and {@code topK} before the chat alias
     * generates the answer, and every citation must trace back to a document and revision this actor may see. With no usable
     * evidence the result is honestly unsupported rather than an uncited generation, and no prompt, vector or raw byte
     * leaves the response.
     *
     * 用法 / Usage: {@code knowledgeRetrievalServiceImpl.answer(actor, kbId, command)}；
     * 模型调用只在事务之外发起，租约或发布状态的变化不构成本方法写入的依据（本端口不写业务行）。
     * @param actor 参数 已验证的管理身份；parameter the verified management actor.
     * @param kbId 参数 知识库十进制字符串 id；parameter decimal-string knowledge base id.
     * @param command 参数 问答命令；parameter the answer command.
     * @return 返回 带引用的答案投影；returns the grounded answer projection with its citations.
     */
    KnowledgeAnswerVO answer(
            @NotNull AdminActor actor,
            @NotBlank
            @Pattern(regexp = "^[1-9][0-9]{0,19}$") String kbId,
            @Valid @NotNull KnowledgeAnswerCommandDTO command
    );
}
