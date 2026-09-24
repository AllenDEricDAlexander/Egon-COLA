package top.egon.cola.component.yuheng.admin.llm.repository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.yuheng.admin.llm.domain.bo.LlmChannelBO;
import top.egon.cola.component.yuheng.admin.llm.domain.bo.LlmModelBO;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 中文说明：{@code LlmConfigurationRepository} 是渠道与模型配置的业务端口，只声明同域 Service 真正需要的具名读取与命令：
 * 活跃行的匹配总数、{@code create_time DESC, id DESC} 的稳定分页当页、按业务 key 的单行读取、
 * 有界 key-IN 批量读取、按路由渠道反查引用它的模型，以及以 {@code channelKey}/{@code modelKey} 加上载体携带的
 * 期望 {@code revision} 为条件的完整保存；它不继承任何泛型 CRUD，也不把
 * {@code LlmChannelPO}/{@code LlmModelPO} 行模型泄漏到签名上。
 * English summary: {@code LlmConfigurationRepository} is the business port for channel and model configuration, declaring
 * only the named reads and commands the same-domain service uses: the matched count of active rows, the page slice of the
 * stable {@code create_time DESC, id DESC} order, the single active row by business key, a bounded key-IN batch read, the
 * reverse lookup of the models routing at one channel, and a full save conditioned on {@code channelKey}/{@code modelKey}
 * plus the expectation the carrier already carries in {@code revision}; it inherits no generic CRUD and never leaks the
 * {@code LlmChannelPO} or {@code LlmModelPO} row model into a signature.
 *
 * 用法 / Usage: 由 {@code MpLlmConfigurationRepository}（bean 名 {@code llmConfigurationRepository}）在调用方
 * {@code gatewayTransactionManager} 事务内以受守卫的 MP 读写实现；租户与操作者取自守卫上下文而不是入参，
 * 因此本端口没有任何自报租户的入口。读取未命中返回空 {@code Optional}/空列表，分页只返回当页载体而总数由
 * {@link #countChannels()}/{@link #countModels()} 独立给出（与旧 SQL 的 {@code count(*)} 与当页查询同口径两次访问）；
 * 保存返回的是服务端推进后的权威载体，修订不匹配或 0 行写入按
 * {@code GatewayAdminRevisionConflictException} 抛出而不伪造成功。/ {@code MpLlmConfigurationRepository} (bean name
 * {@code llmConfigurationRepository}) implements it through guarded MyBatis-Plus reads and writes inside the caller's
 * {@code gatewayTransactionManager} transaction; tenancy and operator come from the guarded context rather than an
 * argument, so the port has no self-reported tenant at all. A missed read is an empty {@code Optional} or list and a page
 * read yields the slice only, with the matched total contributed separately by {@link #countChannels()}/{@link
 * #countModels()} exactly as the legacy {@code count(*)} plus page statement pair did. A save returns the carrier with the
 * server-advanced revision, while a revision mismatch or a zero-row effect raises
 * {@code GatewayAdminRevisionConflictException} instead of a fake success.
 */
@Validated
public interface LlmConfigurationRepository {

    /**
     * 中文说明：执行 countChannels 操作；给出当前租户活跃渠道集合的匹配总数，与 {@link #findChannelPage(int, int)}
     * 同谓词口径，供分页响应携带总条数。
     * English summary: Executes the countChannels operation; reports the matched total of the current tenant's active channel
     * set under the same predicate as {@link #findChannelPage(int, int)} so the page response can carry a real total.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code llmConfigurationRepository.countChannels()}；不触发健康探测，也不解析任何密钥。
     * @return 返回 活跃渠道总数；returns the number of active channels.
     */
    long countChannels();

    /**
     * 中文说明：执行 findChannelPage 操作；按原 Spec §11.2.1 的渠道分页访问式读取活跃行的当页，
     * 排序固定为 {@code create_time DESC, id DESC}（并列时间戳由主键稳定），不返回总数。
     * English summary: Executes the findChannelPage operation; reads the active page slice with the channel page access pattern
     * of Spec §11.2.1 in the fixed {@code create_time DESC, id DESC} order, where equal timestamps are stabilised by the
     * primary key, and reports no total.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code llmConfigurationRepository.findChannelPage(page, size)}；page 从 1 开始，
     * size 有效范围 1–100，空页返回 {@code []}，总数由 {@link #countChannels()} 配对给出。
     * @param page 参数 页码，从 1 开始；parameter one-based page number.
     * @param size 参数 页大小，1–100；parameter effective page size.
     * @return 返回 当页渠道业务载体；returns the channel carriers of that page.
     */
    List<LlmChannelBO> findChannelPage(
            @Min(1) int page,
            @Min(1)
            @Max(100) int size
    );

    /**
     * 中文说明：执行 findChannel 操作；按 {@code channel_key} 在活跃集合内定位至多一行，
     * 软删行与跨租户行一律视为不存在。
     * English summary: Executes the findChannel operation; locates at most one active row by {@code channel_key}, treating a
     * soft-deleted or foreign-tenant row as absent.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code llmConfigurationRepository.findChannel(channelKey)}；
     * 保存路径用它区分创建意图（{@code revision = 0} 且为空）与完整替换。
     * @param channelKey 参数 渠道稳定 key；parameter channel stable key.
     * @return 返回 渠道业务载体；returns the channel carrier when present.
     */
    Optional<LlmChannelBO> findChannel(
            @NotBlank
            @Pattern(regexp = "^[a-z0-9][a-z0-9-]{0,63}$") String channelKey
    );

    /**
     * 中文说明：执行 findChannelsByKeys 操作；模型保存的 route 渠道存在性/协议/deployment 复核按原合同的
     * key-IN 批量访问一次读齐，输入上限 64，结果不保证次序，未命中的 key 不在结果里。
     * English summary: Executes the findChannelsByKeys operation; the route existence, protocol and deployment checks of a model
     * save read every referenced channel in one key-IN batch as the original contract prescribes, bounded at 64 keys,
     * unordered, and a key that misses simply stays absent from the result.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code llmConfigurationRepository.findChannelsByKeys(channelKeys)}；
     * 空集合按参数校验拒绝，重复 key 由实现去重后只读一次。
     * @param channelKeys 参数 渠道稳定 key 集合，至多 64；parameter channel stable keys, at most 64.
     * @return 返回 命中的渠道业务载体列表；returns the matched channel carriers.
     */
    List<LlmChannelBO> findChannelsByKeys(
            @NotEmpty
            @Size(max = 64)
            Collection<@NotBlank @Pattern(regexp = "^[a-z0-9][a-z0-9-]{0,63}$") String> channelKeys
    );

    /**
     * 中文说明：执行 saveChannel 操作；等价于原 {@code UPDATE gateway_llm_channel SET ... WHERE channel_key = :key AND
     * revision = :expected}：同事务内先读活跃行，缺失且载体 {@code revision = 0} 时以权威 {@code revision = 1} 走受守卫
     * 插入（租户、审计列与技术 version 由边界补齐，唯一键竞争按冲突如实抛出），存在则要求其 revision 与载体一致后
     * 沿用原行技术 {@code id}/{@code version} 以 {@code revision + 1} 做乐观锁 CAS；CAS 失败或 0 行影响按
     * {@code GatewayAdminRevisionConflictException} 携带库中现值抛出，成功时把权威 revision 与审计时刻回写进入参载体。
     * English summary: Executes the saveChannel operation, the equivalent of the legacy
     * {@code UPDATE gateway_llm_channel SET ... WHERE channel_key = :key AND revision = :expected}: within the caller's
     * transaction it loads the active row, performs a guarded insert at the authoritative {@code revision = 1} when the row
     * is absent and the carrier's revision is the zero create sentinel (the boundary stamps tenant, audit and the technical
     * version, and a unique-key race surfaces honestly as a conflict), and otherwise requires the stored revision to equal
     * the carrier's before an optimistic-lock CAS reusing the original {@code id}/{@code version} advances it by one; a
     * failed compare-and-set or a zero-row effect raises {@code GatewayAdminRevisionConflictException} carrying the stored
     * revision, and success syncs the authoritative revision plus the audit instants back into the given carrier.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code llmConfigurationRepository.saveChannel(channel)}；入参载体的
     * {@code revision} 是调用方期望值，返回体是同一载体实例但携带服务端推进后的权威值；
     * 路径 key 与命令 key 的一致性由业务入口先行复核。
     * @param channel 参数 渠道完整保存载体；parameter the full channel save carrier.
     * @return 返回 已提交的渠道业务载体；returns the committed channel carrier.
     */
    LlmChannelBO saveChannel(@Valid @NotNull LlmChannelBO channel);

    /**
     * 中文说明：执行 findModelsByChannelKey 操作；渠道侧 LOCAL embedding 反向引用复核按 {@code routes} 绑定反查
     * 引用该渠道的模型；库内 {@code routes} 是 jsonb 列，受守卫 DAO 未提供 JSON 包含谓词，因此实现按
     * {@code create_time DESC, id DESC} 有界分页扫描活跃模型行并在载体解码后比对路由 key，超过扫描上界如实失败，
     * 绝不把未扫全当成无反向引用。
     * English summary: Executes the findModelsByChannelKey operation; the channel-side LOCAL embedding reverse-reference check
     * finds the models whose {@code routes} bindings address this channel. Because {@code routes} is a jsonb column and the
     * guarded DAO offers no JSON containment predicate, the implementation scans active model rows in bounded
     * {@code create_time DESC, id DESC} pages and compares the routed key on the decoded carrier, failing honestly once the
     * scan bound is exceeded rather than reading an unfinished scan as “no reverse reference”.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code llmConfigurationRepository.findModelsByChannelKey(channelKey)}；
     * 无引用时返回空列表。
     * @param channelKey 参数 渠道稳定 key；parameter channel stable key.
     * @return 返回 引用该渠道的模型业务载体列表；returns the model carriers routing at this channel.
     */
    List<LlmModelBO> findModelsByChannelKey(
            @NotBlank
            @Pattern(regexp = "^[a-z0-9][a-z0-9-]{0,63}$") String channelKey
    );

    /**
     * 中文说明：执行 countModels 操作；给出当前租户活跃模型集合的匹配总数，与 {@link #findModelPage(int, int)} 同口径。
     * English summary: Executes the countModels operation; reports the matched total of the current tenant's active model set
     * under the same predicate as {@link #findModelPage(int, int)}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code llmConfigurationRepository.countModels()}。
     * @return 返回 活跃模型总数；returns the number of active models.
     */
    long countModels();

    /**
     * 中文说明：执行 findModelPage 操作；按原 Spec §11.2.2 的模型分页访问式读取活跃模型的当页，
     * 排序与总数口径与渠道一致。
     * English summary: Executes the findModelPage operation; reads the active model page slice with the Spec §11.2.2 page access
     * pattern under the same ordering and total-count rules as the channel page.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code llmConfigurationRepository.findModelPage(page, size)}；
     * 三个 jsonb 列在此解码为结构化集合，空页返回 {@code []}。
     * @param page 参数 页码，从 1 开始；parameter one-based page number.
     * @param size 参数 页大小，1–100；parameter effective page size.
     * @return 返回 当页模型业务载体；returns the model carriers of that page.
     */
    List<LlmModelBO> findModelPage(
            @Min(1) int page,
            @Min(1)
            @Max(100) int size
    );

    /**
     * 中文说明：执行 findModel 操作；按 {@code model_key} 在活跃集合内定位至多一行。
     * English summary: Executes the findModel operation; locates at most one active row by {@code model_key}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code llmConfigurationRepository.findModel(modelKey)}。
     * @param modelKey 参数 模型稳定 key；parameter model stable key.
     * @return 返回 模型业务载体；returns the model carrier when present.
     */
    Optional<LlmModelBO> findModel(
            @NotBlank
            @Pattern(regexp = "^[a-z0-9][a-z0-9-]{0,63}$") String modelKey
    );

    /**
     * 中文说明：执行 saveModel 操作；与渠道保存共用同一套创建/替换 CAS 语义，作用在 {@code model_key} 上，
     * 并把 {@code protocols}/{@code allowedSubjects}/{@code routes} 三个 jsonb 列作为结构化业务列整行替换；
     * 已绑定嵌入空间的模型不得改变 {@code embeddingSpaceId}/{@code dimensions} 这条规则由业务入口复核，
     * 仓储不静默放宽，权威 revision 与审计时刻同样回写进入参载体。
     * English summary: Executes the saveModel operation; it shares the create-versus-replace CAS semantics of the channel save
     * on {@code model_key} and replaces the three jsonb columns {@code protocols}/{@code allowedSubjects}/{@code routes} as
     * structured business columns. The rule that an already bound model keeps its {@code embeddingSpaceId} and
     * {@code dimensions} is re-checked by the owning service rather than silently relaxed here, and the authoritative
     * revision plus audit instants are likewise synced back into the given carrier.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code llmConfigurationRepository.saveModel(model)}。
     * @param model 参数 模型完整保存载体；parameter the full model save carrier.
     * @return 返回 已提交的模型业务载体；returns the committed model carrier.
     */
    LlmModelBO saveModel(@Valid @NotNull LlmModelBO model);
}
