package top.egon.cola.component.common.id.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import top.egon.cola.component.common.id.generator.IdGenerator;
import top.egon.cola.component.common.id.snowflake.SnowflakeIdGenerator;

/**
 * Spring Boot auto-configuration that binds the static Snowflake ID engine during the configuration
 * phase.
 *
 * <p>Generation is a static entry point, so no generator bean is published. Supplying any
 * {@link IdGenerator} bean, or disabling the Starter, leaves the static engine unbound.</p>
 */
@AutoConfiguration
@EnableConfigurationProperties(IdGeneratorProperties.class)
@ConditionalOnProperty(prefix = IdGeneratorProperties.PREFIX, name = "enabled",
        havingValue = "true", matchIfMissing = true)
@ConditionalOnMissingBean(IdGenerator.class)
public class IdGeneratorAutoConfiguration {

    /**
     * Validates the bound configuration and binds the process-wide engine exactly once.
     *
     * @param properties bound common ID properties
     */
    public IdGeneratorAutoConfiguration(IdGeneratorProperties properties) {
        SnowflakeIdGenerator.initialize(properties.getMachineId(), properties.getMaxClockBackward());
    }
}
