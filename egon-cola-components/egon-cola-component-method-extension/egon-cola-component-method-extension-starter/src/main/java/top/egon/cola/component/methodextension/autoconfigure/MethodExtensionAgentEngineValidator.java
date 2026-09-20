package top.egon.cola.component.methodextension.autoconfigure;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.ClassUtils;
import top.egon.cola.component.common.core.validation.BaseValidator;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.methodextension.common.exception.MethodExtensionConfigurationException;

import java.util.Objects;
import java.util.function.BooleanSupplier;

/**
 * Fails startup when {@code engine=AGENT} is selected without the agent integration on the
 * classpath.
 *
 * <p>The AOP advisor only registers for {@code engine=AOP} and the agent adapter ships in the
 * bytecode starter, so this combination previously left nothing intercepting {@code @MethodExtension}
 * methods and the application started clean while every extension point was silently skipped.
 *
 * <p>The adapter is looked up by name because the dependency runs the other way: the bytecode
 * starter depends on this module, not the reverse.
 */
public class MethodExtensionAgentEngineValidator extends BaseValidator implements InitializingBean {

    static final String AGENT_ADAPTER_CLASS =
            "top.egon.cola.component.bytecode.starter.methodextension.MethodExtensionRuntimeAdapter";

    private final MethodExtensionProperties properties;

    private final ValidationUtils validationUtils;

    private final BooleanSupplier agentIntegrationPresent;

    public MethodExtensionAgentEngineValidator(MethodExtensionProperties properties,
                                               ValidationUtils validationUtils) {
        this(properties, validationUtils, () -> ClassUtils.isPresent(
                AGENT_ADAPTER_CLASS, MethodExtensionAgentEngineValidator.class.getClassLoader()));
    }

    MethodExtensionAgentEngineValidator(MethodExtensionProperties properties,
                                        ValidationUtils validationUtils,
                                        BooleanSupplier agentIntegrationPresent) {
        this.properties = Objects.requireNonNull(properties, "properties");
        this.validationUtils = Objects.requireNonNull(validationUtils, "validationUtils");
        this.agentIntegrationPresent = Objects.requireNonNull(
                agentIntegrationPresent, "agentIntegrationPresent");
    }

    @Override
    protected ValidationUtils getValidationUtils() {
        return validationUtils;
    }

    @Override
    public void afterPropertiesSet() {
        validateBean(properties);
        if (properties.effectiveEngine() != MethodExtensionEngine.AGENT) {
            return;
        }
        if (agentIntegrationPresent.getAsBoolean()) {
            return;
        }
        throw new MethodExtensionConfigurationException(
                "egon.cola.component.method-extension.engine=AGENT requires the bytecode starter"
                        + " (egon-cola-component-bytecode-starter) and the Egon bytecode agent."
                        + " Add that dependency, or set engine=AOP to intercept through Spring AOP.");
    }
}
