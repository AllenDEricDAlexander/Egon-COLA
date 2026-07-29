package top.egon.cola.component.accessguard.autoconfigure;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.util.ClassUtils;
import top.egon.cola.component.accessguard.adapter.aop.GuardBinding;
import top.egon.cola.component.accessguard.adapter.aop.GuardBindingResolver;
import top.egon.cola.component.accessguard.api.AccessGuard;
import top.egon.cola.component.accessguard.api.AccessGuardAgentIntegration;
import top.egon.cola.component.accessguard.core.plan.GuardPlan;
import top.egon.cola.component.accessguard.core.plan.GuardPlanProperties;
import top.egon.cola.component.accessguard.core.plan.GuardPlanResolver;
import top.egon.cola.component.accessguard.core.plan.GuardPlanValidator;
import top.egon.cola.component.accessguard.execution.FallbackMethodCache;
import top.egon.cola.component.accessguard.execution.JsonRejectValueParser;
import top.egon.cola.component.accessguard.store.AccessGuardStorageIntegration;
import top.egon.cola.component.accessguard.execution.reactive.ReactiveGuardExecutor;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class AccessGuardStartupValidator implements SmartInitializingSingleton {

    private final GuardPlanProperties properties;
    private final GuardPlanResolver planResolver;
    private final GuardPlanValidator planValidator;
    private final GuardBindingResolver bindingResolver;
    private final FallbackMethodCache fallbackCache;
    private final JsonRejectValueParser jsonParser;
    private final ListableBeanFactory beanFactory;
    private final ObjectProvider<AccessGuardAgentIntegration> integrations;
    private final ObjectProvider<AccessGuardStorageIntegration> storageIntegrations;
    private final ObjectProvider<ReactiveGuardExecutor> reactiveExecutors;

    public AccessGuardStartupValidator(
            GuardPlanProperties properties,
            GuardPlanResolver planResolver,
            GuardPlanValidator planValidator,
            GuardBindingResolver bindingResolver,
            FallbackMethodCache fallbackCache,
            JsonRejectValueParser jsonParser,
            ListableBeanFactory beanFactory,
            ObjectProvider<AccessGuardAgentIntegration> integrations,
            ObjectProvider<AccessGuardStorageIntegration> storageIntegrations,
            ObjectProvider<ReactiveGuardExecutor> reactiveExecutors
    ) {
        this.properties = Objects.requireNonNull(properties, "properties");
        this.planResolver = Objects.requireNonNull(planResolver, "planResolver");
        this.planValidator = Objects.requireNonNull(planValidator, "planValidator");
        this.bindingResolver = Objects.requireNonNull(bindingResolver, "bindingResolver");
        this.fallbackCache = Objects.requireNonNull(fallbackCache, "fallbackCache");
        this.jsonParser = Objects.requireNonNull(jsonParser, "jsonParser");
        this.beanFactory = Objects.requireNonNull(beanFactory, "beanFactory");
        this.integrations = Objects.requireNonNull(integrations, "integrations");
        this.storageIntegrations = Objects.requireNonNull(storageIntegrations, "storageIntegrations");
        this.reactiveExecutors = Objects.requireNonNull(reactiveExecutors, "reactiveExecutors");
    }

    @Override
    public void afterSingletonsInstantiated() {
        validateEngineIntegration();
        if (!properties.getRules().isEmpty()
                && (properties.getKey().getHmacSecret() == null
                || properties.getKey().getHmacSecret().isBlank())) {
            throw new IllegalStateException("Access Guard key HMAC secret must not be blank when rules are configured");
        }
        validateStorageIntegration();
        properties.getRules().keySet().forEach(planResolver::resolve);
        if (properties.getEngine() != AccessGuardEngine.DISABLED) {
            validateGuardedBeans();
        }
    }

    private void validateEngineIntegration() {
        if (properties.getEngine() != AccessGuardEngine.AGENT) {
            return;
        }
        List<AccessGuardAgentIntegration> installed = integrations.orderedStream().toList();
        if (installed.size() != 1) {
            throw new IllegalStateException(
                    "Access Guard engine=AGENT requires exactly one integration from "
                            + "egon-cola-component-bytecode-starter; found " + installed.size());
        }
    }

    private void validateStorageIntegration() {
        if (properties.getStorage() != GuardPlanProperties.Storage.REDISSON) {
            return;
        }
        List<AccessGuardStorageIntegration> installed = storageIntegrations.orderedStream()
                .filter(integration -> GuardPlanProperties.Storage.REDISSON.name().equals(integration.storage()))
                .toList();
        if (installed.size() != 1) {
            throw new IllegalStateException(
                    "REDISSON storage requires exactly one Access Guard Redisson integration; found "
                            + installed.size());
        }
    }

    private void validateGuardedBeans() {
        Set<Class<?>> types = new LinkedHashSet<>();
        for (String beanName : beanFactory.getBeanDefinitionNames()) {
            Class<?> beanType = beanFactory.getType(beanName, false);
            if (beanType != null) {
                types.add(ClassUtils.getUserClass(beanType));
            }
        }
        types.forEach(this::validateGuardedType);
    }

    private void validateGuardedType(Class<?> type) {
        if (properties.getEngine() == AccessGuardEngine.AOP) {
            for (Constructor<?> constructor : type.getDeclaredConstructors()) {
                if (constructor.isAnnotationPresent(AccessGuard.class)) {
                    throw new IllegalStateException(
                            "AOP mode does not support guarded constructor " + constructor.toGenericString());
                }
            }
        }
        for (Method method : allMethods(type)) {
            bindingResolver.resolve(method, type).ifPresent(binding -> validateBinding(method, binding));
        }
    }

    private void validateBinding(Method method, GuardBinding binding) {
        if (isReactive(method) && reactiveExecutors.orderedStream().count() != 1L) {
            throw new IllegalStateException(
                    "Reactive Access Guard method requires exactly one Reactor adapter: "
                            + method.toGenericString());
        }
        GuardPlan plan = planResolver.resolve(binding.ruleId()).plan();
        validateDedicatedBinding(binding, plan);
        planValidator.validateExecution(method, plan, fallbackCache, jsonParser);
    }

    private static boolean isReactive(Method method) {
        String returnType = method.getReturnType().getName();
        return "reactor.core.publisher.Mono".equals(returnType)
                || "reactor.core.publisher.Flux".equals(returnType);
    }

    private static void validateDedicatedBinding(GuardBinding binding, GuardPlan plan) {
        if (binding.kind() == GuardBinding.Kind.ACCESS) {
            return;
        }
        int enabled = enabledPolicyCount(plan);
        boolean expected = switch (binding.kind()) {
            case ALLOW_LIST -> plan.admission().allowList().enabled();
            case RATE_LIMIT -> plan.admission().rateLimit().enabled();
            case TIME_LIMIT -> plan.execution().timeLimit().enabled();
            case ACCESS -> true;
        };
        if (!expected || enabled != 1) {
            throw new IllegalStateException(
                    "A dedicated guard annotation must bind a single matching policy: " + binding.ruleId());
        }
    }

    private static int enabledPolicyCount(GuardPlan plan) {
        int count = 0;
        count += plan.admission().denyList().enabled() ? 1 : 0;
        count += plan.admission().allowList().enabled() ? 1 : 0;
        count += plan.admission().penaltyBox().enabled() ? 1 : 0;
        count += plan.admission().rateLimit().enabled() ? 1 : 0;
        count += plan.execution().timeLimit().enabled() ? 1 : 0;
        return count;
    }

    private static List<Method> allMethods(Class<?> type) {
        Set<Method> methods = new LinkedHashSet<>();
        for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
            methods.addAll(Arrays.asList(current.getDeclaredMethods()));
        }
        methods.addAll(Arrays.asList(type.getMethods()));
        return List.copyOf(methods);
    }
}
