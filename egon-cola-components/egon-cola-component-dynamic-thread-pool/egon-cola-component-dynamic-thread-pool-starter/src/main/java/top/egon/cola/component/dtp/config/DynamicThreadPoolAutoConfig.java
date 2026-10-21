package top.egon.cola.component.dtp.config;

import top.egon.cola.component.dtp.exception.LoggingDtpTaskExceptionHandler;
import top.egon.cola.component.dtp.exception.DtpTaskExceptionHandler;
import top.egon.cola.component.dtp.context.DtpTaskContextManager;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.redisson.Redisson;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.dtp.executor.ThreadManage;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import top.egon.cola.component.dtp.domain.DynamicThreadPoolService;
import top.egon.cola.component.dtp.domain.IDynamicThreadPoolService;
import top.egon.cola.component.dtp.executor.ManagedExecutorRegistry;
import top.egon.cola.component.dtp.metrics.DtpMeterBinder;
import top.egon.cola.component.dtp.registry.IRegistry;
import top.egon.cola.component.dtp.registry.model.DtpConfigChangeMessage;
import top.egon.cola.component.dtp.registry.model.DtpRedisKeys;
import top.egon.cola.component.dtp.registry.redis.RedisRegistry;
import top.egon.cola.component.dtp.trigger.job.ThreadPoolDataReportJob;
import top.egon.cola.component.dtp.trigger.listener.ThreadPoolConfigAdjustListener;

import java.lang.management.ManagementFactory;

/**
 * @ClassName: DynamicThreadPoolAutoConfig
 * @description: 动态配置入口
 * @author: 有罗敷的马同学
 * @datetime: 2025Year-01Month-05Day-21:14
 * @Version: 1.0
 */
@AutoConfiguration
@EnableConfigurationProperties(DynamicThreadPoolAutoProperties.class)
@ConditionalOnProperty(prefix = "egon.cola.component.dtp", name = "enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class DynamicThreadPoolAutoConfig {

    @Bean("dynamicThreadRedissonClient")
    public RedissonClient redissonClient(DynamicThreadPoolAutoProperties properties) {
        Config config = new Config();

        // 使用自定义 ObjectMapper 的 JsonJacksonCodec
        config.setCodec(new JsonJacksonCodec(createRedisObjectMapper()));

        DynamicThreadPoolAutoProperties.Redis redis = properties.getRegistry().getRedis();
        config.useSingleServer()
                .setAddress("redis://" + redis.getHost() + ":" + redis.getPort())
                .setPassword(redis.getPassword())
                .setDatabase(redis.getDatabase())
                .setConnectionPoolSize(redis.getPoolSize())
                .setConnectionMinimumIdleSize(redis.getMinIdleSize())
                .setIdleConnectionTimeout(redis.getIdleTimeout())
                .setConnectTimeout(redis.getConnectTimeout())
                .setRetryAttempts(redis.getRetryAttempts())
                .setRetryInterval(redis.getRetryInterval())
                .setPingConnectionInterval(redis.getPingInterval())
                .setKeepAlive(redis.isKeepAlive())
        ;

        RedissonClient redissonClient = Redisson.create(config);
        log.info("动态线程池，注册器（redis）链接初始化完成。{} {} {}", redis.getHost(), redis.getPoolSize(), !redissonClient.isShutdown());
        return redissonClient;
    }

    static ObjectMapper createRedisObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.activateDefaultTyping(
            LaissezFaireSubTypeValidator.instance,
            ObjectMapper.DefaultTyping.NON_FINAL,
            JsonTypeInfo.As.PROPERTY
        );
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        return objectMapper;
    }

    @Bean("redisRegistry")
    public IRegistry redisRegistry(@Qualifier("dynamicThreadRedissonClient") RedissonClient dynamicThreadRedissonClient) {
        return new RedisRegistry(dynamicThreadRedissonClient);
    }

    public ManagedExecutorRegistry managedExecutorRegistry(ApplicationContext applicationContext,
                                                            DynamicThreadPoolAutoProperties properties) {
        String appName = resolveAppName(applicationContext, properties);
        return new ManagedExecutorRegistry(appName, resolveInstanceId(applicationContext, properties, appName), properties);
    }

    @Bean("managedExecutorRegistry")
    public ManagedExecutorRegistry managedExecutorRegistry(ApplicationContext context, DynamicThreadPoolAutoProperties properties,
            @Qualifier("dtpTaskContextManager") ObjectProvider<DtpTaskContextManager> contexts,
            @Qualifier("dtpTaskExceptionHandler") ObjectProvider<DtpTaskExceptionHandler> handlers,
            @Qualifier("dtpValidationUtils") ObjectProvider<ValidationUtils> validation) {
        String appName = resolveAppName(context, properties);
        var fallback = new DtpTaskContextManager();
        return new ManagedExecutorRegistry(appName, resolveInstanceId(context, properties, appName), properties,
                () -> contexts.getIfAvailable(() -> fallback),
                () -> handlers.getIfAvailable(LoggingDtpTaskExceptionHandler::new),
                validation::getIfAvailable);
    }

    /** 保留手工调用自动配置工厂的历史签名；正常自动发现使用初始化后登记。 */
    public ManagedExecutorRegistry managedExecutorRegistry(ApplicationContext context, DynamicThreadPoolAutoProperties properties,
            java.util.Map<String, java.util.concurrent.ThreadPoolExecutor> platform,
            java.util.Map<String, org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor> spring,
            java.util.Map<String, top.egon.cola.component.dtp.executor.virtual.BoundedVirtualThreadExecutor> virtual) {
        ManagedExecutorRegistry registry = managedExecutorRegistry(context, properties);
        platform.forEach(registry::registerBean);
        spring.forEach(registry::registerBean);
        virtual.forEach(registry::registerBean);
        return registry;
    }

    @Bean("dtpExecutorBeanPostProcessor")
    public static DtpExecutorBeanPostProcessor dtpExecutorBeanPostProcessor(
            @Qualifier("managedExecutorRegistry") ObjectProvider<ManagedExecutorRegistry> registry) {
        return new DtpExecutorBeanPostProcessor(registry);
    }

    @Bean("dtpValidator")
    public LocalValidatorFactoryBean dtpValidator() {
        return new LocalValidatorFactoryBean();
    }

    @Bean("dtpValidationUtils")
    public ValidationUtils dtpValidationUtils(@Qualifier("dtpValidator") jakarta.validation.Validator validator) {
        return new ValidationUtils(validator);
    }

    @Bean("threadManage")
    public ThreadManage threadManage(@Qualifier("managedExecutorRegistry") ManagedExecutorRegistry registry,
                                      @Qualifier("dynamicThreadPollService") IDynamicThreadPoolService service,
                                      @Qualifier("dtpValidationUtils") ValidationUtils validation) {
        return new ThreadManage(registry, service, validation);
    }

    @Bean("dynamicThreadPollService")
    public DynamicThreadPoolService dynamicThreadPollService(
            @Qualifier("managedExecutorRegistry") ManagedExecutorRegistry managedExecutorRegistry,
            @Qualifier("dtpValidationUtils") ValidationUtils validation) {
        return new DynamicThreadPoolService(managedExecutorRegistry, validation);
    }

    public DynamicThreadPoolService dynamicThreadPollService(ManagedExecutorRegistry registry) {
        return new DynamicThreadPoolService(registry);
    }

    public ThreadPoolConfigAdjustListener threadPoolConfigAdjustListener(IDynamicThreadPoolService service, IRegistry registry) {
        return new ThreadPoolConfigAdjustListener(service, registry);
    }

    @Bean("threadPoolConfigAdjustListener")
    public ThreadPoolConfigAdjustListener threadPoolConfigAdjustListener(
            @Qualifier("dynamicThreadPollService") IDynamicThreadPoolService dynamicThreadPoolService,
            @Qualifier("redisRegistry") IRegistry registry, ApplicationContext context, DynamicThreadPoolAutoProperties properties) {
        String appName = resolveAppName(context, properties);
        return new ThreadPoolConfigAdjustListener(dynamicThreadPoolService, registry, appName, resolveInstanceId(context, properties, appName));
    }

    @Bean(name = "dynamicThreadPoolRedisTopic")
    public RTopic dynamicThreadPoolRedisTopic(ApplicationContext applicationContext,
                                             DynamicThreadPoolAutoProperties properties,
                                             @Qualifier("dynamicThreadRedissonClient") RedissonClient redissonClient,
                                             ThreadPoolConfigAdjustListener threadPoolConfigAdjustListener) {
        String appName = resolveAppName(applicationContext, properties);
        RTopic topic = redissonClient.getTopic(DtpRedisKeys.changeTopic(appName));
        topic.addListener(DtpConfigChangeMessage.class, threadPoolConfigAdjustListener);
        return topic;
    }

    String resolveAppName(ApplicationContext applicationContext, DynamicThreadPoolAutoProperties properties) {
        if (StringUtils.hasText(properties.getAppName())) {
            return properties.getAppName();
        }
        String springApplicationName = applicationContext.getEnvironment().getProperty("spring.application.name");
        if (StringUtils.hasText(springApplicationName)) {
            return springApplicationName;
        }
        return "default-app";
    }

    String resolveInstanceId(ApplicationContext applicationContext, DynamicThreadPoolAutoProperties properties, String appName) {
        if (StringUtils.hasText(properties.getInstanceId())) {
            return properties.getInstanceId();
        }
        String serverPort = applicationContext.getEnvironment().getProperty("server.port");
        if (StringUtils.hasText(serverPort)) {
            return appName + "-" + serverPort;
        }
        return appName + "-" + ManagementFactory.getRuntimeMXBean().getName();
    }

    @Configuration
    @EnableConfigurationProperties(DynamicThreadPoolAutoProperties.class)
    @EnableScheduling
    @ConditionalOnProperty(prefix = "egon.cola.component.dtp.report", name = "enabled", havingValue = "true", matchIfMissing = true)
    static class DtpReportConfiguration {

        @Bean("threadPoolDataReportJob")
        public ThreadPoolDataReportJob threadPoolDataReportJob(IDynamicThreadPoolService dynamicThreadPoolService,
                                                               IRegistry registry, DynamicThreadPoolAutoProperties properties) {
            return new ThreadPoolDataReportJob(dynamicThreadPoolService, registry, properties);
        }

    }

    @Configuration
    @ConditionalOnClass(name = "io.micrometer.core.instrument.MeterRegistry")
    static class DtpMetricsConfiguration {

        @Bean("dtpMeterBinder")
        @ConditionalOnBean(MeterRegistry.class)
        public DtpMeterBinder dtpMeterBinder(ManagedExecutorRegistry managedExecutorRegistry) {
            return new DtpMeterBinder(managedExecutorRegistry);
        }

    }

}
