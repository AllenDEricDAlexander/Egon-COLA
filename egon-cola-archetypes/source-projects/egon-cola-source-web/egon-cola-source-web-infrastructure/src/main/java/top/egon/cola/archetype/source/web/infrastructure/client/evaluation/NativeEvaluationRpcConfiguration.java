package top.egon.cola.archetype.source.web.infrastructure.client.evaluation;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.Set;
import top.egon.cola.platform.tianquan.shoubing.contract.ServiceTokenContext;
import top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterProperties;
import top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterProperties.RpcConsumerModeEnum;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;
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
import top.egon.cola.archetype.source.service.facade.course.CourseFacade;
import top.egon.cola.archetype.source.service.facade.exam.ExamFacade;
import top.egon.cola.archetype.source.service.facade.exam.ScoreFacade;

/** Builds DIRECT references through the existing RPC descriptor, strategy and proxy factories. */
@Configuration(value = "nativeEvaluationRpcConfiguration", proxyBeanMethods = false)
@Profile({"dev", "prod"})
@EnableConfigurationProperties(NativeEvaluationRpcProperties.class)
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Slf4j
public class NativeEvaluationRpcConfiguration {
    @Qualifier("rpcContractValidator")
    private final RpcContractValidator contractValidator;
    @Qualifier("rpcReferenceStrategyFactory")
    private final RpcReferenceStrategyFactory strategies;
    @Qualifier("rpcConsumerProxyFactory")
    private final RpcConsumerProxyFactory proxies;
    @Qualifier("rpcProcessIdentity")
    private final RpcProcessIdentity processIdentity;
    @Qualifier("organization.integrations.evaluation-top.egon.cola.archetype.source.web.infrastructure.client.evaluation.NativeEvaluationRpcProperties")
    private final NativeEvaluationRpcProperties target;
    @Qualifier("egon.cola.component.rpc-top.egon.cola.component.rpc.config.EgonRpcProperties")
    private final EgonRpcProperties runtimeProperties;
    @Qualifier("nativeRpcValidation")
    private final ValidationUtils validation;
    @Qualifier("idpStarterProperties")
    private final IdpStarterProperties identityProperties;

    @Bean("evaluationQueryConverter")
    EvaluationQueryConverter evaluationQueryConverter() {
        return Mappers.getMapper(EvaluationQueryConverter.class);
    }

    @Bean("evaluationCourseFacade")
    CourseFacade evaluationCourseFacade() {
        return reference(CourseFacade.class, target.courseGroup(), "GetCourse", "course:read");
    }

    @Bean("evaluationExamFacade")
    ExamFacade evaluationExamFacade() {
        return reference(ExamFacade.class, target.examGroup(), "GetExam", "exam:read");
    }

    @Bean("evaluationScoreFacade")
    ScoreFacade evaluationScoreFacade() {
        return reference(ScoreFacade.class, target.scoreGroup(), "GetScore", "score:read");
    }

    private <T> T reference(Class<T> contractType, String group, String operation, String intent) {
        validation.validate(target);
        var descriptor = contractValidator.validate(contractType);
        String fullMethod = descriptor.serviceName() + "/" + operation;
        var candidates = identityProperties.getRpc().getConsumer().getBindings().values().stream()
                .filter(binding -> descriptor.serviceName().equals(binding.getServiceName())
                        && group.equals(binding.getGroup()) && target.version().equals(binding.getVersion())
                        && fullMethod.equals(binding.getFullMethodName())
                        && processIdentity.applicationName().equals(binding.getProcessIdentity().get("applicationName"))
                        && processIdentity.env().equals(binding.getProcessIdentity().get("env")))
                .toList();
        if (!identityProperties.getRpc().isEnabled() || !identityProperties.getRpc().getConsumer().isEnabled()
                || identityProperties.getRpc().getConsumer().getBindings().size() != 3 || candidates.size() != 1) {
            throw new IllegalStateException("EVALUATION_RPC_BINDING_UNAVAILABLE");
        }
        var binding = validation.validate(candidates.getFirst());
        if (binding.getMode() != RpcConsumerModeEnum.SERVICE || binding.getScopeContext() != ServiceTokenContext.TENANT
                || !binding.getScopes().equals(Set.of(target.applicationNamespace() + ":" + intent))) {
            throw new IllegalStateException("EVALUATION_RPC_BINDING_INVALID");
        }
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
