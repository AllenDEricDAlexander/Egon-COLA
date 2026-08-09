package top.egon.cola.component.ddc.admin.security.rpc;

import io.grpc.ServerInterceptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import top.egon.cola.component.ddc.admin.config.DdcAdminProperties;
import top.egon.cola.component.rpc.provider.RpcProviderExceptionMapper;

/**
 * 将 DDC RPC 认证与异常映射接入中立 RPC Provider 扩展点。
 * / Connects DDC RPC authentication and error mapping to neutral provider hooks.
 */
@Configuration(proxyBeanMethods = false)
public class DdcRpcSecurityConfiguration {

    /** 创建请求级 HMAC 认证拦截器。 / Creates the request-level HMAC authentication interceptor. */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE + 100)
    public ServerInterceptor ddcRpcServerInterceptor(
            DdcAdminProperties properties,
            DdcHmacCredentialRegistry credentialRegistry,
            ObjectProvider<DdcNonceStore> nonceStore) {
        return new DdcRpcServerInterceptor(
                properties,
                credentialRegistry,
                nonceStore.getIfAvailable()
        );
    }

    /** 创建 Admin 领域异常 Mapper。 / Creates the Admin domain exception mapper. */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE + 100)
    public RpcProviderExceptionMapper ddcRpcProviderExceptionMapper() {
        return new DdcRpcProviderExceptionMapper();
    }
}
