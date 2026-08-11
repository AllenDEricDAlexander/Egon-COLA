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
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.scheduling.annotation.EnableScheduling;
import top.egon.cola.component.ddc.admin.repository.DdcConfigLeaseRedisRepository;
import top.egon.cola.component.ddc.admin.repository.DdcInstanceRepository;
import top.egon.cola.component.ddc.admin.repository.DdcRedisRepository;
import top.egon.cola.component.ddc.admin.repository.DdcServiceRegistryRedisRepository;
import top.egon.cola.component.ddc.admin.security.rpc.DdcNonceStore;
import top.egon.cola.component.ddc.admin.security.rpc.RedisDdcNonceStore;
import top.egon.cola.component.ddc.admin.security.admission.DdcAdmissionVerifier;
import top.egon.cola.component.ddc.admin.security.admission.IdpJwtDdcAdmissionVerifier;
import top.egon.cola.component.ddc.admin.service.lease.DdcConfigLeaseService;
import top.egon.cola.component.ddc.admin.service.lease.DdcLeaseExpiryScanner;
import top.egon.cola.component.ddc.admin.service.lease.DdcLeaseValidator;
import top.egon.cola.component.ddc.admin.service.metadata.DdcScopeGate;
import top.egon.cola.component.ddc.admin.service.registry.DdcServiceRegistryService;
import top.egon.cola.component.ddc.redis.DdcRedisClientFactory;

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

    /**
     * 创建统一的 IdP Resource Server 准入票据校验器。
     *
     * <p>Creates the shared IdP Resource Server admission-ticket verifier.</p>
     *
     * @param redissonClient DDC 与 IdP 运行态投影所在 Redis 客户端；Redis client holding DDC
     * leases and IdP runtime projections
     * @param objectMapper JSON 解析器；JSON parser
     * @param properties DDC Admin 配置；DDC Admin settings
     * @return 准入校验器；admission verifier
     */
    @Bean
    @ConditionalOnBean(name = "ddcAdminRedissonClient")
    @ConditionalOnMissingBean(DdcAdmissionVerifier.class)
    public DdcAdmissionVerifier ddcAdmissionVerifier(
            @Qualifier("ddcAdminRedissonClient") RedissonClient redissonClient,
            ObjectMapper objectMapper,
            DdcAdminProperties properties
    ) {
        DdcAdminProperties.Admission admission = properties.getAdmission();
        String issuer = firstText(
                admission.getIssuer(),
                properties.getSecurity().getJwt().getIssuer(),
                "urn:egon:unconfigured:idp"
        );
        String jwkSetUri = firstText(
                admission.getJwkSetUri(),
                properties.getSecurity().getJwt().getJwkSetUri(),
                null
        );
        JwtDecoder decoder = hasText(jwkSetUri)
                ? NimbusJwtDecoder.withJwkSetUri(jwkSetUri.trim()).build()
                : token -> {
                    throw new JwtException(
                            "DDC admission JWK Set URI is not configured"
                    );
                };
        return new IdpJwtDdcAdmissionVerifier(
                decoder,
                redissonClient,
                objectMapper,
                admission.getResourceProjectionPrefix(),
                issuer,
                "ddc-registry"
        );
    }

    @Bean
    @ConditionalOnBean(DdcConfigLeaseRedisRepository.class)
    @ConditionalOnMissingBean
    public DdcConfigLeaseService ddcConfigLeaseService(
            DdcConfigLeaseRedisRepository repository,
            DdcAdmissionVerifier admissionVerifier,
            DdcAdminProperties properties) {
        return new DdcConfigLeaseService(
                repository,
                new DdcLeaseValidator(properties),
                admissionVerifier
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
            DdcAdmissionVerifier admissionVerifier) {
        return new DdcServiceRegistryService(
                repository,
                new DdcLeaseValidator(properties),
                scopeGate,
                admissionVerifier
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
