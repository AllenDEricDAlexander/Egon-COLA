package top.egon.cola.component.yuheng.llm.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import top.egon.cola.component.yuheng.llm.proxy.domain.enums.LlmProtocolEnum;

/**
 * 中文说明：{@code LlmGatewayConfiguration} 是 LLM engine 协议面的装配点，只承担四项被批准的职责：
 * ① 用 {@link MapperScan} 注册 {@code proxy.dao}，因为 {@code LlmGatewayApplication} 不是本 Step 的声明文件、
 * 不得改动，缺了这里新 DAO 根本不会成为 Mapper；② 固定四种同协议 Strategy 的具名注册表，把
 * 「协议 → Strategy bean 名」变成一份不可扩的常量映射，杜绝 {@code if (protocol == X)} 式硬编码分支，
 * 而各协议<b>原生</b>错误对象（Chat/Embeddings/Responses 出 OpenAI error、Messages 出 {@code type=error}）由这些
 * Strategy 自己产出，本类不复用 admin 的业务错误 wrapper，也不在这里猜协议；③ 建立有界的流式输出执行器
 * （线程数来自 {@code yuheng.llm.streaming-threads}，零等待队列，饱和即在提交前拒绝，由协议面翻译成 429）；
 * ④ 以显式顺序注册 Messages 凭据适配器，使其排在既有身份过滤器之前。
 * 本类<b>不</b>声明数据源、{@code SqlSessionFactory} 或第二个事务管理器，也不重复
 * 「单数据源/单会话工厂/单事务管理器且无 JPA/Flyway」这一断言——那已经由 {@link LlmPersistenceConfiguration}
 * 无条件地把关，另起一套平行 bootstrap 只会绕开守卫。
 * English summary: {@code LlmGatewayConfiguration} is the wiring point of the LLM engine's protocol face and carries
 * exactly four approved duties: (1) {@link MapperScan} over {@code proxy.dao}, which is required because
 * {@code LlmGatewayApplication} is not a declared file of this Step and must stay untouched, so without this the new DAOs
 * would never become mappers; (2) the named registry of the four same-protocol Strategies, turning
 * "protocol → Strategy bean name" into a non-extensible constant map so no {@code if (protocol == X)} branch can creep
 * back in, while each protocol's <b>native</b> error object (Chat/Embeddings/Responses emit an OpenAI error object and
 * Messages emit a native {@code type=error} object) is produced by those Strategies rather than by this class, which
 * neither reuses the admin business wrapper nor guesses a protocol; (3) the bounded streaming executor sized by
 * {@code yuheng.llm.streaming-threads} with a zero-wait queue that rejects before submission when saturated, which the
 * protocol face translates into 429; and (4) the Messages credential adapter registered at an explicit order ahead of the
 * existing identity filters. This class declares <b>no</b> DataSource, {@code SqlSessionFactory} or second transaction
 * manager and does not re-assert "one datasource, one session factory, one transaction manager, no JPA or Flyway" —
 * {@link LlmPersistenceConfiguration} already guards that unconditionally, and a parallel bootstrapper would only bypass
 * the guards.
 *
 * 用法 / Usage: 由宿主容器装配；{@code yuheng.llm.*} 缺项或身份/白名单不合法时在绑定阶段失败关闭。
 */
@Slf4j
@RequiredArgsConstructor
@Configuration(value = "llmGatewayConfiguration", proxyBeanMethods = false)
@EnableConfigurationProperties(LlmGatewayProperties.class)
@MapperScan("top.egon.cola.component.yuheng.llm.proxy.dao")
public class LlmGatewayConfiguration {

    /** 中文说明：四种同协议 Strategy 的具名注册表键值，Step 11 的实现必须使用这些 bean 名。 English summary: the named registry entries of the four same-protocol Strategies, which Step 11's implementations must adopt as their bean names. */
    private static final String OPENAI_CHAT_STRATEGY_BEAN = "openAiChatProtocolStrategy";
    private static final String OPENAI_EMBEDDING_STRATEGY_BEAN = "openAiEmbeddingProtocolStrategy";
    private static final String OPENAI_RESPONSES_STRATEGY_BEAN = "openAiResponsesProtocolStrategy";
    private static final String ANTHROPIC_MESSAGES_STRATEGY_BEAN = "anthropicMessagesProtocolStrategy";

    /** 中文说明：本进程运行边界（流式线程数与出网开关）。 English summary: this process's runtime boundary (streaming thread count and the egress switch). */
    @Qualifier(LlmGatewayProperties.BEAN_NAME)
    private final LlmGatewayProperties gatewayProperties;

    /** 中文说明：容器唯一的 Jackson 栈，交给凭据过滤器使用，禁止另起第二套 JSON 实现。 English summary: the container's single Jackson stack, handed to the credential filter so no second JSON implementation can appear. */
    @Qualifier("jacksonObjectMapper")
    private final ObjectMapper objectMapper;

    /**
     * 中文说明：注册四种同协议 Strategy 的固定映射。Step 10 先固定「四协议各自对应哪个具名 bean」，因为
     * {@code LlmProtocolStrategy} SPI 属于 Step 11 的声明文件，此刻引用该类型会让整个模块编译不过；Step 11 把
     * 该 Map 的 value 换成按这些名字限定的 Strategy 引用即可，映射本身不允许增删。
     * English summary: Registers the fixed mapping of the four same-protocol Strategies. Step 10 pins "which named bean answers
     * which protocol", because the {@code LlmProtocolStrategy} SPI is a declared file of Step 11 and referencing that type now
     * would leave the whole module uncompilable; Step 11 only has to turn these values into Strategy references qualified by the
     * same names, and the mapping itself may neither grow nor shrink.
     *
     * 用法 / Usage: 由协议面按 {@code LlmProtocolEnum} 取 bean 名后限定注入；缺项即失败关闭，不回落 Chat。
     * @return 返回 四条不可变映射；returns the immutable four-entry map.
     */
    @Bean("llmProtocolStrategyRegistry")
    public Map<LlmProtocolEnum, String> llmProtocolStrategyRegistry() {
        Map<LlmProtocolEnum, String> registry = new LinkedHashMap<>();
        registry.put(LlmProtocolEnum.OPENAI_CHAT, OPENAI_CHAT_STRATEGY_BEAN);
        registry.put(LlmProtocolEnum.OPENAI_EMBEDDING, OPENAI_EMBEDDING_STRATEGY_BEAN);
        registry.put(LlmProtocolEnum.OPENAI_RESPONSES, OPENAI_RESPONSES_STRATEGY_BEAN);
        registry.put(LlmProtocolEnum.ANTHROPIC_MESSAGES, ANTHROPIC_MESSAGES_STRATEGY_BEAN);
        return Collections.unmodifiableMap(registry);
    }

    /**
     * 中文说明：建立有界流式输出执行器：线程数取 {@code yuheng.llm.streaming-threads}（上限 64），队列容量固定为 0，
     * 因此饱和时立刻抛 {@code TaskRejectedException} 而不是无界排队——协议面把它翻译成提交前的 429，避免慢上游把
     * engine 拖成堆积源。关闭时等待在途流写完，超时上限固定 30 秒。
     * English summary: Builds the bounded streaming executor whose thread count comes from {@code yuheng.llm.streaming-threads}
     * (capped at 64) with a queue capacity pinned to 0, so saturation raises {@code TaskRejectedException} immediately instead
     * of queueing without bound — the protocol face turns that into a pre-submission 429, keeping a slow upstream from turning
     * the engine into a backlog. Shutdown waits for in-flight streams up to a fixed 30 seconds.
     *
     * 用法 / Usage: 由 {@code LlmServletStreamComponent}（Step 11）按 bean 名 {@code llmStreamingExecutor} 注入。
     * @return 返回 已配置的有界执行器；returns the configured bounded executor.
     */
    @Bean("llmStreamingExecutor")
    public ThreadPoolTaskExecutor llmStreamingExecutor() {
        int threads = gatewayProperties.getStreamingThreads();
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(threads);
        executor.setMaxPoolSize(threads);
        executor.setQueueCapacity(0);
        executor.setThreadNamePrefix("llm-stream-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        log.info("llm streaming executor bounded to threads={} queue=0", threads);
        return executor;
    }

    /**
     * 中文说明：把 Messages 凭据适配器注册在既有身份过滤器之前（{@link LlmClientCredentialFilter#FILTER_ORDER}）。
     * 过滤器实例本身不是 bean，避免 Boot 以默认顺序重复注册，从而让 x-api-key 在认证之后才被适配。
     * English summary: Registers the Messages credential adapter before the existing identity filters
     * ({@link LlmClientCredentialFilter#FILTER_ORDER}). The filter instance is deliberately not a bean of its own, so Boot
     * cannot also register it at the default order and adapt x-api-key only after authentication already ran.
     *
     * 用法 / Usage: 由 Servlet 容器消费。/ Consumed by the servlet container.
     * @return 返回 显式顺序的过滤器注册；returns the explicitly ordered filter registration.
     */
    @Bean("llmClientCredentialFilterRegistration")
    public FilterRegistrationBean<LlmClientCredentialFilter> llmClientCredentialFilterRegistration() {
        FilterRegistrationBean<LlmClientCredentialFilter> registration = new FilterRegistrationBean<>(
                new LlmClientCredentialFilter(objectMapper));
        registration.setName("llmClientCredentialFilter");
        registration.addUrlPatterns("/*");
        registration.setOrder(LlmClientCredentialFilter.FILTER_ORDER);
        return registration;
    }
}
