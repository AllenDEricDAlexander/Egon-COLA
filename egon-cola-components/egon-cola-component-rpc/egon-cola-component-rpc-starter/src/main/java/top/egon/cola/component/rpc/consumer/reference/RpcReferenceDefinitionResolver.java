package top.egon.cola.component.rpc.consumer.reference;

import org.springframework.context.ApplicationContext;
import top.egon.cola.component.rpc.annotation.EgonRpcReference;
import top.egon.cola.component.rpc.annotation.EgonRpcService;
import top.egon.cola.component.rpc.annotation.EgonServiceMeta;
import top.egon.cola.component.rpc.annotation.FailStrategy;
import top.egon.cola.component.rpc.annotation.LoadBalance;
import top.egon.cola.component.rpc.config.EgonRpcProperties;
import top.egon.cola.component.rpc.consumer.loadbalance.RpcLoadBalanceKeyResolver;
import top.egon.cola.component.rpc.consumer.provider.RpcProviderQuery;
import top.egon.cola.component.rpc.context.identity.RpcProcessIdentity;
import top.egon.cola.component.rpc.contract.descriptor.RpcContractDescriptor;
import top.egon.cola.component.rpc.contract.descriptor.RpcMethodDescriptor;
import top.egon.cola.component.rpc.exception.EgonRpcErrorCode;
import top.egon.cola.component.rpc.exception.EgonRpcException;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Resolves annotations and trusted named beans once at injection time. */
public final class RpcReferenceDefinitionResolver {

    private final EgonRpcProperties.Consumer consumer;
    private final RpcProcessIdentity processIdentity;
    private final ApplicationContext applicationContext;

    public RpcReferenceDefinitionResolver(
            EgonRpcProperties properties,
            RpcProcessIdentity processIdentity) {
        this(properties, processIdentity, null);
    }

    public RpcReferenceDefinitionResolver(
            EgonRpcProperties properties,
            RpcProcessIdentity processIdentity,
            ApplicationContext applicationContext) {
        if (properties == null || processIdentity == null) {
            throw invalid("RPC reference resolver properties and process identity are required");
        }
        this.consumer = properties.getConsumer();
        this.consumer.validateSharedSettings();
        this.processIdentity = processIdentity;
        this.applicationContext = applicationContext;
    }

    public RpcReferenceDefinition resolve(
            Field field,
            RpcContractDescriptor descriptor) {
        Objects.requireNonNull(field, "field");
        Objects.requireNonNull(descriptor, "descriptor");
        if (!descriptor.contractType().isAssignableFrom(field.getType())
                && !field.getType().isAssignableFrom(descriptor.contractType())) {
            throw invalid("RPC reference field type does not match contract");
        }
        EgonRpcReference reference = field.getAnnotation(EgonRpcReference.class);
        if (reference == null) {
            throw invalid("RPC reference field must declare @EgonRpcReference");
        }
        RpcReferenceMode mode = reference.mode();
        validateMode(reference, mode);
        EgonRpcService service = descriptor.contractType().getAnnotation(EgonRpcService.class);
        EgonServiceMeta typeMeta = descriptor.contractType().getAnnotation(EgonServiceMeta.class);
        Map<java.lang.reflect.Method, RpcReferencePolicy> policies = new LinkedHashMap<>();
        for (RpcMethodDescriptor method : descriptor.methods()) {
            policies.put(method.javaMethod(), resolvePolicy(
                    method.javaMethod().getAnnotation(EgonServiceMeta.class),
                    typeMeta,
                    service,
                    reference,
                    field,
                    descriptor));
        }
        RpcProviderQuery directQuery = mode == RpcReferenceMode.DIRECT
                ? directQuery(reference, descriptor) : null;
        return new RpcReferenceDefinition(
                mode,
                new top.egon.cola.component.rpc.contract.identity.RpcServiceIdentity(
                        descriptor.serviceName(),
                        effectiveGroup(reference, descriptor.group()),
                        effectiveVersion(reference, descriptor.version())),
                directQuery,
                policies);
    }

    private RpcReferencePolicy resolvePolicy(
            EgonServiceMeta methodMeta,
            EgonServiceMeta typeMeta,
            EgonRpcService service,
            EgonRpcReference reference,
            Field field,
            RpcContractDescriptor descriptor) {
        long timeout = timeout(
                methodMeta == null ? -1 : methodMeta.timeoutMs(),
                reference.timeoutMs(),
                service == null ? -1 : service.timeoutMs(),
                typeMeta == null ? -1 : typeMeta.timeoutMs(),
                consumer.getDefaultTimeoutMs());
        int retries = retries(
                methodMeta == null ? -1 : methodMeta.retries(),
                reference.retries(),
                service == null ? -1 : service.retries(),
                typeMeta == null ? -1 : typeMeta.retries());
        LoadBalance loadBalance = firstLoadBalance(
                methodMeta == null ? LoadBalance.INHERIT : methodMeta.loadBalance(),
                reference.loadBalance(),
                service == null ? LoadBalance.INHERIT : service.loadBalance(),
                typeMeta == null ? LoadBalance.INHERIT : typeMeta.loadBalance(),
                consumer.getDefaultLoadBalance());
        String fallbackBean = reference.fallbackBean();
        FailStrategy failStrategy = reference.failStrategy();
        if (failStrategy == FailStrategy.INHERIT) {
            failStrategy = FailStrategy.FAIL_CLOSED;
        }
        validateFallback(field, descriptor, fallbackBean, failStrategy);
        String resolverBean = reference.loadBalanceKeyResolver();
        RpcLoadBalanceKeyResolver keyResolver = resolveKeyResolver(
                field, resolverBean, loadBalance);
        return new RpcReferencePolicy(
                timeout, retries, loadBalance, failStrategy, fallbackBean, keyResolver);
    }

    private RpcProviderQuery directQuery(
            EgonRpcReference reference,
            RpcContractDescriptor descriptor) {
        String env = blank(reference.env()) ? processIdentity.env() : reference.env().trim();
        String group = effectiveGroup(reference, descriptor.group());
        String version = effectiveVersion(reference, descriptor.version());
        return new RpcProviderQuery(
                reference.bizCode(),
                reference.appCode(),
                env,
                descriptor.serviceName(),
                group,
                version,
                "grpc");
    }

    private String effectiveGroup(
            EgonRpcReference reference,
            String fallback) {
        String override = reference.group();
        return blank(override) ? fallback : override.trim();
    }

    private String effectiveVersion(
            EgonRpcReference reference,
            String fallback) {
        String override = reference.version();
        return blank(override) ? fallback : override.trim();
    }

    private void validateMode(EgonRpcReference reference, RpcReferenceMode mode) {
        if (mode == null) {
            throw invalid("RPC reference mode is required");
        }
        if (mode == RpcReferenceMode.DIRECT) {
            if (blank(reference.bizCode()) || blank(reference.appCode())) {
                throw invalid("DIRECT RPC reference requires bizCode and appCode");
            }
            return;
        }
        if (mode == RpcReferenceMode.GATEWAY
                && (!blank(reference.bizCode())
                || !blank(reference.appCode())
                || !blank(reference.env()))) {
            throw invalid("bizCode, appCode and env are only valid for DIRECT references");
        }
    }

    private long timeout(long... values) {
        long resolved = Long.MAX_VALUE;
        for (long value : values) {
            if (value == -1) {
                continue;
            }
            if (value <= 0) {
                throw invalid("RPC reference timeout must be -1 or positive");
            }
            resolved = Math.min(resolved, value);
        }
        if (resolved == Long.MAX_VALUE) {
            throw invalid("RPC reference timeout is not configured");
        }
        return resolved;
    }

    private int retries(int... values) {
        int resolved = consumer.getMaxRetries();
        for (int value : values) {
            if (value == -1) {
                continue;
            }
            if (value < 0 || value > consumer.getMaxRetries()) {
                throw invalid("RPC reference retries exceed consumer max-retries");
            }
            resolved = value;
            break;
        }
        return resolved;
    }

    private LoadBalance firstLoadBalance(LoadBalance... values) {
        for (LoadBalance value : values) {
            if (value != null && value != LoadBalance.INHERIT) {
                return value;
            }
        }
        throw invalid("RPC reference load balance is not configured");
    }

    private RpcLoadBalanceKeyResolver resolveKeyResolver(
            Field field,
            String beanName,
            LoadBalance loadBalance) {
        if (loadBalance != LoadBalance.CONSISTENT_HASH && !blank(beanName)) {
            throw invalid("load-balance key resolver is only valid for CONSISTENT_HASH");
        }
        if (loadBalance == LoadBalance.CONSISTENT_HASH) {
            if (blank(beanName) || applicationContext == null) {
                throw invalid("CONSISTENT_HASH requires a named resolver bean for "
                        + field.getName());
            }
            try {
                Object bean = applicationContext.getBean(beanName.trim());
                if (!(bean instanceof RpcLoadBalanceKeyResolver resolver)) {
                    throw invalid("resolver bean has wrong type for " + field.getName());
                }
                return resolver;
            } catch (EgonRpcException exception) {
                throw exception;
            } catch (RuntimeException exception) {
                throw invalid("resolver bean is unavailable for " + field.getName(), exception);
            }
        }
        return null;
    }

    private void validateFallback(
            Field field,
            RpcContractDescriptor descriptor,
            String beanName,
            FailStrategy failStrategy) {
        if (failStrategy == FailStrategy.LOCAL_FALLBACK && blank(beanName)) {
            throw invalid("LOCAL_FALLBACK requires fallback bean for " + field.getName());
        }
        if (failStrategy != FailStrategy.LOCAL_FALLBACK && !blank(beanName)) {
            throw invalid("fallback bean is only valid for LOCAL_FALLBACK");
        }
        if (!blank(beanName)) {
            if (applicationContext == null) {
                throw invalid("fallback bean is unavailable for " + field.getName());
            }
            try {
                Object bean = applicationContext.getBean(beanName.trim());
                if (!descriptor.contractType().isInstance(bean)) {
                    throw invalid("fallback bean has wrong type for " + field.getName());
                }
            } catch (EgonRpcException exception) {
                throw exception;
            } catch (RuntimeException exception) {
                throw invalid("fallback bean is unavailable for " + field.getName(), exception);
            }
        }
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private EgonRpcException invalid(String message) {
        return new EgonRpcException(EgonRpcErrorCode.RPC_INVALID_CONTRACT, message);
    }

    private EgonRpcException invalid(String message, Throwable cause) {
        return new EgonRpcException(EgonRpcErrorCode.RPC_INVALID_CONTRACT, message, cause);
    }
}
