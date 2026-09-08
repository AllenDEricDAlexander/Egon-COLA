package top.egon.cola.archetype.source.service.infrastructure.client.organization;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.rpc.annotation.FailStrategy;
import top.egon.cola.component.rpc.annotation.LoadBalance;
import top.egon.cola.component.rpc.config.EgonRpcProperties;
import top.egon.cola.component.rpc.consumer.provider.RpcProviderQuery;
import top.egon.cola.component.rpc.consumer.proxy.RpcConsumerProxyFactory;
import top.egon.cola.component.rpc.consumer.reference.RpcReferenceDefinition;
import top.egon.cola.component.rpc.consumer.reference.RpcReferenceMode;
import top.egon.cola.component.rpc.consumer.reference.RpcReferencePolicy;
import top.egon.cola.component.rpc.consumer.reference.RpcReferenceStrategyFactory;
import top.egon.cola.component.rpc.context.identity.RpcProcessIdentity;
import top.egon.cola.component.rpc.contract.identity.RpcServiceIdentity;
import top.egon.cola.component.rpc.contract.validation.RpcContractValidator;
import top.egon.cola.organization.facade.rpc.OrganizationRpcConverter;
import top.egon.cola.organization.facade.rpc.UserRpcService;
import top.egon.cola.organization.facade.rpc.SchoolClassRpcService;

/** Builds DIRECT references through the existing RPC descriptor, strategy and proxy factories. */
@Configuration(value = "nativeOrganizationRpcConfiguration", proxyBeanMethods = false)
@Profile({"dev", "prod"})
@EnableConfigurationProperties(NativeOrganizationRpcProperties.class)
@RequiredArgsConstructor
public class NativeOrganizationRpcConfiguration {
    @Qualifier("rpcContractValidator")
    private final RpcContractValidator contractValidator;
    @Qualifier("rpcReferenceStrategyFactory")
    private final RpcReferenceStrategyFactory strategies;
    @Qualifier("rpcConsumerProxyFactory")
    private final RpcConsumerProxyFactory proxies;
    @Qualifier("rpcProcessIdentity")
    private final RpcProcessIdentity processIdentity;
    @Qualifier("app.integrations.organization-top.egon.cola.archetype.source.service.infrastructure.client.organization.NativeOrganizationRpcProperties")
    private final NativeOrganizationRpcProperties target;
    @Qualifier("egon.cola.component.rpc-top.egon.cola.component.rpc.config.EgonRpcProperties")
    private final EgonRpcProperties runtimeProperties;
    @Qualifier("nativeRpcValidation")
    private final ValidationUtils validation;

    @Bean("organizationRpcConverter")
    OrganizationRpcConverter organizationRpcConverter() {
        return Mappers.getMapper(OrganizationRpcConverter.class);
    }

    @Bean("organizationDirectoryConverter")
    OrganizationDirectoryConverter organizationDirectoryConverter() {
        return Mappers.getMapper(OrganizationDirectoryConverter.class);
    }

    @Bean("organizationUserRpcService")
    UserRpcService organizationUserRpcService() {
        return reference(UserRpcService.class, target.group());
    }

    @Bean("organizationSchoolClassRpcService")
    SchoolClassRpcService organizationSchoolClassRpcService() {
        return reference(SchoolClassRpcService.class, target.group());
    }

    private <T> T reference(Class<T> contractType, String group) {
        validation.validate(target);
        var descriptor = contractValidator.validate(contractType);
        var identity = new RpcServiceIdentity(descriptor.serviceName(), group, target.version());
        var query = new RpcProviderQuery(target.bizCode(), target.appCode(), processIdentity.env(),
                descriptor.serviceName(), group, target.version(), "grpc");
        var policy = new RpcReferencePolicy(
                Math.min(target.timeoutMs(), runtimeProperties.getConsumer().getDefaultTimeoutMs()),
                0, LoadBalance.ROUND_ROBIN, FailStrategy.FAIL_CLOSED, "", null);
        Map<Method, RpcReferencePolicy> policies = new LinkedHashMap<>();
        descriptor.methods().forEach(method -> policies.put(method.javaMethod(), policy));
        var definition = new RpcReferenceDefinition(RpcReferenceMode.DIRECT, identity, query, policies);
        return proxies.create(descriptor, definition, strategies.create(definition));
    }
}
