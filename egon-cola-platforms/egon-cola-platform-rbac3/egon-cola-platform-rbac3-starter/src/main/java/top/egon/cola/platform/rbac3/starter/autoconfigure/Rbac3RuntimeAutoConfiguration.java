package top.egon.cola.platform.rbac3.starter.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import top.egon.cola.platform.idp.starter.autoconfigure.IdpStarterAutoConfiguration;
import top.egon.cola.platform.rbac3.starter.runtime.Rbac3RuntimeRedissonConfiguration;

/**
 * 类型 `Rbac3RuntimeAutoConfiguration` 位于当前包内，是类型，用于承载 `Rbac3 Runtime Auto Configuration` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `Rbac3RuntimeAutoConfiguration` is a type in its package and carries the responsibility, state, or contract for `Rbac3 Runtime Auto Configuration`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * Creates the shared runtime Redis client before IdP state readers are evaluated.
 */
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
