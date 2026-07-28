package top.egon.cola.component.common.id.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import top.egon.cola.component.common.id.generator.IdGenerator;
import top.egon.cola.component.common.id.snowflake.SnowflakeIdGenerator;

/**
 * Spring Boot auto-configuration for the default stateful Snowflake ID
 * generator. Applications may replace it with any {@link IdGenerator} bean.
 */
@AutoConfiguration
@EnableConfigurationProperties(IdGeneratorProperties.class)
@ConditionalOnProperty(prefix = IdGeneratorProperties.PREFIX, name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class IdGeneratorAutoConfiguration {

    /**
     * Creates the default generator after fail-fast configuration validation.
     *
     * @param properties bound common ID properties
     * @return the stateful Snowflake generator
     */
    @Bean
    @ConditionalOnMissingBean(IdGenerator.class)
    public SnowflakeIdGenerator snowflakeIdGenerator(IdGeneratorProperties properties) {
        IdGeneratorPropertiesValidator.validate(properties);
        return new SnowflakeIdGenerator(properties.getMachineId(), properties.getMaxClockBackward());
    }
}
