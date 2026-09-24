package top.egon.cola.component.yuheng.admin.knowledge.service;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * 中文说明：{@code KnowledgeModelClientService} 是知识库访问同一企业 engine 的模型端口，
 * 只有两个能力：按知识库冻结的 embedding alias 取本地向量，以及按该知识库的 chat alias 做接地生成。
 * 它刻意不引入任何厂商 SDK——实现用 JDK {@code java.net.http.HttpClient} 调用
 * {@code POST /v1/embeddings} 与 {@code POST /v1/chat/completions}，SERVICE token 只从配置的文件/环境变量引用解析，
 * 绝不出现在代码、日志或响应里；{@code baseUrl} 等强制配置为空时首次使用即失败关闭，不采用任何宽松默认地址。
 * English summary: {@code KnowledgeModelClientService} is the knowledge port onto the same enterprise engine, offering
 * exactly two capabilities: local vectors through the knowledge base's frozen embedding alias and grounded generation
 * through its chat alias. It deliberately pulls in no vendor SDK — the implementation uses the JDK
 * {@code java.net.http.HttpClient} against {@code POST /v1/embeddings} and {@code POST /v1/chat/completions}, resolves the
 * SERVICE token only from the configured file or environment reference so it never appears in code, logs or a response, and
 * fails closed at first use when mandatory configuration such as {@code baseUrl} is blank instead of adopting a permissive
 * default address.
 *
 * 用法 / Usage: 由 {@code DocumentIngestionStrategy}（批量 ≤ 64）与 Step 13 的检索实现调用（bean 名
 * {@code knowledgeModelClientServiceImpl}）；两个方法都在数据库事务与锁之外使用，
 * 也不在持锁事务中等待上游响应。返回值只承载向量与助手文本，调用方负责把它们留在服务端内部
 * （写分块或组装答案），日志只记 kbId、批次大小、耗时与机器码。
 * 错误契约固定：上游非 2xx、超时、响应体超限或结构不合（向量条数不等于文本数、长度不等于期望维度、
 * 存在非有限或全零向量、助手文本为空）一律 {@code 503 KNOWLEDGE_MODEL_UNAVAILABLE}，
 * 不回显上游响应体也不降级为云端嵌入；文档侧永远没有云端嵌入兜底路径。
 * / Called by {@code DocumentIngestionStrategy} (batches of at most 64) and by the Step 13 retrieval implementation (bean
 * name {@code knowledgeModelClientServiceImpl}); both methods run outside any database transaction and lock, never waiting
 * on an upstream response while holding one. The return values carry only vectors and assistant text, which the caller keeps
 * server-side (chunk persistence or answer assembly), and logging stays on kbId, batch size, timing and machine codes. The
 * error contract is fixed: an upstream non-2xx, a timeout, an over-sized or structurally wrong response (vector count
 * different from the text count, a length different from the expected dimensions, a non-finite or all-zero vector, an empty
 * assistant text) all raise {@code 503 KNOWLEDGE_MODEL_UNAVAILABLE} without echoing the upstream body and without falling
 * back to cloud embedding — documents have no cloud embedding path at all.
 */
@Validated
public interface KnowledgeModelClientService {

    /**
     * 中文说明：执行 embed 操作；只走本地（LOCAL）嵌入 alias 为该知识库的文本批量取向量，
     * 返回值条数必须恰等于 {@code texts.size()} 且与入参同序，每条长度恰等于 {@code expectedDimensions}、
     * 全部有限且非全零——不满足即按依赖不可用失败，绝不补零、截断、复用旧向量或转向云端。
     * {@code expectedDimensions} 是知识库首次上传时冻结的维度，与本知识库既有索引口径必须一致。
     * English summary: Executes the embed operation; batches of text for one knowledge base are embedded strictly through the
     * LOCAL embedding alias, and the result must contain exactly {@code texts.size()} vectors in input order, each exactly
     * {@code expectedDimensions} long, fully finite and not all zero — otherwise the call fails as an unavailable dependency
     * rather than being zero-padded, truncated, served from a stale vector, or routed to a cloud provider.
     * {@code expectedDimensions} is the dimension frozen at the knowledge base's first upload and must match its existing
     * index.
     *
     * 用法 / Usage: {@code knowledgeModelClientServiceImpl.embed(kbId, texts, expectedDimensions)}；
     * 单批至多 64 条文本（与 {@code yuheng.knowledge.embed-batch-size} 同键），摄取按批次失败时旧活动 revision 不变。
     * @param kbId 参数 知识库十进制字符串 id，决定冻结的 alias/维度；parameter decimal-string knowledge base id deciding the
     *             frozen alias and dimensions.
     * @param texts 参数 待嵌入文本，1–64 条且逐条非空；parameter texts to embed, 1 to 64 non-blank entries.
     * @param expectedDimensions 参数 期望向量维度，正整数；parameter expected vector length, a positive number.
     * @return 返回 与入参同序的向量列表；returns the vectors in input order.
     */
    List<float[]> embed(
            @NotBlank
            @Pattern(regexp = "^[1-9][0-9]{0,19}$") String kbId,
            @NotEmpty
            @Size(max = 64)
            List<@NotBlank String> texts,
            @Min(1) int expectedDimensions
    );

    /**
     * 中文说明：执行 generate 操作；通过该知识库配置并复核过的 chat alias 做一次接地生成，
     * 返回原始助手文本，由调用方负责组装答案与引用（本端口不解析结构、也不落库）；
     * 助手文本为空、被截断到无内容或上游异常都如实按依赖不可用失败，绝不清空答案或伪造一段无依据文本。
     * 出域策略（{@code LOCAL_ONLY}/{@code CLOUD_ALLOWED}）与目标别名由实现按知识库判定，
     * 违反出域策略的请求在发出之前即以校验失败拒绝，而不是静默改道。
     * English summary: Executes the generate operation; one grounded generation runs through the knowledge base's configured
     * and re-validated chat alias and the raw assistant text is returned for the caller to assemble into an answer with its
     * citations (this port parses no structure and persists nothing). An empty or fully truncated assistant text, or an
     * upstream failure, fails honestly as an unavailable dependency instead of clearing the answer or fabricating unsupported
     * text. The egress policy ({@code LOCAL_ONLY}/{@code CLOUD_ALLOWED}) and target alias are decided by the implementation
     * per knowledge base, and a request that would violate the policy is rejected as a validation failure before it leaves
     * rather than being silently rerouted.
     *
     * 用法 / Usage: {@code knowledgeModelClientServiceImpl.generate(kbId, prompt, maxTokens)}；
     * 提示词上限 60000 字符且只出现在出站请求里，绝不写日志；本调用总在事务之外发起。
     * @param kbId 参数 知识库十进制字符串 id，决定 chat alias 与出域策略；parameter decimal-string knowledge base id
     *             deciding the chat alias and egress policy.
     * @param prompt 参数 接地提示词，非空且至多 60000 字符；parameter the grounded prompt, non-blank and at most 60000
     *               characters.
     * @param maxTokens 参数 生成上限令牌数，正整数；parameter generation token bound, a positive number.
     * @return 返回 原始助手文本；returns the raw assistant text.
     */
    String generate(
            @NotBlank
            @Pattern(regexp = "^[1-9][0-9]{0,19}$") String kbId,
            @NotBlank
            @Size(max = 60_000) String prompt,
            @Min(1) int maxTokens
    );
}
