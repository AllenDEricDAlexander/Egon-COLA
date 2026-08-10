package top.egon.cola.platform.idp.admin.identity.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.egon.cola.platform.idp.admin.support.ddc.IdpRuntimePolicy;
import top.egon.cola.platform.idp.core.audit.IdentitySecurityEventPort;
import top.egon.cola.platform.idp.core.identity.IdentityFacade;
import top.egon.cola.platform.idp.core.identity.UsernameNormalizer;
import top.egon.cola.platform.idp.core.port.IdentityUserStatePort;
import top.egon.cola.platform.idp.core.port.IdentityUserStore;
import top.egon.cola.platform.idp.core.port.PasswordCredentialStore;
import top.egon.cola.platform.idp.core.port.PasswordHashPort;

/**
 * 装配统一身份认证领域所需的核心门面及动态安全策略。
 *
 * <p>Configures the core identity facade and its dynamic security policy dependencies.</p>
 */
@Configuration(proxyBeanMethods = false)
public class IdentityConfig {

    @Bean
    IdentityFacade identityFacade(
            IdentityUserStore users,
            PasswordCredentialStore credentials,
            PasswordHashPort passwordHashes,
            IdentityUserStatePort states,
            IdentitySecurityEventPort securityEvents,
            IdpRuntimePolicy runtimePolicy
    ) {
        return IdentityFacade.dynamicPolicy(
                users,
                credentials,
                passwordHashes,
                states,
                securityEvents,
                new UsernameNormalizer(),
                () -> runtimePolicy.current().maximumLoginFailures(),
                () -> runtimePolicy.current().loginLockDuration()
        );
    }
}
