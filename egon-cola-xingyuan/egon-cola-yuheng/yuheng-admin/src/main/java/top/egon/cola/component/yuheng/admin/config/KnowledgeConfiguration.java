package top.egon.cola.component.yuheng.admin.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import top.egon.cola.component.rag.api.RagExtractionService;
import top.egon.cola.component.rag.autoconfigure.RagAutoConfiguration;
import top.egon.cola.component.rag.chunk.MarkdownHeadingRagChunkingStrategy;
import top.egon.cola.component.rag.chunk.RagChunkingStrategy;
import top.egon.cola.component.rag.chunk.RagChunkingStrategyFactory;
import top.egon.cola.component.rag.chunk.RecursiveRagChunkingStrategy;
import top.egon.cola.component.rag.chunk.TokenRagChunkingStrategy;
import top.egon.cola.component.rag.execution.RagExtractionServiceImpl;
import top.egon.cola.component.rag.extract.MarkdownRagDocumentExtractor;
import top.egon.cola.component.rag.extract.PdfRagDocumentExtractor;
import top.egon.cola.component.rag.extract.PlainTextRagDocumentExtractor;
import top.egon.cola.component.rag.extract.RagDocumentExtractor;
import top.egon.cola.component.rag.extract.RagDocumentExtractorRegistry;
import top.egon.cola.component.rag.extract.TikaRagDocumentExtractor;
import top.egon.cola.component.yuheng.admin.config.properties.KnowledgeModelClientProperties;
import top.egon.cola.component.yuheng.admin.config.properties.KnowledgeProperties;
import top.egon.cola.component.yuheng.admin.knowledge.domain.enums.KnowledgeJobTypeEnum;
import top.egon.cola.component.yuheng.admin.knowledge.service.KnowledgeJobStrategy;

import java.time.Clock;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 中文说明：{@code KnowledgeConfiguration} 是知识摄取面的装配点，只承担四项被批准的职责：
 * ① 用 {@link EnableConfigurationProperties} 注册 {@link KnowledgeProperties} 与
 * {@link KnowledgeModelClientProperties}，让<b>边界值</b>只有这一个来源；② 固定
 * {@code knowledgeJobStrategyRegistry}——由容器里的全部 {@link KnowledgeJobStrategy} 归出的<b>不可变
 * {@code EnumMap}</b>，同一 {@code type()} 出现两个实现即在启动期 {@link IllegalStateException} 失败关闭，
 * 因为 Rule 9 规定类型分发只有这一条通路，注册表若在运行期可变就会让「静默改路由」成为可能；
 * ③ 建立有界的 {@code knowledgeJobTaskExecutor}（核心=最大={@code worker-concurrency}，队列 32），并且
 * <b>只在</b> {@code yuheng.knowledge.enabled} 与 {@code yuheng.knowledge.worker-enabled} 同时为真时创建它；
 * ④ <b>显式复用</b> {@code egon-cola-component-rag-starter} 的解析/分块 Bean（下面按类型逐个声明，
 * 只引用组件自己的类型，绝不复制其逻辑），同时让整体 RAG 自动配置保持关闭。
 * 关于第 ④ 项的准确含义：被关闭的是
 * {@link RagAutoConfiguration}（{@code top.egon.cola.component.rag.autoconfigure.RagAutoConfiguration}），
 * 它自带 {@code @ConditionalOnProperty(prefix = "egon.cola.component.rag", name = "enabled",
 * havingValue = "true", matchIfMissing = false)}，因此在没有任何 YAML 的缺省部署里整个 profile——
 * {@code ragProperties}、{@code ragVectorStore}、{@code ragEmbeddingModelRegistry}、
 * {@code ragIngestionService}、{@code ragRetrievalService}、{@code ragDocumentStorage}、
 * {@code ragVectorStoreProbe}——都不成立，这正确性上是必需的：知识面的向量只经 LOCAL 嵌入 alias 由
 * {@code KnowledgeModelClientService} 产生并写进 {@code gateway_knowledge_chunk.embedding}，
 * 组件那套 Spring AI {@code VectorStore}/{@code EmbeddingModel} 若同时成立就会造出第二份向量事实与第二条
 * 出网通路（Spec 明令禁止云端兜底）。这里<b>不</b>用 {@code spring.autoconfigure.exclude} 去排除它，因为
 * 那需要写 YAML，而本 Step 的配置一律留给使用者在启动时补充；改用「组件自己的开关默认关闭 + 本类以组件
 * 原名的同名 Bean 复用」这条无配置通路：本类声明的每个 Bean 都沿用 {@link RagAutoConfiguration} 里的 Bean
 * <em>名字与类型</em>（{@code plainTextRagDocumentExtractor}、{@code ragExtractionService}、
 * {@code tokenRagChunkingStrategy}、{@code ragChunkingStrategyFactory} 等），所以即便使用者日后把
 * {@code egon.cola.component.rag.enabled} 打开，自动配置里那些
 * {@code @ConditionalOnMissingBean(name = ...)} / {@code @ConditionalOnMissingBean(具体类型.class)} 也会
 * 逐个退让，既不会重复注册，也不会因为多出一个同类型策略而触发
 * {@code RagChunkingStrategyFactory} 的「two chunking strategies」启动失败——自动配置总在使用者 Bean
 * <em>之后</em>处理，这也是这些 Bean 必须写在普通 {@code @Configuration} 而不是照抄自动配置的原因。
 * 本类<b>不</b>新建平行 bootstrap、<b>不</b>声明数据源/事务管理器/{@code SqlSessionFactory}，也<b>不</b>重复
 * 「全上下文恰好一个业务 {@code dataSource} 与一个事务管理器、且无 JPA/Flyway 运行期」这一断言：那是
 * {@link GatewayPersistenceConfiguration#afterSingletonsInstantiated()} 已经无条件把守的唯一事实来源，
 * 而 {@code yuheng.persistence.*}（安装租户、身份租户、服务审计主体、期望 schema 版本/指纹）本身就无默认值，
 * 缺失即在绑定阶段失败关闭；另起一套平行装配只会绕开这些守卫。唯一额外的 fail-closed 是
 * {@code knowledgeJobTaskExecutor}：创建它的条件同时证明 worker 已开启，于是顺手要求模型客户端可用
 * （{@link KnowledgeModelClientProperties#requireUsable()}），以免进程起来后立刻认领一批注定失败的任务。
 * English summary: {@code KnowledgeConfiguration} is the wiring point of the knowledge ingestion face and carries exactly
 * four approved duties: (1) {@link EnableConfigurationProperties} over {@link KnowledgeProperties} and
 * {@link KnowledgeModelClientProperties} so the boundary values have one and only one source; (2) the fixed
 * {@code knowledgeJobStrategyRegistry}, an <b>unmodifiable {@code EnumMap}</b> resolved from every
 * {@link KnowledgeJobStrategy} in the container where two implementations sharing one {@code type()} fail startup with an
 * {@link IllegalStateException}, because Rule 9 makes this registry the only type dispatch and a mutable registry would
 * re-open "silently re-route a job type" at runtime; (3) the bounded {@code knowledgeJobTaskExecutor}
 * (core = max = {@code worker-concurrency}, queue 32) created <b>only</b> while {@code yuheng.knowledge.enabled} and
 * {@code yuheng.knowledge.worker-enabled} are both true; and (4) the <b>explicit reuse</b> of the
 * {@code egon-cola-component-rag-starter} parser and chunker beans, declared below by the component's own types and never by
 * copying their logic, while the RAG auto-configuration as a whole stays disabled. What exactly is disabled for (4):
 * {@link RagAutoConfiguration}
 * ({@code top.egon.cola.component.rag.autoconfigure.RagAutoConfiguration}), which is itself gated by
 * {@code @ConditionalOnProperty(prefix = "egon.cola.component.rag", name = "enabled", havingValue = "true",
 * matchIfMissing = false)}, so in a default deployment with no YAML its whole profile — {@code ragProperties},
 * {@code ragVectorStore}, {@code ragEmbeddingModelRegistry}, {@code ragIngestionService},
 * {@code ragRetrievalService}, {@code ragDocumentStorage}, {@code ragVectorStoreProbe} — never exists. That is required for
 * correctness: the knowledge face produces vectors only through the LOCAL embedding alias via
 * {@code KnowledgeModelClientService} and writes them into {@code gateway_knowledge_chunk.embedding}, so the component's
 * Spring AI {@code VectorStore}/{@code EmbeddingModel} pair existing alongside it would create a second vector fact and a
 * second egress path, which the Spec forbids outright (no cloud fallback). {@code spring.autoconfigure.exclude} is
 * <b>not</b> used, because that would mean writing YAML while this Step leaves all configuration to the operator at
 * startup; instead a no-configuration route is taken — "the component's own switch is off by default, and this class reuses
 * those beans under the component's own names". Every bean declared here keeps the <em>name and type</em>
 * {@link RagAutoConfiguration} uses ({@code plainTextRagDocumentExtractor}, {@code ragExtractionService},
 * {@code tokenRagChunkingStrategy}, {@code ragChunkingStrategyFactory} and so on), so even an operator who later turns
 * {@code egon.cola.component.rag.enabled} on finds each of that class's
 * {@code @ConditionalOnMissingBean(name = ...)} / {@code @ConditionalOnMissingBean(concrete type)} guards stepping aside:
 * nothing is registered twice, and {@code RagChunkingStrategyFactory} cannot trip its "two chunking strategies" startup
 * failure over an extra strategy of the same enum value — auto-configurations are always processed <em>after</em> user
 * beans, which is precisely why these beans live in an ordinary {@code @Configuration} rather than in a copied
 * auto-configuration. This class creates <b>no</b> parallel bootstrap and declares <b>no</b> DataSource, transaction
 * manager or {@code SqlSessionFactory}, and it does <b>not</b> re-assert "exactly one business {@code dataSource} and one
 * transaction manager, with no JPA or Flyway runtime" — {@link GatewayPersistenceConfiguration#afterSingletonsInstantiated()}
 * is already the single unconditional owner of that invariant, and {@code yuheng.persistence.*} (installation tenant,
 * identity tenant, service audit principal, expected schema version and fingerprint) carries no defaults at all, so a
 * missing identity value already fails closed during binding; a parallel wiring point would only bypass those guards. The
 * one extra fail-closed sits in {@code knowledgeJobTaskExecutor}: the condition that creates it also proves the worker is
 * on, so it additionally demands a usable model client
 * ({@link KnowledgeModelClientProperties#requireUsable()}) rather than starting a process that would immediately claim
 * jobs it can never finish.
 *
 * 用法 / Usage: 由 {@code GatewayAdminApplication} 的包扫描装配；{@code knowledgeJobStrategyRegistry} 与
 * {@code knowledgeJobTaskExecutor} 供 {@code KnowledgeJobServiceImpl} 与 {@code KnowledgeJobWorker} 按 bean 名限定注入，
 * 下面每个 RAG Bean 供 {@code DocumentIngestionStrategy} 按 bean 名限定注入。
 */
@Slf4j
@Configuration(value = "knowledgeConfiguration", proxyBeanMethods = false)
@EnableConfigurationProperties({KnowledgeProperties.class, KnowledgeModelClientProperties.class})
public class KnowledgeConfiguration {

    /** 中文说明：worker 执行器的队列容量，与 Spec 的「worker 并发默认 2，队列容量 32」逐字相同；有界是硬要求，无界队列会让积压吃满堆。 English summary: the queue capacity of the worker executor, verbatim from the Spec's "worker concurrency default 2, queue capacity 32"; boundedness is mandatory because an unbounded queue lets the backlog eat the heap. */
    private static final int WORKER_QUEUE_CAPACITY = 32;

    /** 中文说明：worker 执行器的线程名前缀，日志与线程 dump 里据此唯一认出摄取线程；稳定且不含业务语义，改名即失去可比性。 English summary: the thread name prefix of the worker executor, which is how logs and thread dumps recognise the ingestion threads uniquely; it is stable and semantics-free, so renaming it costs comparability. */
    private static final String WORKER_THREAD_NAME_PREFIX = "yuheng-knowledge-worker-";

    /**
     * 中文说明：把容器里全部 {@link KnowledgeJobStrategy} 归成不可变的 {@code 作业类型 → Strategy} 注册表，
     * 这是 Rule 9 下唯一的类型分发事实来源：任何协作者要执行一个类型就只能查这张表，写不出
     * {@code switch (type)}。三条启动期不变量：条目必须声明类型（{@code null} 类型永不可达，视同重复的反面）；
     * 同一类型只允许一个实现（重复即 {@link IllegalStateException}，绝不静默覆盖，因为「谁赢」会取决于 bean
     * 顺序）；注册表不得为空（空表意味着没有任何类型可分发，配置本身已经坏了）。
     * 刻意<b>不</b>要求覆盖 {@link KnowledgeJobTypeEnum} 的全部常量：{@code WIKI_GENERATE} 属于 Step 14，
     * 已认领类型在表内缺失时由 {@code KnowledgeJobServiceImpl} 收口为终态
     * {@code 503 KNOWLEDGE_STRATEGY_UNAVAILABLE} 而不是静默丢弃，这与本类的启动期检查是两条互补而非替代的路径。
     * 返回 {@code Collections.unmodifiableMap} 包装的 {@code EnumMap}：键空间固定、查找 O(1)，且发布后
     * 任何人都不可能对它做结构性修改。
     * English summary: Files every {@link KnowledgeJobStrategy} in the container into the immutable
     * {@code job type → Strategy} registry, which is the single source of truth for type dispatch under Rule 9: any
     * collaborator executing a job type must look this table up, so no {@code switch (type)} can be written. Three startup
     * invariants: an entry must declare its type (a null type is unreachable and is the mirror image of a duplicate), one
     * type admits one implementation (a duplicate raises {@link IllegalStateException} and is never silently overwritten,
     * because "who wins" would then depend on bean order), and the registry may not be empty (an empty table dispatches
     * nothing, which means the configuration itself is broken). Coverage of every {@link KnowledgeJobTypeEnum} constant is
     * deliberately <b>not</b> required: {@code WIKI_GENERATE} belongs to Step 14, and a claimed type missing from the map is
     * closed by {@code KnowledgeJobServiceImpl} as the terminal {@code 503 KNOWLEDGE_STRATEGY_UNAVAILABLE} rather than
     * dropped silently, so startup completeness and this check are complementary paths, not substitutes. The returned
     * {@code EnumMap} is wrapped in {@code Collections.unmodifiableMap}: a fixed key space, O(1) lookup, and no structural
     * modification possible for anyone once published.
     *
     * 用法 / Usage: 由 {@code KnowledgeJobServiceImpl}（按 {@code knowledgeJobStrategyRegistry} 限定）与
     * {@code KnowledgeJobWorker} 消费一次注入的只读视图。/ Injected once and read by {@code KnowledgeJobServiceImpl}
     * (qualified by {@code knowledgeJobStrategyRegistry}) and by {@code KnowledgeJobWorker}.
     * @param knowledgeJobStrategies 参数 容器内全部类型策略实现；多元素集合注入因此刻意不带
     *                               {@code @Qualifier}——限定值会把集合注入收窄成单个 bean，与
     *                               {@code LlmApiController} 对 {@code Map<String, T>} 视图的既有处理同构。
     *                               parameter every typed strategy implementation in the container; this is a multi-element
     *                               collection injection and so carries {@code @Qualifier} deliberately absent, since a
     *                               qualifier value would narrow collection injection down to a single bean, exactly as
     *                               {@code LlmApiController} already handles its {@code Map<String, T>} view.
     * @return 返回 不可变的类型到策略注册表；returns the immutable type to strategy registry.
     * @throws IllegalStateException 策略缺类型、同一类型重复或注册表为空；a strategy without a type, a duplicated type, or an empty registry.
     */
    @Bean("knowledgeJobStrategyRegistry")
    public Map<KnowledgeJobTypeEnum, KnowledgeJobStrategy> knowledgeJobStrategyRegistry(
            List<KnowledgeJobStrategy> knowledgeJobStrategies) {
        if (knowledgeJobStrategies == null || knowledgeJobStrategies.isEmpty()) {
            throw new IllegalStateException("The knowledge job strategy registry carries no strategy implementation");
        }
        EnumMap<KnowledgeJobTypeEnum, KnowledgeJobStrategy> resolved = new EnumMap<>(KnowledgeJobTypeEnum.class);
        for (KnowledgeJobStrategy strategy : knowledgeJobStrategies) {
            KnowledgeJobTypeEnum type = strategy == null ? null : strategy.type();
            if (type == null) {
                throw new IllegalStateException("Every knowledge job strategy must declare a KnowledgeJobTypeEnum, got "
                        + (strategy == null ? "null" : strategy.getClass().getName()));
            }
            KnowledgeJobStrategy previous = resolved.put(type, strategy);
            if (previous != null) {
                throw new IllegalStateException("Two knowledge job strategies serve " + type + ": "
                        + previous.getClass().getName() + " and " + strategy.getClass().getName());
            }
        }
        log.info("knowledge job strategy registry checked strategies={} types={}",
                resolved.size(), resolved.keySet());
        return Collections.unmodifiableMap(resolved);
    }

    /**
     * 中文说明：建立有界的摄取执行器：核心与最大线程数都取 {@code yuheng.knowledge.worker-concurrency}（Spec 默认 2），
     * 队列容量固定 32，二者一起构成「同时在途 + 排队」的总上界，因此 {@code KnowledgeJobWorker} 必须先按空闲槽
     * 收口再 claim（每次 claim 不得超过可用槽），而不是靠这个池去吸收超额认领。
     * 关闭时 {@code waitForTasksToCompleteOnShutdown(false)} 是刻意选择：在途任务持有租约，
     * 正确性由租约与 {@code lease_token} 的 CAS 保证，优雅排空既救不回注定过期的租约，只会把关停时间拖过
     * {@code yuheng.knowledge.lease}，让本实例在下线期间继续被别人视为持有者。
     * 这个 Bean 只在 {@code yuheng.knowledge.enabled} 与 {@code yuheng.knowledge.worker-enabled} <b>同时</b>为真时创建，
     * 两把开关都用显式 {@code havingValue = "true"}，既没有静默默认值也不再加运行期守卫，因此 worker 关闭时
     * 执行线程一条都不会存在。方法体里的 {@link KnowledgeModelClientProperties#requireUsable()} 是同一条件带来的
     * 启动期快速失败：能走到这里就说明后台会出网，基址或 SERVICE token 引用缺失必须当场拒绝启动。
     * English summary: Builds the bounded ingestion executor: core and maximum pool size both come from
     * {@code yuheng.knowledge.worker-concurrency} (Spec default 2) next to a queue capacity pinned to 32, and the two
     * together are the ceiling on "in flight plus queued", which is why {@code KnowledgeJobWorker} must clamp to the free
     * slots before claiming (a claim never exceeds the free slots) rather than letting this pool absorb over-claims.
     * {@code waitForTasksToCompleteOnShutdown(false)} is a deliberate choice: an in-flight task holds a lease and
     * correctness rests on that lease plus the {@code lease_token} CAS, so draining gracefully rescues no lease that is
     * already doomed to expire while pushing shutdown past {@code yuheng.knowledge.lease}, during which this instance would
     * still look like the holder to everybody else. The bean exists only while {@code yuheng.knowledge.enabled} and
     * {@code yuheng.knowledge.worker-enabled} are <b>both</b> true, each with an explicit {@code havingValue = "true"}, so
     * there is neither a silent default nor a second runtime guard and no worker thread exists at all while disabled. The
     * {@link KnowledgeModelClientProperties#requireUsable()} call inside the method is the startup fast failure that same
     * condition permits: reaching here already proves the process will egress in the background, so a missing base URL or
     * SERVICE token reference must be refused now rather than after jobs were claimed.
     *
     * 用法 / Usage: 由 {@code KnowledgeJobWorker}（bean 名 {@code knowledgeJobTaskExecutor}）注入并提交认领到的任务。
     * @param knowledgeProperties 参数 已绑定并校验的知识运行边界；parameter the bound and validated knowledge runtime boundary.
     * @param knowledgeModelClientProperties 参数 已绑定的模型客户端边界；parameter the bound model client boundary.
     * @return 返回 已配置的有界执行器；returns the configured bounded executor.
     * @throws IllegalStateException worker 已开启但模型客户端基址或 token 引用为空白；a worker that is switched on while the model client's base URL or token reference is blank.
     */
    @Bean("knowledgeJobTaskExecutor")
    @ConditionalOnProperty(name = {"yuheng.knowledge.enabled", "yuheng.knowledge.worker-enabled"},
            havingValue = "true")
    public ThreadPoolTaskExecutor knowledgeJobTaskExecutor(
            @Qualifier(KnowledgeProperties.BEAN_NAME) KnowledgeProperties knowledgeProperties,
            @Qualifier(KnowledgeModelClientProperties.BEAN_NAME)
            KnowledgeModelClientProperties knowledgeModelClientProperties) {
        knowledgeModelClientProperties.requireUsable();
        int concurrency = knowledgeProperties.getWorkerConcurrency();
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(concurrency);
        executor.setMaxPoolSize(concurrency);
        executor.setQueueCapacity(WORKER_QUEUE_CAPACITY);
        executor.setThreadNamePrefix(WORKER_THREAD_NAME_PREFIX);
        executor.setWaitForTasksToCompleteOnShutdown(false);
        log.info("knowledge job executor bounded to threads={} queue={} lease={} heartbeat={}", concurrency,
                WORKER_QUEUE_CAPACITY, knowledgeProperties.getLease(), knowledgeProperties.getHeartbeat());
        return executor;
    }

    /**
     * 中文说明：知识面唯一的 UTC 时钟：{@code *_at} 列一律是 {@code Instant}，租约到期、退避排程与审计时间戳都必须
     * 与数据库的 {@code now()} 处在同一时区基准上；它也是 {@code RagExtractionServiceImpl} 记录耗时用的时钟，
     * 因此这里以组件原名 {@code ragClock} 之外另立具名 {@code knowledgeClock}，让知识面的时间来源可被独立替换。
     * 之所以每个使用者都按名注入而不依赖类型注入：容器里存在多个 {@code Clock} bean
     * （如 {@code gatewayProjectionClock}），按类型注入会把不属于本域的时刻带进租约计算。
     * English summary: The knowledge face's one UTC clock: every {@code *_at} column is an {@code Instant}, so lease
     * expiry, retry scheduling and audit timestamps must share the database's {@code now()} baseline, and it is also the
     * clock {@code RagExtractionServiceImpl} uses for timings. It is published as its own named {@code knowledgeClock}
     * rather than under the component's {@code ragClock} so the knowledge face keeps an independently replaceable time
     * source. Consumers inject it by name instead of by type because the container already holds several {@code Clock}
     * beans (for instance {@code gatewayProjectionClock}), and a by-type injection would let a foreign instant into lease
     * arithmetic.
     *
     * 用法 / Usage: 由 {@code KnowledgeServiceImpl} 与 {@code KnowledgeJobServiceImpl} 按 {@code knowledgeClock} 限定注入。
     * @return 返回 UTC 系统时钟；returns the system UTC clock.
     */
    @Bean("knowledgeClock")
    public Clock knowledgeClock() {
        return Clock.systemUTC();
    }

    /**
     * 中文说明：显式复用组件的纯文本抽取器（Spec 允许的 TXT/MD 原件走这一条），返回组件自己的具体类型，
     * 这样 {@link RagAutoConfiguration} 的 {@code @ConditionalOnMissingBean(具体类型.class)} 能被本 Bean 准确命中。
     * English summary: Reuses the component's plain-text extractor explicitly (the TXT/MD originals the Spec allows go through
     * it) and returns the component's own concrete type, so {@link RagAutoConfiguration}'s
     * {@code @ConditionalOnMissingBean(concrete type)} guards match this bean precisely.
     *
     * 用法 / Usage: 由 {@link #ragDocumentExtractorRegistry(List)} 以 {@code List<RagDocumentExtractor>} 收齐后路由。
     * @return 返回 无状态抽取器实例；returns the stateless extractor instance.
     */
    @Bean("plainTextRagDocumentExtractor")
    public PlainTextRagDocumentExtractor plainTextRagDocumentExtractor() {
        return new PlainTextRagDocumentExtractor();
    }

    /**
     * 中文说明：显式复用组件的 Markdown 抽取器，保留标题结构作为可信定位元数据（{@code metadata} 只允许放解析器
     * 给出的页码/标题/偏移，绝不放 caller ACL）。 English summary: Reuses the component's Markdown extractor, keeping the heading
     * structure as trusted locator metadata (the {@code metadata} column may hold only parser-supplied page, heading or
     * offset, never a caller ACL).
     *
     * 用法 / Usage: 由 {@link #ragDocumentExtractorRegistry(List)} 收齐后按格式路由。
     * @return 返回 无状态抽取器实例；returns the stateless extractor instance.
     */
    @Bean("markdownRagDocumentExtractor")
    public MarkdownRagDocumentExtractor markdownRagDocumentExtractor() {
        return new MarkdownRagDocumentExtractor();
    }

    /**
     * 中文说明：显式复用组件的 PDF 抽取器（Spec 允许 PDF 且明确「PDF 不运行脚本」，OCR 不作承诺）。
     * 组件侧该类受 {@code @ConditionalOnClass} 保护，而 {@code spring-ai-pdf-document-reader} 已是本模块的编译期依赖，
     * 因此此处可以按具体类型直接声明，不需要再复制一次类存在性判断。
     * English summary: Reuses the component's PDF extractor (the Spec allows PDF, states explicitly that a PDF runs no script
     * and promises no OCR). The component guards that class with {@code @ConditionalOnClass}, but since
     * {@code spring-ai-pdf-document-reader} is already a compile dependency of this module the concrete type can be declared
     * here without copying the class-presence test.
     *
     * 用法 / Usage: 由 {@link #ragDocumentExtractorRegistry(List)} 收齐后按格式路由。
     * @return 返回 无状态抽取器实例；returns the stateless extractor instance.
     */
    @Bean("pdfRagDocumentExtractor")
    public PdfRagDocumentExtractor pdfRagDocumentExtractor() {
        return new PdfRagDocumentExtractor();
    }

    /**
     * 中文说明：显式复用组件的 Tika 抽取器，覆盖 DOCX/XLSX 这类 zip 容器格式（Spec 要求启用 Tika 安全限制、
     * 拒绝宏执行/外部实体/压缩炸弹）。它与 PDF 抽取器的优先级由组件自己的 {@code order()} 决定，本类不重新排序，
     * 因为「哪个抽取器负责哪种格式」是组件事实而不是装配偏好。
     * English summary: Reuses the component's Tika extractor for the zip-container formats DOCX/XLSX, where the Spec demands
     * Tika's safety limits with macros, external entities and compression bombs rejected. Its precedence against the PDF
     * extractor stays the component's own {@code order()} rather than being re-sorted here, because "which extractor owns
     * which format" is a component fact and not a wiring preference.
     *
     * 用法 / Usage: 由 {@link #ragDocumentExtractorRegistry(List)} 收齐后按格式路由。
     * @return 返回 无状态抽取器实例；returns the stateless extractor instance.
     */
    @Bean("tikaRagDocumentExtractor")
    public TikaRagDocumentExtractor tikaRagDocumentExtractor() {
        return new TikaRagDocumentExtractor();
    }

    /**
     * 中文说明：显式复用组件的抽取器路由表：把上面四个抽取器交给组件自己构造，由它按 {@code order()} 排序并在
     * 两个抽取器共享同一优先级同一格式时<b>在构造期</b>失败（那种选择的胜负就会取决于 bean 顺序）。
     * 本类因此不复刻路由、不复刻 MIME 判定，也不新增第五个抽取器。
     * English summary: Reuses the component's extractor routing table explicitly: the four extractors above are handed to the
     * component's own constructor, which orders them by {@code order()} and fails <b>at construction</b> when two share one
     * priority and one format, since such a choice would otherwise be decided by bean order. This class therefore
     * re-implements neither the routing nor the MIME decision nor adds a fifth extractor.
     *
     * 用法 / Usage: 由 {@link #ragExtractionService(RagDocumentExtractorRegistry, Clock)} 消费；
     * {@code DocumentIngestionStrategy} 只注入本 Bean 产出的服务，不直接路由格式。
     * @param ragDocumentExtractors 参数 组件内全部抽取器；多元素集合注入，与上面注册表同理不加
     *                              {@code @Qualifier}。parameter every component extractor; a multi-element collection
     *                              injection, so no {@code @Qualifier} for the same reason as the registry above.
     * @return 返回 组件的路由表；returns the component's routing table.
     */
    @Bean("ragDocumentExtractorRegistry")
    public RagDocumentExtractorRegistry ragDocumentExtractorRegistry(List<RagDocumentExtractor> ragDocumentExtractors) {
        return new RagDocumentExtractorRegistry(ragDocumentExtractors);
    }

    /**
     * 中文说明：显式复用组件的抽取服务：字节流 → 文本 + 可信结构元数据，<b>不</b>分块、<b>不</b>嵌入、
     * <b>不</b>持久化。知识面恰恰需要这条边界——原件与 revision 必须同一事务落库，而解析在事务之外重跑一次会
     * 白烧 CPU，所以摄取先把抽取文本持久化、重试时直接复用，绝不回读原件重解析。
     * 日志纪律由组件自己守住（只记文件名/抽取器名/字符数/耗时），本类不再包装。
     * English summary: Reuses the component's extraction service explicitly: a byte stream into text plus trusted structural
     * metadata, with <b>no</b> chunking, <b>no</b> embedding and <b>no</b> persistence. That is exactly the boundary the
     * knowledge face needs, because the original bytes and the revision must land in one transaction while re-parsing
     * outside that transaction on every retry would burn CPU for nothing, so ingestion persists the extracted text first and
     * reuses it instead of re-reading the original. The logging discipline stays the component's own (file name, extractor
     * name, character count, duration) and is not wrapped here.
     *
     * 用法 / Usage: 由 {@code DocumentIngestionStrategy}（bean 名 {@code ragExtractionService}）限定注入，
     * 在 PARSE 阶段以 {@code RagExtractionCommand} 调用一次。
     * @param ragDocumentExtractorRegistry 参数 组件的抽取器路由表；parameter the component's extractor routing table.
     * @param knowledgeClock 参数 知识面唯一 UTC 时钟；parameter the knowledge face's single UTC clock.
     * @return 返回 组件的抽取服务；returns the component's extraction service.
     */
    @Bean("ragExtractionService")
    public RagExtractionService ragExtractionService(
            @Qualifier("ragDocumentExtractorRegistry") RagDocumentExtractorRegistry ragDocumentExtractorRegistry,
            @Qualifier("knowledgeClock") Clock knowledgeClock) {
        return new RagExtractionServiceImpl(ragDocumentExtractorRegistry, knowledgeClock);
    }

    /**
     * 中文说明：显式复用组件的 token 预算分块策略（{@code chunkSize}/{@code chunkOverlap} 以 token 为单位，
     * 界 32..4096 与被复用组件一致）。策略要求<b>确定性</b>：同文档同配置必须产出同样的块，
     * 因为 {@code (tenant_id, revision_id, chunk_index)} 永久唯一，重试就是覆盖同一批块。
     * 返回类型与被复用组件的守卫逐字对齐：该处是 {@code @ConditionalOnMissingBean(TokenRagChunkingStrategy.class)}
     * 这种按<em>具体类型</em>的判断，而条件求值只看 {@code @Bean} 方法声明的返回类型，所以本类也必须声明具体类型，
     * 否则自动配置认不出本 Bean、会以同一个 bean 名重复注册并让上下文启动失败。
     * English summary: Reuses the component's token-budget chunking strategy ({@code chunkSize}/{@code chunkOverlap} in
     * tokens, bounded 32..4096 exactly as the reused component does). Determinism is a strategy requirement: the same
     * document and configuration must yield the same chunks, because {@code (tenant_id, revision_id, chunk_index)} is unique
     * forever and a retry therefore overwrites that same batch. The declared return type is aligned word for word with the
     * component's guard, which is {@code @ConditionalOnMissingBean(TokenRagChunkingStrategy.class)} on the
     * <em>concrete type</em>, and condition evaluation only sees the declared return type of a {@code @Bean} method — so this
     * class must declare the concrete type too, otherwise the auto-configuration would not recognise this bean, would
     * register a second one under the same bean name, and the context would fail to start.
     *
     * 用法 / Usage: 由 {@link #ragChunkingStrategyFactory(List)} 收齐后按枚举解析。
     * @return 返回 无状态分块策略；returns the stateless chunking strategy.
     */
    @Bean("tokenRagChunkingStrategy")
    public TokenRagChunkingStrategy tokenRagChunkingStrategy() {
        return new TokenRagChunkingStrategy();
    }

    /**
     * 中文说明：显式复用组件的 Markdown 标题分块策略，按冻结的标题层级切分，使块边界与文档结构对齐。
     * 返回类型同样取组件的具体实现，理由与 {@link #tokenRagChunkingStrategy()} 相同：组件那侧的退让条件
     * 是 {@code @ConditionalOnMissingBean(MarkdownHeadingRagChunkingStrategy.class)}。
     * English summary: Reuses the component's Markdown heading chunking strategy, splitting on the frozen heading levels so
     * chunk boundaries line up with the document structure. The concrete implementation is the declared return type for the
     * same reason as in {@link #tokenRagChunkingStrategy()}: the component backs off via
     * {@code @ConditionalOnMissingBean(MarkdownHeadingRagChunkingStrategy.class)}.
     *
     * 用法 / Usage: 由 {@link #ragChunkingStrategyFactory(List)} 收齐后按枚举解析。
     * @return 返回 无状态分块策略；returns the stateless chunking strategy.
     */
    @Bean("markdownHeadingRagChunkingStrategy")
    public MarkdownHeadingRagChunkingStrategy markdownHeadingRagChunkingStrategy() {
        return new MarkdownHeadingRagChunkingStrategy();
    }

    /**
     * 中文说明：显式复用组件的递归分块策略，作为无结构纯文本的确定性兜底切法；「兜底」指的是策略选择上的
     * 缺省项，<b>不是</b>失败后改用云端或跳过切分。返回类型同样取组件的具体实现，与
     * {@code @ConditionalOnMissingBean(RecursiveRagChunkingStrategy.class)} 对齐。
     * English summary: Reuses the component's recursive chunking strategy, the deterministic default cut for unstructured plain
     * text. "Default" here means the fallback in strategy selection only and <b>not</b> a cloud fallback or a skipped split
     * after a failure, and the concrete implementation is again the declared return type to match
     * {@code @ConditionalOnMissingBean(RecursiveRagChunkingStrategy.class)}.
     *
     * 用法 / Usage: 由 {@link #ragChunkingStrategyFactory(List)} 收齐后按枚举解析。
     * @return 返回 无状态分块策略；returns the stateless chunking strategy.
     */
    @Bean("recursiveRagChunkingStrategy")
    public RecursiveRagChunkingStrategy recursiveRagChunkingStrategy() {
        return new RecursiveRagChunkingStrategy();
    }

    /**
     * 中文说明：显式复用组件的分块策略工厂：枚举 → 实现的唯一解析处，重复注册同一枚举值即在构造期抛
     * {@code RagConfigurationException}，缺实现同样是配置失败而不是静默回落。这正是本 Step 需要的性质，
     * 因此本类不复刻注册表，也不使用组件的 {@code ragChunkIdFactory}——知识面的 chunk 主键来自
     * {@code SnowflakeIdGenerator} 并以 {@code (revisionId, chunkIndex)} 表达唯一意图，组件那套
     * {@code documentId:chunkIndex} 派生 id 只服务于它自己的 VectorStore。
     * English summary: Reuses the component's chunking strategy factory explicitly: the only place a strategy enum is resolved
     * to an implementation, where two registrations of the same enum value raise {@code RagConfigurationException} at
     * construction and a missing implementation is likewise a configuration failure rather than a silent fallback — exactly
     * the property this Step needs, so no second registry is copied here. The component's {@code ragChunkIdFactory} is
     * deliberately <b>not</b> reused: a knowledge chunk's primary key comes from {@code SnowflakeIdGenerator} with its
     * uniqueness intent expressed as {@code (revisionId, chunkIndex)}, while that factory's {@code documentId:chunkIndex}
     * derivation exists only for the component's own VectorStore.
     *
     * 用法 / Usage: 由 {@code DocumentIngestionStrategy}（bean 名 {@code ragChunkingStrategyFactory}）注入，
     * 用冻结进 {@code chunking_config} 的策略与参数切分抽取文本。
     * @param ragChunkingStrategies 参数 组件内全部分块策略；多元素集合注入，与上面注册表同理不加
     *                              {@code @Qualifier}。parameter every component chunking strategy; a multi-element
     *                              collection injection, so no {@code @Qualifier} for the same reason as the registry above.
     * @return 返回 组件的策略工厂；returns the component's strategy factory.
     */
    @Bean("ragChunkingStrategyFactory")
    public RagChunkingStrategyFactory ragChunkingStrategyFactory(
            List<RagChunkingStrategy> ragChunkingStrategies) {
        return new RagChunkingStrategyFactory(ragChunkingStrategies);
    }
}
