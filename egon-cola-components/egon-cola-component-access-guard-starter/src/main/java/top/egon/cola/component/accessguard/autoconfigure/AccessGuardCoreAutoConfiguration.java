package top.egon.cola.component.accessguard.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import top.egon.cola.component.accessguard.adapter.aop.GuardBindingResolver;
import top.egon.cola.component.accessguard.adapter.programmatic.DefaultAccessGuardClient;
import top.egon.cola.component.accessguard.api.AccessGuardClient;
import top.egon.cola.component.accessguard.core.DefaultGuardEngine;
import top.egon.cola.component.accessguard.core.GuardEngine;
import top.egon.cola.component.accessguard.core.failure.DefaultFailurePolicyResolver;
import top.egon.cola.component.accessguard.core.failure.FailurePolicyResolver;
import top.egon.cola.component.accessguard.core.plan.DefaultGuardPlanResolver;
import top.egon.cola.component.accessguard.core.plan.GuardPlanResolver;
import top.egon.cola.component.accessguard.core.plan.GuardPlanSource;
import top.egon.cola.component.accessguard.core.plan.GuardPlanValidator;
import top.egon.cola.component.accessguard.core.plan.PropertiesGuardPlanSource;
import top.egon.cola.component.accessguard.execution.DefaultRejectionHandler;
import top.egon.cola.component.accessguard.execution.FallbackHandler;
import top.egon.cola.component.accessguard.execution.FallbackMethodCache;
import top.egon.cola.component.accessguard.execution.JsonRejectValueParser;
import top.egon.cola.component.accessguard.execution.MethodHandleFallbackHandler;
import top.egon.cola.component.accessguard.execution.RejectionHandler;
import top.egon.cola.component.accessguard.execution.TimeLimiter;
import top.egon.cola.component.accessguard.execution.async.CompletionStageGuardExecutor;
import top.egon.cola.component.accessguard.execution.reactive.ReactiveGuardExecutor;
import top.egon.cola.component.accessguard.key.CompositeGuardKeyResolver;
import top.egon.cola.component.accessguard.key.GuardKeyResolver;
import top.egon.cola.component.accessguard.key.HmacSha256KeyHasher;
import top.egon.cola.component.accessguard.key.KeyHasher;
import top.egon.cola.component.accessguard.key.TrustedProxyMatcher;
import top.egon.cola.component.accessguard.key.contributor.ArgumentKeyContributor;
import top.egon.cola.component.accessguard.key.contributor.AttributeKeyContributor;
import top.egon.cola.component.accessguard.key.contributor.ClientIpKeyContributor;
import top.egon.cola.component.accessguard.key.contributor.GlobalKeyContributor;
import top.egon.cola.component.accessguard.key.contributor.GuardKeyContributor;
import top.egon.cola.component.accessguard.key.contributor.HttpHeaderKeyContributor;
import top.egon.cola.component.accessguard.key.contributor.PrincipalKeyContributor;
import top.egon.cola.component.accessguard.policy.AdmissionPolicies;
import top.egon.cola.component.accessguard.policy.GuardPolicy;
import top.egon.cola.component.accessguard.policy.allow.AllowListPolicy;
import top.egon.cola.component.accessguard.policy.deny.DenyListPolicy;
import top.egon.cola.component.accessguard.policy.penalty.DefaultPenaltyService;
import top.egon.cola.component.accessguard.policy.penalty.PenaltyBoxPolicy;
import top.egon.cola.component.accessguard.policy.penalty.PenaltyService;
import top.egon.cola.component.accessguard.policy.ratelimit.RateLimitPolicy;
import top.egon.cola.component.accessguard.observability.GuardEventPublisher;
import top.egon.cola.component.accessguard.store.AllowListStore;
import top.egon.cola.component.accessguard.store.DenyListStore;
import top.egon.cola.component.accessguard.store.PenaltyStore;
import top.egon.cola.component.accessguard.store.RateLimitBackend;
import top.egon.cola.component.accessguard.store.AccessGuardStorageIntegration;
import top.egon.cola.component.accessguard.store.local.LocalPenaltyStore;
import top.egon.cola.component.accessguard.store.local.LocalRateLimitBackend;

import java.util.List;
import java.util.Map;

@AutoConfiguration
@EnableConfigurationProperties(AccessGuardProperties.class)
@ConditionalOnProperty(
        prefix = AccessGuardProperties.PREFIX,
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class AccessGuardCoreAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public GuardPlanValidator accessGuardPlanValidator() {
        return new GuardPlanValidator();
    }

    @Bean
    @ConditionalOnMissingBean
    public PropertiesGuardPlanSource accessGuardPropertiesPlanSource(AccessGuardProperties properties) {
        return new PropertiesGuardPlanSource(properties);
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean(GuardPlanResolver.class)
    public DefaultGuardPlanResolver accessGuardPlanResolver(
            List<GuardPlanSource> sources,
            GuardPlanValidator validator,
            AccessGuardProperties properties,
            ObjectProvider<GuardEventPublisher> eventPublishers
    ) {
        GuardEventPublisher eventPublisher = eventPublishers.getIfAvailable(GuardEventPublisher::noop);
        return new DefaultGuardPlanResolver(sources, validator, event -> {
            AccessGuardProperties.Rule rule = properties.getRules().get(event.ruleId());
            if (rule == null || rule.getObservability().isMetrics()) {
                try {
                    eventPublisher.publishPlanChanged(event);
                } catch (RuntimeException ignored) {
                    // Plan activation is independent from optional observability delivery.
                }
            }
        });
    }

    @Bean
    @ConditionalOnMissingBean
    public TrustedProxyMatcher accessGuardTrustedProxyMatcher(AccessGuardProperties properties) {
        return new TrustedProxyMatcher(properties.getKey().getTrustedProxies());
    }

    @Bean
    @ConditionalOnMissingBean
    public ArgumentKeyContributor accessGuardArgumentKeyContributor() {
        return new ArgumentKeyContributor();
    }

    @Bean
    @ConditionalOnMissingBean
    public AttributeKeyContributor accessGuardAttributeKeyContributor() {
        return new AttributeKeyContributor();
    }

    @Bean
    @ConditionalOnMissingBean
    public GlobalKeyContributor accessGuardGlobalKeyContributor() {
        return new GlobalKeyContributor();
    }

    @Bean
    @ConditionalOnMissingBean
    public PrincipalKeyContributor accessGuardPrincipalKeyContributor() {
        return new PrincipalKeyContributor();
    }

    @Bean
    @ConditionalOnMissingBean
    public HttpHeaderKeyContributor accessGuardHttpHeaderKeyContributor() {
        return new HttpHeaderKeyContributor();
    }

    @Bean
    @ConditionalOnMissingBean
    public ClientIpKeyContributor accessGuardClientIpKeyContributor(TrustedProxyMatcher matcher) {
        return new ClientIpKeyContributor(matcher);
    }

    @Bean
    @ConditionalOnMissingBean
    public KeyHasher accessGuardKeyHasher() {
        return new HmacSha256KeyHasher();
    }

    @Bean
    @ConditionalOnMissingBean(GuardKeyResolver.class)
    public CompositeGuardKeyResolver accessGuardKeyResolver(
            List<GuardKeyContributor> contributors,
            KeyHasher keyHasher
    ) {
        return new CompositeGuardKeyResolver(contributors, keyHasher);
    }

    @Bean
    @ConditionalOnMissingBean
    public FailurePolicyResolver accessGuardFailurePolicyResolver() {
        return new DefaultFailurePolicyResolver();
    }

    @Bean
    @ConditionalOnMissingBean
    public DenyListPolicy accessGuardDenyListPolicy(DenyListStore store) {
        return new DenyListPolicy(store);
    }

    @Bean
    @ConditionalOnMissingBean
    public AllowListPolicy accessGuardAllowListPolicy(AllowListStore store) {
        return new AllowListPolicy(store);
    }

    @Bean
    @ConditionalOnMissingBean
    public PenaltyBoxPolicy accessGuardPenaltyBoxPolicy(PenaltyStore store) {
        return new PenaltyBoxPolicy(store);
    }

    @Bean
    @ConditionalOnMissingBean
    public RateLimitPolicy accessGuardRateLimitPolicy(RateLimitBackend backend) {
        return new RateLimitPolicy(backend);
    }

    @Bean
    @ConditionalOnMissingBean
    public PenaltyService accessGuardPenaltyService(PenaltyStore store) {
        return new DefaultPenaltyService(store);
    }

    @Bean("accessGuardAdmissionPolicies")
    public List<GuardPolicy<?>> accessGuardAdmissionPolicies(
            DenyListPolicy denyList,
            AllowListPolicy allowList,
            PenaltyBoxPolicy penaltyBox,
            RateLimitPolicy rateLimit
    ) {
        return AdmissionPolicies.builtIns(denyList, allowList, penaltyBox, rateLimit);
    }

    @Bean("accessGuardLocalPolicies")
    public Map<String, GuardPolicy<?>> accessGuardLocalPolicies(
            LocalPenaltyStore penaltyStore,
            LocalRateLimitBackend rateLimitBackend
    ) {
        return Map.of(
                "penalty-box", new PenaltyBoxPolicy(penaltyStore),
                "rate-limit", new RateLimitPolicy(rateLimitBackend));
    }

    @Bean
    @ConditionalOnMissingBean
    public FallbackMethodCache accessGuardFallbackMethodCache() {
        return new FallbackMethodCache();
    }

    @Bean
    @ConditionalOnMissingBean
    public FallbackHandler accessGuardFallbackHandler(FallbackMethodCache cache) {
        return new MethodHandleFallbackHandler(cache);
    }

    @Bean
    @ConditionalOnMissingBean
    public JsonRejectValueParser accessGuardJsonRejectValueParser(ObjectProvider<ObjectMapper> objectMapper) {
        return new JsonRejectValueParser(() -> {
            ObjectMapper mapper = objectMapper.getIfAvailable();
            if (mapper == null) {
                throw new IllegalStateException("RETURN_JSON requires a Spring-managed ObjectMapper");
            }
            return mapper;
        });
    }

    @Bean
    @ConditionalOnMissingBean
    public RejectionHandler accessGuardRejectionHandler(
            FallbackHandler fallbackHandler,
            JsonRejectValueParser jsonParser
    ) {
        return new DefaultRejectionHandler(fallbackHandler, jsonParser);
    }

    @Bean
    @ConditionalOnMissingBean(GuardEngine.class)
    public DefaultGuardEngine accessGuardEngine(
            GuardPlanResolver planResolver,
            GuardKeyResolver keyResolver,
            @Qualifier("accessGuardAdmissionPolicies") List<GuardPolicy<?>> policies,
            @Qualifier("accessGuardLocalPolicies") Map<String, GuardPolicy<?>> localPolicies,
            FailurePolicyResolver failurePolicyResolver,
            PenaltyService penaltyService,
            TimeLimiter timeLimiter,
            RejectionHandler rejectionHandler,
            AccessGuardProperties properties,
            ObjectProvider<GuardEventPublisher> eventPublishers
    ) {
        return new DefaultGuardEngine(
                planResolver,
                keyResolver,
                policies,
                localPolicies,
                failurePolicyResolver,
                penaltyService,
                timeLimiter,
                rejectionHandler,
                System::nanoTime,
                properties.getStorage().name(),
                properties.getEngine().name(),
                eventPublishers.getIfAvailable(GuardEventPublisher::noop));
    }

    @Bean
    @ConditionalOnMissingBean
    public AccessGuardClient accessGuardClient(GuardEngine engine) {
        return new DefaultAccessGuardClient(engine);
    }

    @Bean
    @ConditionalOnMissingBean
    public CompletionStageGuardExecutor accessGuardCompletionStageExecutor(GuardEngine engine) {
        return new CompletionStageGuardExecutor(engine);
    }

    @Bean
    @ConditionalOnMissingBean
    public GuardBindingResolver accessGuardBindingResolver() {
        return new GuardBindingResolver();
    }

    @Bean
    @ConditionalOnMissingBean
    public AccessGuardStartupValidator accessGuardStartupValidator(
            AccessGuardProperties properties,
            GuardPlanResolver planResolver,
            GuardPlanValidator planValidator,
            GuardBindingResolver bindingResolver,
            FallbackMethodCache fallbackCache,
            JsonRejectValueParser jsonParser,
            org.springframework.beans.factory.ListableBeanFactory beanFactory,
            ObjectProvider<top.egon.cola.component.accessguard.api.AccessGuardAgentIntegration> integrations,
            ObjectProvider<AccessGuardStorageIntegration> storageIntegrations,
            ObjectProvider<ReactiveGuardExecutor> reactiveExecutors
    ) {
        return new AccessGuardStartupValidator(
                properties,
                planResolver,
                planValidator,
                bindingResolver,
                fallbackCache,
                jsonParser,
                beanFactory,
                integrations,
                storageIntegrations,
                reactiveExecutors);
    }
}
