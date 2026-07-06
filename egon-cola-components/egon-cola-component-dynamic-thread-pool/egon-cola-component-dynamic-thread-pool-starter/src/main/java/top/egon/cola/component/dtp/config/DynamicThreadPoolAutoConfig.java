package top.egon.cola.component.dtp.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import top.egon.cola.component.dtp.domain.DynamicThreadPoolService;
import top.egon.cola.component.dtp.domain.IDynamicThreadPoolService;
import top.egon.cola.component.dtp.executor.ManagedExecutor;
import top.egon.cola.component.dtp.executor.ManagedExecutorRegistry;
import top.egon.cola.component.dtp.executor.adapter.BoundedVirtualThreadManagedExecutor;
import top.egon.cola.component.dtp.executor.adapter.ThreadPoolExecutorManagedExecutor;
import top.egon.cola.component.dtp.executor.adapter.ThreadPoolTaskExecutorManagedExecutor;
import top.egon.cola.component.dtp.executor.virtual.BoundedVirtualThreadExecutor;
import top.egon.cola.component.dtp.metrics.DtpMeterBinder;
import top.egon.cola.component.dtp.registry.IRegistry;
import top.egon.cola.component.dtp.registry.model.DtpConfigChangeMessage;
import top.egon.cola.component.dtp.registry.model.DtpRedisKeys;
import top.egon.cola.component.dtp.registry.redis.RedisRegistry;
import top.egon.cola.component.dtp.trigger.job.ThreadPoolDataReportJob;
import top.egon.cola.component.dtp.trigger.listener.ThreadPoolConfigAdjustListener;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

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

    @Bean
    public IRegistry redisRegistry(@Qualifier("dynamicThreadRedissonClient") RedissonClient dynamicThreadRedissonClient) {
        return new RedisRegistry(dynamicThreadRedissonClient);
    }

    @Bean
    public ManagedExecutorRegistry managedExecutorRegistry(ApplicationContext applicationContext,
                                                          DynamicThreadPoolAutoProperties properties,
                                                          Map<String, ThreadPoolExecutor> threadPoolExecutorMap,
                                                          Map<String, ThreadPoolTaskExecutor> threadPoolTaskExecutorMap,
                                                          Map<String, BoundedVirtualThreadExecutor> boundedVirtualThreadExecutorMap) {
        String appName = resolveAppName(applicationContext, properties);
        String instanceId = resolveInstanceId(applicationContext, properties, appName);
        List<ManagedExecutor> managedExecutors = new ArrayList<>();
        threadPoolExecutorMap.forEach((executorName, executor) ->
                managedExecutors.add(new ThreadPoolExecutorManagedExecutor(appName, instanceId, executorName, executor))
        );
        threadPoolTaskExecutorMap.forEach((executorName, executor) ->
                managedExecutors.add(new ThreadPoolTaskExecutorManagedExecutor(appName, instanceId, executorName, executor))
        );
        boundedVirtualThreadExecutorMap.forEach((executorName, executor) ->
                managedExecutors.add(new BoundedVirtualThreadManagedExecutor(appName, instanceId, executorName, executor))
        );
        return new ManagedExecutorRegistry(managedExecutors);
    }

    @Bean("dynamicThreadPollService")
    public DynamicThreadPoolService dynamicThreadPollService(ManagedExecutorRegistry managedExecutorRegistry) {
        return new DynamicThreadPoolService(managedExecutorRegistry);
    }

    @Bean
    public ThreadPoolConfigAdjustListener threadPoolConfigAdjustListener(IDynamicThreadPoolService dynamicThreadPoolService,
                                                                         IRegistry registry) {
        return new ThreadPoolConfigAdjustListener(dynamicThreadPoolService, registry);
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
        if (StringUtils.isNotBlank(properties.getAppName())) {
            return properties.getAppName();
        }
        String springApplicationName = applicationContext.getEnvironment().getProperty("spring.application.name");
        if (StringUtils.isNotBlank(springApplicationName)) {
            return springApplicationName;
        }
        return "default-app";
    }

    String resolveInstanceId(ApplicationContext applicationContext, DynamicThreadPoolAutoProperties properties, String appName) {
        if (StringUtils.isNotBlank(properties.getInstanceId())) {
            return properties.getInstanceId();
        }
        String serverPort = applicationContext.getEnvironment().getProperty("server.port");
        if (StringUtils.isNotBlank(serverPort)) {
            return appName + "-" + serverPort;
        }
        return appName + "-" + ManagementFactory.getRuntimeMXBean().getName();
    }

    @Configuration
    @EnableScheduling
    @ConditionalOnProperty(prefix = "egon.cola.component.dtp.report", name = "enabled", havingValue = "true", matchIfMissing = true)
    static class DtpReportConfiguration {

        @Bean
        public ThreadPoolDataReportJob threadPoolDataReportJob(IDynamicThreadPoolService dynamicThreadPoolService,
                                                               IRegistry registry) {
            return new ThreadPoolDataReportJob(dynamicThreadPoolService, registry);
        }

    }

    @Configuration
    @ConditionalOnClass(name = "io.micrometer.core.instrument.MeterRegistry")
    static class DtpMetricsConfiguration {

        @Bean
        @ConditionalOnBean(MeterRegistry.class)
        public DtpMeterBinder dtpMeterBinder(ManagedExecutorRegistry managedExecutorRegistry) {
            return new DtpMeterBinder(managedExecutorRegistry);
        }

    }

}
