package top.egon.cola.component.methodextension.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.Ordered;

@ConfigurationProperties(prefix = "egon.cola.component.method-extension", ignoreInvalidFields = true)
public class MethodExtensionProperties {

    private boolean enabled = true;

    private int order = Ordered.HIGHEST_PRECEDENCE + 100;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }
}
