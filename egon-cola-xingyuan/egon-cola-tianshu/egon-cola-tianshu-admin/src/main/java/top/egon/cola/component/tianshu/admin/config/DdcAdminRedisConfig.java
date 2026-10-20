package top.egon.cola.component.tianshu.admin.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.PlatformTransactionManager;
import top.egon.cola.component.tianshu.admin.repository.DdcConfigLeaseRedisRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcInstanceRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcRedisRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcServiceRegistryRedisRepository;
import top.egon.cola.component.tianshu.admin.security.rpc.DdcNonceStore;
import top.egon.cola.component.tianshu.admin.security.rpc.RedisDdcNonceStore;
import top.egon.cola.component.tianshu.admin.security.tenancy.DdcTenantAdminProperties;
import top.egon.cola.component.tianshu.admin.security.tenancy.DdcTenantContextExecutor;
import top.egon.cola.component.tianshu.admin.security.tenancy.DdcTenantRegistry;
import top.egon.cola.component.tianshu.admin.security.registration.DdcRegistrationCredentialVerifier;
import top.egon.cola.component.tianshu.admin.security.registration.IdpJwtDdcRegistrationCredentialVerifier;

import top.egon.cola.component.tianshu.admin.service.lease.DdcConfigLeaseService;
import top.egon.cola.component.tianshu.admin.service.lease.DdcLeaseExpiryScanner;
import top.egon.cola.component.tianshu.admin.service.lease.DdcLeaseValidator;
import top.egon.cola.component.tianshu.admin.service.metadata.DdcScopeGate;
import top.egon.cola.component.tianshu.admin.service.notification.DdcNotificationPublisher;
import top.egon.cola.component.tianshu.admin.service.registry.DdcServiceRegistryService;
import top.egon.cola.component.tianshu.redis.DdcRedisClientFactory;
import top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterProperties;
import top.egon.cola.platform.tianquan.shoubing.starter.security.ServiceAccessTokenVerifier;

import java.net.URI;
import java.util.Objects;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(DdcAdminProperties.class)
public class DdcAdminRedisConfig {

    /** Keeps Common cache on the configuration center's own Redis connection. */
    @Bean(name = "redissonClient", destroyMethod = "")
    @ConditionalOnMissingBean(name = "redissonClient")
    @ConditionalOnProperty(prefix = "egon.cola.component.tianshu.admin.redis", name = "enabled", havingValue = "true", matchIfMissing = true)
    RedissonClient applicationCacheRedissonClient(@Qualifier("ddcAdminRedissonClient") RedissonClient client) {
        return client;
    }

    @Bean("ddcAdminRedissonClient")
    @ConditionalOnMissingBean(name = "ddcAdminRedissonClient")
    @ConditionalOnProperty(prefix = "egon.cola.component.tianshu.admin.redis", name = "enabled", havingValue = "true", matchIfMissing = true)
    public RedissonClient ddcAdminRedissonClient(DdcAdminProperties properties) {
        DdcAdminProperties.Redis redis = properties.getRedis();
        return DdcRedisClientFactory.create(
                redis.getMode(),
                redis.getNodes(),
                redis.getMasterName(),
                redis.getHost(),
                redis.getPort(),
                redis.getPassword(),
                redis.getDatabase()
        );
    }

    @Bean("ddcRedisRepository")
    @ConditionalOnBean(name = "ddcAdminRedissonClient")
    @ConditionalOnMissingBean
    public DdcRedisRepository ddcRedisRepository(
            @Qualifier("ddcAdminRedissonClient") RedissonClient redissonClient,
            @Qualifier("jacksonObjectMapper") ObjectMapper objectMapper) {
        return new DdcRedisRepository(redissonClient, objectMapper);
    }

    @Bean("ddcNonceStore")
    @ConditionalOnBean(name = "ddcAdminRedissonClient")
    @ConditionalOnMissingBean(DdcNonceStore.class)
    public DdcNonceStore redisDdcNonceStore(
            @Qualifier("ddcAdminRedissonClient")
            RedissonClient redissonClient) {
        return new RedisDdcNonceStore(redissonClient);
    }

    @Bean
    @ConditionalOnBean(name = "ddcAdminRedissonClient")
    @ConditionalOnMissingBean
    public DdcConfigLeaseRedisRepository ddcConfigLeaseRedisRepository(
            @Qualifier("ddcAdminRedissonClient") RedissonClient redissonClient,
            ObjectMapper objectMapper) {
        return new DdcConfigLeaseRedisRepository(redissonClient, objectMapper);
    }

    /** Creates the Tianshu verifier backed by the shared Tianquan-Shoubing SERVICE-token verifier. */
    @Bean
    @ConditionalOnBean(name = "ddcAdminRedissonClient")
    @ConditionalOnMissingBean(DdcRegistrationCredentialVerifier.class)
    public DdcRegistrationCredentialVerifier ddcRegistrationCredentialVerifier(
            ServiceAccessTokenVerifier serviceTokens,
            DdcAdminProperties properties,
            @Qualifier("idpStarterProperties")
            IdpStarterProperties idpProperties
    ) {
        DdcAdminProperties.Registration registration =
                properties.getRegistration();
        String resourceServerId = firstText(
                registration.getResourceServerId(),
                idpProperties.getResourceServerId(),
                null
        );
        URI resourceUri = registration.getResourceUri() != null
                ? registration.getResourceUri()
                : idpProperties.getResourceUri();
        return new IdpJwtDdcRegistrationCredentialVerifier(
                serviceTokens,
                required(resourceServerId, "Tianshu Resource Server id"),
                Objects.requireNonNull(
                        resourceUri,
                        "Tianshu Resource URI is required"
                ),
                required(
                        registration.getRequiredScope(),
                        "Tianshu registration scope"
                ),
                java.time.Clock.systemUTC()
        );
    }

    @Bean
    @ConditionalOnBean(DdcConfigLeaseRedisRepository.class)
    @ConditionalOnMissingBean
    public DdcConfigLeaseService ddcConfigLeaseService(
            DdcConfigLeaseRedisRepository repository,
            DdcRegistrationCredentialVerifier registrationVerifier,
            DdcAdminProperties properties) {
        return new DdcConfigLeaseService(
                repository,
                new DdcLeaseValidator(properties),
                registrationVerifier);
    }

    @Bean("ddcServiceRegistryRedisRepository")
    @ConditionalOnBean(name = "ddcAdminRedissonClient")
    @ConditionalOnMissingBean
    public DdcServiceRegistryRedisRepository ddcServiceRegistryRedisRepository(
            @Qualifier("ddcAdminRedissonClient") RedissonClient redissonClient,
            @Qualifier("jacksonObjectMapper") ObjectMapper objectMapper,
            @Qualifier("ddcNotificationPublisher") DdcNotificationPublisher notificationPublisher) {
        return new DdcServiceRegistryRedisRepository(redissonClient, objectMapper, notificationPublisher);
    }

    @Bean
    @ConditionalOnBean(DdcServiceRegistryRedisRepository.class)
    @ConditionalOnMissingBean
    public DdcServiceRegistryService ddcServiceRegistryService(
            DdcServiceRegistryRedisRepository repository,
            DdcAdminProperties properties,
            DdcScopeGate scopeGate,
            DdcRegistrationCredentialVerifier registrationVerifier) {
        return new DdcServiceRegistryService(
                repository,
                new DdcLeaseValidator(properties),
                scopeGate,
                registrationVerifier);
    }

    @Bean
    @ConditionalOnBean(DdcConfigLeaseRedisRepository.class)
    @ConditionalOnMissingBean
    public DdcLeaseExpiryScanner ddcLeaseExpiryScanner(DdcInstanceRepository instanceRepository,
                                                       DdcConfigLeaseRedisRepository leaseRepository,
                                                       DdcServiceRegistryRedisRepository registryRepository,
                                                       @Qualifier("egon.cola.component.tianshu.admin.tenancy-top.egon.cola.component.tianshu.admin.security.tenancy.DdcTenantAdminProperties")
                                                       DdcTenantAdminProperties tenantProperties,
                                                       @Qualifier("ddcTenantRegistry") DdcTenantRegistry tenantRegistry,
                                                       @Qualifier("ddcTenantContextExecutor") DdcTenantContextExecutor tenantContextExecutor,
                                                       PlatformTransactionManager transactionManager) {
        return new DdcLeaseExpiryScanner(
                instanceRepository,
                leaseRepository,
                registryRepository,
                tenantProperties,
                tenantRegistry,
                tenantContextExecutor,
                transactionManager
        );
    }

    /**
     * 返回第一个非空配置值。
     *
     * <p>Returns the first non-blank configuration value.</p>
     *
     * @param primary 首选值；preferred value
     * @param fallback 兼容回退值；compatibility fallback
     * @param defaultValue 默认值；default value
     * @return 选中的值；selected value
     */
    private String firstText(
            String primary,
            String fallback,
            String defaultValue
    ) {
        if (hasText(primary)) {
            return primary.trim();
        }
        return hasText(fallback) ? fallback.trim() : defaultValue;
    }

    /** Returns a required non-blank configuration value. */
    private String required(String value, String description) {
        if (!hasText(value)) {
            throw new IllegalStateException(description + " is required");
        }
        return value.trim();
    }

    /**
     * 判断字符串是否包含非空白文本。
     *
     * <p>Determines whether a string contains non-whitespace text.</p>
     *
     * @param value 待判断值；value to inspect
     * @return 包含文本时为 {@code true}；{@code true} when text is present
     */
    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
