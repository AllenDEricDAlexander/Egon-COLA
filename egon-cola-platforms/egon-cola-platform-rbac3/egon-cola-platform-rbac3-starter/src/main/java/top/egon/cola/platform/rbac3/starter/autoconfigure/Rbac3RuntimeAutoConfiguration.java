package top.egon.cola.platform.rbac3.starter.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import top.egon.cola.platform.idp.starter.autoconfigure.IdpStarterAutoConfiguration;
import top.egon.cola.platform.rbac3.starter.runtime.Rbac3RuntimeRedissonConfiguration;

/** Creates the shared runtime Redis client before IdP state readers are evaluated. */
@AutoConfiguration
@AutoConfigureBefore(IdpStarterAutoConfiguration.class)
@EnableConfigurationProperties(Rbac3StarterProperties.class)
@ConditionalOnProperty(
        prefix = "egon.cola.platform.rbac3",
        name = "enabled",
        havingValue = "true")
@Import(Rbac3RuntimeRedissonConfiguration.class)
public class Rbac3RuntimeAutoConfiguration {
}
