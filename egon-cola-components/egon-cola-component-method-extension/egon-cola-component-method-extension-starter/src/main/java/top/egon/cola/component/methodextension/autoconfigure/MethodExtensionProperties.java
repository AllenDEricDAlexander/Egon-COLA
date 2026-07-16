package top.egon.cola.component.methodextension.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.Ordered;

@ConfigurationProperties(prefix = "egon.cola.component.method-extension", ignoreInvalidFields = true)
public class MethodExtensionProperties {

    private boolean enabled = true;

    private MethodExtensionEngine engine = MethodExtensionEngine.AOP;

    private MethodExtensionNotReadyPolicy notReadyPolicy =
            MethodExtensionNotReadyPolicy.PROCEED;

    private int order = Ordered.HIGHEST_PRECEDENCE + 100;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public MethodExtensionEngine getEngine() {
        return engine;
    }

    public void setEngine(MethodExtensionEngine engine) {
        this.engine = engine == null ? MethodExtensionEngine.AOP : engine;
    }

    public MethodExtensionNotReadyPolicy getNotReadyPolicy() {
        return notReadyPolicy;
    }

    public void setNotReadyPolicy(MethodExtensionNotReadyPolicy notReadyPolicy) {
        this.notReadyPolicy = notReadyPolicy == null
                ? MethodExtensionNotReadyPolicy.PROCEED : notReadyPolicy;
    }

    public MethodExtensionEngine effectiveEngine() {
        return enabled ? engine : MethodExtensionEngine.DISABLED;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }
}
