package top.egon.cola.component.ddc.admin.config;

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
import top.egon.cola.component.ddc.admin.repository.DdcConfigLeaseRedisRepository;
import top.egon.cola.component.ddc.admin.repository.DdcInstanceRepository;
import top.egon.cola.component.ddc.admin.repository.DdcRedisRepository;
import top.egon.cola.component.ddc.admin.repository.DdcServiceRegistryRedisRepository;
import top.egon.cola.component.ddc.admin.security.rpc.DdcNonceStore;
import top.egon.cola.component.ddc.admin.security.rpc.RedisDdcNonceStore;
import top.egon.cola.component.ddc.admin.security.registration.DdcRegistrationCredentialVerifier;
import top.egon.cola.component.ddc.admin.security.registration.IdpJwtDdcRegistrationCredentialVerifier;
import top.egon.cola.component.ddc.admin.service.lease.DdcConfigLeaseService;
import top.egon.cola.component.ddc.admin.service.lease.DdcLeaseExpiryScanner;
import top.egon.cola.component.ddc.admin.service.lease.DdcLeaseValidator;
import top.egon.cola.component.ddc.admin.service.metadata.DdcScopeGate;
import top.egon.cola.component.ddc.admin.service.registry.DdcServiceRegistryService;
import top.egon.cola.component.ddc.redis.DdcRedisClientFactory;
import top.egon.cola.platform.idp.starter.autoconfigure.IdpStarterProperties;
import top.egon.cola.platform.idp.starter.security.ServiceAccessTokenVerifier;

import java.net.URI;
import java.util.Objects;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(DdcAdminProperties.class)
public class DdcAdminRedisConfig {

    @Bean("ddcAdminRedissonClient")
    @ConditionalOnMissingBean(name = "ddcAdminRedissonClient")
    @ConditionalOnProperty(prefix = "egon.cola.component.ddc.admin.redis", name = "enabled", havingValue = "true", matchIfMissing = true)
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

    @Bean
    @ConditionalOnBean(name = "ddcAdminRedissonClient")
    @ConditionalOnMissingBean
    public DdcRedisRepository ddcRedisRepository(@Qualifier("ddcAdminRedissonClient") RedissonClient redissonClient) {
        return new DdcRedisRepository(redissonClient);
    }

    @Bean
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

    /** Creates the DDC verifier backed by the shared IdP SERVICE-token verifier. */
    @Bean
    @ConditionalOnBean(
            value = ServiceAccessTokenVerifier.class,
            name = "ddcAdminRedissonClient"
    )
    @ConditionalOnMissingBean(DdcRegistrationCredentialVerifier.class)
    public DdcRegistrationCredentialVerifier ddcRegistrationCredentialVerifier(
            ServiceAccessTokenVerifier serviceTokens,
            DdcAdminProperties properties,
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
                required(resourceServerId, "DDC Resource Server id"),
                Objects.requireNonNull(
                        resourceUri,
                        "DDC Resource URI is required"
                ),
                required(
                        registration.getRequiredScope(),
                        "DDC registration scope"
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
                registrationVerifier
        );
    }

    @Bean
    @ConditionalOnBean(name = "ddcAdminRedissonClient")
    @ConditionalOnMissingBean
    public DdcServiceRegistryRedisRepository ddcServiceRegistryRedisRepository(
            @Qualifier("ddcAdminRedissonClient") RedissonClient redissonClient,
            ObjectMapper objectMapper) {
        return new DdcServiceRegistryRedisRepository(redissonClient, objectMapper);
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
                registrationVerifier
        );
    }

    @Bean
    @ConditionalOnBean(DdcConfigLeaseRedisRepository.class)
    @ConditionalOnMissingBean
    public DdcLeaseExpiryScanner ddcLeaseExpiryScanner(DdcInstanceRepository instanceRepository,
                                                       DdcConfigLeaseRedisRepository leaseRepository,
                                                       DdcServiceRegistryRedisRepository registryRepository) {
        return new DdcLeaseExpiryScanner(
                instanceRepository,
                leaseRepository,
                registryRepository
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
