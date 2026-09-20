package top.egon.cola.component.rpc.config;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.context.annotation.Bean;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.rpc.provider.server.RpcAccessGuardExceptionMapper;

/** Optional, class-safe Access Guard to RPC Provider adapter. */
@AutoConfiguration(
        after = ValidationAutoConfiguration.class,
        afterName = {
                "top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusAutoConfiguration",
                "top.egon.cola.component.accessguard.autoconfigure.AccessGuardAopAutoConfiguration"
        })
@ConditionalOnClass(name =
        "top.egon.cola.component.accessguard.common.exception.AccessGuardRejectedException")
@ConditionalOnProperty(
        prefix = "egon.cola.component.rpc.provider",
        name = "enabled",
        havingValue = "true"
)
public class RpcAccessGuardAutoConfiguration {

    @Bean(name = "egonColaValidationUtils")
    @ConditionalOnMissingBean(name = "egonColaValidationUtils")
    public ValidationUtils egonColaValidationUtils(ObjectProvider<Validator> validators) {
        return new ValidationUtils(validators.getIfAvailable(
                () -> Validation.buildDefaultValidatorFactory().getValidator()));
    }

    @Bean
    @ConditionalOnMissingBean(RpcAccessGuardExceptionMapper.class)
    public RpcAccessGuardExceptionMapper rpcAccessGuardExceptionMapper() {
        return new RpcAccessGuardExceptionMapper();
    }
}
