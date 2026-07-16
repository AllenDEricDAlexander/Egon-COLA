package top.egon.cola.component.accessguard.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import top.egon.cola.component.accessguard.aop.AccessGuardAop;
import top.egon.cola.component.accessguard.blacklist.BlacklistService;
import top.egon.cola.component.accessguard.blacklist.BlacklistStatus;
import top.egon.cola.component.accessguard.circuitbreaker.ThreadPoolTimeoutCircuitBreakerExecutor;
import top.egon.cola.component.accessguard.circuitbreaker.TimeoutCircuitBreakerExecutor;
import top.egon.cola.component.accessguard.config.AccessGuardAnnotationResolver;
import top.egon.cola.component.accessguard.config.AccessGuardConfigProvider;
import top.egon.cola.component.accessguard.config.AccessGuardRule;
import top.egon.cola.component.accessguard.config.AccessGuardRuleResolver;
import top.egon.cola.component.accessguard.config.DefaultAccessGuardConfigProvider;
import top.egon.cola.component.accessguard.context.AccessGuardContext;
import top.egon.cola.component.accessguard.event.AccessGuardEventListener;
import top.egon.cola.component.accessguard.event.AccessGuardEventPublisher;
import top.egon.cola.component.accessguard.event.LoggingAccessGuardEventListener;
import top.egon.cola.component.accessguard.event.NoopAccessGuardEventPublisher;
import top.egon.cola.component.accessguard.execution.AccessGuardExecutionService;
import top.egon.cola.component.accessguard.execution.AccessGuardFailureHandler;
import top.egon.cola.component.accessguard.execution.ConstructorAccessGuardExecutionService;
import top.egon.cola.component.accessguard.execution.ConstructorAccessGuardValidator;
import top.egon.cola.component.accessguard.key.AccessKeyResolver;
import top.egon.cola.component.accessguard.key.DefaultAccessKeyResolver;
import top.egon.cola.component.accessguard.key.ExecutableAccessKeyResolver;
import top.egon.cola.component.accessguard.ratelimiter.LocalRateLimiterExecutor;
import top.egon.cola.component.accessguard.ratelimiter.RateLimiterExecutor;
import top.egon.cola.component.accessguard.reject.ReflectionFallbackInvoker;
import top.egon.cola.component.accessguard.reject.RejectResponseInvoker;
import top.egon.cola.component.accessguard.whitelist.DefaultWhiteListService;
import top.egon.cola.component.accessguard.whitelist.WhiteListRepository;
import top.egon.cola.component.accessguard.whitelist.WhiteListService;

import java.util.List;
import java.util.concurrent.Executors;

@AutoConfiguration
@EnableConfigurationProperties(AccessGuardProperties.class)
@ConditionalOnProperty(prefix = "egon.cola.component.access-guard", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AccessGuardAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AccessGuardConfigProvider accessGuardConfigProvider() {
        return new DefaultAccessGuardConfigProvider();
    }

    @Bean
    @ConditionalOnMissingBean
    public AccessGuardAnnotationResolver accessGuardAnnotationResolver() {
        return new AccessGuardAnnotationResolver();
    }

    @Bean
    @ConditionalOnMissingBean
    public AccessGuardRuleResolver accessGuardRuleResolver(
            AccessGuardProperties properties,
            AccessGuardConfigProvider configProvider,
            AccessGuardAnnotationResolver annotationResolver
    ) {
        return new AccessGuardRuleResolver(properties, configProvider, annotationResolver);
    }

    @Bean
    @ConditionalOnMissingBean
    public AccessKeyResolver accessKeyResolver(AccessGuardProperties properties) {
        return new DefaultAccessKeyResolver(properties.getKeyResolveFailureStrategy());
    }

    @Bean
    @ConditionalOnMissingBean
    public ExecutableAccessKeyResolver executableAccessKeyResolver(
            AccessGuardProperties properties
    ) {
        return new DefaultAccessKeyResolver(properties.getKeyResolveFailureStrategy());
    }

    @Bean
    @ConditionalOnMissingBean
    public WhiteListRepository whiteListRepository() {
        return (ruleName, accessKeyHash) -> false;
    }

    @Bean
    @ConditionalOnMissingBean
    public WhiteListService whiteListService(AccessGuardProperties properties, WhiteListRepository whiteListRepository) {
        return new DefaultWhiteListService(properties, whiteListRepository);
    }

    @Bean
    @ConditionalOnMissingBean
    public RateLimiterExecutor rateLimiterExecutor() {
        return new LocalRateLimiterExecutor();
    }

    @Bean
    @ConditionalOnMissingBean
    public BlacklistService blacklistService() {
        return new BlacklistService() {
            @Override
            public BlacklistStatus status(AccessGuardRule rule, AccessGuardContext context) {
                return BlacklistStatus.none();
            }

            @Override
            public BlacklistStatus incrementRejectAndMaybeBlacklist(AccessGuardRule rule, AccessGuardContext context) {
                return BlacklistStatus.none();
            }

            @Override
            public void remove(String ruleName, String accessKeyHash) {
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean
    public RejectResponseInvoker rejectResponseInvoker() {
        return new ReflectionFallbackInvoker();
    }

    @Bean
    @ConditionalOnMissingBean
    public TimeoutCircuitBreakerExecutor timeoutCircuitBreakerExecutor(
            AccessGuardProperties properties,
            RejectResponseInvoker rejectResponseInvoker
    ) {
        int poolSize = Math.max(1, properties.getThreadPool().getCorePoolSize());
        return new ThreadPoolTimeoutCircuitBreakerExecutor(Executors.newFixedThreadPool(poolSize), rejectResponseInvoker);
    }

    @Bean
    @ConditionalOnMissingBean
    public LoggingAccessGuardEventListener loggingAccessGuardEventListener() {
        return new LoggingAccessGuardEventListener();
    }

    @Bean
    @ConditionalOnMissingBean
    public AccessGuardEventPublisher accessGuardEventPublisher(List<AccessGuardEventListener> listeners) {
        return new NoopAccessGuardEventPublisher(listeners);
    }

    @Bean
    @ConditionalOnMissingBean
    public AccessGuardFailureHandler accessGuardFailureHandler(AccessGuardProperties properties) {
        return new AccessGuardFailureHandler(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public AccessGuardExecutionService accessGuardExecutionService(
            AccessGuardProperties properties,
            AccessGuardRuleResolver ruleResolver,
            AccessKeyResolver keyResolver,
            WhiteListService whiteListService,
            BlacklistService blacklistService,
            RateLimiterExecutor rateLimiterExecutor,
            TimeoutCircuitBreakerExecutor timeoutCircuitBreakerExecutor,
            RejectResponseInvoker rejectResponseInvoker,
            AccessGuardEventPublisher eventPublisher,
            AccessGuardFailureHandler failureHandler
    ) {
        return new AccessGuardExecutionService(
                properties,
                ruleResolver,
                keyResolver,
                whiteListService,
                blacklistService,
                rateLimiterExecutor,
                timeoutCircuitBreakerExecutor,
                rejectResponseInvoker,
                eventPublisher,
                failureHandler
        );
    }

    @Bean
    @ConditionalOnMissingBean
    public ConstructorAccessGuardValidator constructorAccessGuardValidator() {
        return new ConstructorAccessGuardValidator();
    }

    @Bean
    @ConditionalOnMissingBean
    public ConstructorAccessGuardExecutionService constructorAccessGuardExecutionService(
            AccessGuardProperties properties,
            AccessGuardRuleResolver ruleResolver,
            ConstructorAccessGuardValidator validator,
            ExecutableAccessKeyResolver keyResolver,
            WhiteListService whiteListService,
            BlacklistService blacklistService,
            RateLimiterExecutor rateLimiterExecutor,
            AccessGuardEventPublisher eventPublisher,
            AccessGuardFailureHandler failureHandler
    ) {
        return new ConstructorAccessGuardExecutionService(
                properties,
                ruleResolver,
                validator,
                keyResolver,
                whiteListService,
                blacklistService,
                rateLimiterExecutor,
                eventPublisher,
                failureHandler
        );
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(
            prefix = "egon.cola.component.access-guard",
            name = "engine",
            havingValue = "AOP",
            matchIfMissing = true
    )
    public AccessGuardAop accessGuardAop(
            AccessGuardProperties properties,
            AccessGuardExecutionService executionService
    ) {
        return new AccessGuardAop(properties, executionService);
    }
}
