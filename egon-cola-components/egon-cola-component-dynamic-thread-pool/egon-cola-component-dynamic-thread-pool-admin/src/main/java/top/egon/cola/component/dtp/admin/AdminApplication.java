package top.egon.cola.component.dtp.admin;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.egon.cola.component.dtp.config.DynamicThreadPoolAutoConfig;

@SpringBootApplication(
        exclude = DynamicThreadPoolAutoConfig.class,
        scanBasePackages = {
                "top.egon.cola.component.dtp.admin.config",
                "top.egon.cola.component.dtp.admin.trigger",
                "top.egon.cola.component.dtp.admin.manifest"
        }
)
@Configurable
public class AdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(AdminApplication.class);
    }

    @Configuration
    @EnableConfigurationProperties(RedisClientConfigProperties.class)
    public static class RedisClientConfig {

        @Bean("redissonClient")
        public RedissonClient redissonClient(ConfigurableApplicationContext applicationContext, RedisClientConfigProperties properties) {
            Config config = new Config();

            // 使用自定义 ObjectMapper 的 JsonJacksonCodec
            config.setCodec(new JsonJacksonCodec(createRedisObjectMapper()));

            SingleServerConfig singleServerConfig = config.useSingleServer()
                    .setAddress("redis://" + properties.getHost() + ":" + properties.getPort())
                    .setDatabase(properties.getDatabase())
                    .setConnectionPoolSize(properties.getPoolSize())
                    .setConnectionMinimumIdleSize(properties.getMinIdleSize())
                    .setIdleConnectionTimeout(properties.getIdleTimeout())
                    .setConnectTimeout(properties.getConnectTimeout())
                    .setRetryAttempts(properties.getRetryAttempts())
                    .setRetryInterval(properties.getRetryInterval())
                    .setPingConnectionInterval(properties.getPingInterval())
                    .setKeepAlive(properties.isKeepAlive())
            ;
            if (StringUtils.isNotBlank(properties.getPassword())) {
                singleServerConfig.setPassword(properties.getPassword());
            }

            return Redisson.create(config);
        }

        static ObjectMapper createRedisObjectMapper() {
            // 创建 ObjectMapper 并配置类型信息
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

    }


    @Data
    @ConfigurationProperties(prefix = "egon.cola.component.dtp.registry.redis", ignoreInvalidFields = true)
    public static class RedisClientConfigProperties {
        /**
         * host:ip
         */
        private String host;
        /**
         * 端口
         */
        private int port;
        /**
         * 账密
         */
        private String password;
        /**
         * Redis database
         */
        private int database = 0;
        /**
         * 设置连接池的大小，默认为 64
         */
        private int poolSize = 64;
        /**
         * 设置连接池的最小空闲连接数，默认为 10
         */
        private int minIdleSize = 10;
        /**
         * 设置连接的最大空闲时间（单位：毫秒），超过该时间的空闲连接将被关闭，默认为 10000
         */
        private int idleTimeout = 10000;
        /**
         * 设置连接超时时间（单位：毫秒），默认为 10000
         */
        private int connectTimeout = 10000;
        /**
         * 设置连接重试次数，默认为 3
         */
        private int retryAttempts = 3;
        /**
         * 设置连接重试的间隔时间（单位：毫秒），默认为 1000
         */
        private int retryInterval = 1000;
        /**
         * 设置定期检查连接是否可用的时间间隔（单位：毫秒），默认为 0，表示不进行定期检查
         */
        private int pingInterval = 0;
        /**
         * 设置是否保持长连接，默认为 true
         */
        private boolean keepAlive = true;
    }

}
