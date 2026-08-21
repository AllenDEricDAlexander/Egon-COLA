package top.egon.cola.component.rpc.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import top.egon.cola.component.rpc.provider.server.RpcAccessGuardExceptionMapper;

/** Optional, class-safe Access Guard to RPC Provider adapter. */
@AutoConfiguration(afterName =
        "top.egon.cola.component.accessguard.autoconfigure.AccessGuardAopAutoConfiguration")
@ConditionalOnClass(name =
        "top.egon.cola.component.accessguard.api.AccessGuardRejectedException")
@ConditionalOnProperty(
        prefix = "egon.cola.component.rpc.provider",
        name = "enabled",
        havingValue = "true"
)
public class RpcAccessGuardAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(RpcAccessGuardExceptionMapper.class)
    public RpcAccessGuardExceptionMapper rpcAccessGuardExceptionMapper() {
        return new RpcAccessGuardExceptionMapper();
    }
}
