package top.egon.cola.platform.rbac3.admin.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * RBAC3 administration settings. Token issuance, refresh-token lifetime and
 * user authentication state are owned by IdP and are intentionally absent.
 */
@ConfigurationProperties(prefix = "egon.rbac3")
public class Rbac3AdminProperties {

    private int maximumActiveRoots = 16;
    private boolean platformTargetingEnabled;
    private Set<String> componentKeys = new LinkedHashSet<>();

    public int getMaximumActiveRoots() {
        return maximumActiveRoots;
    }

    public void setMaximumActiveRoots(int maximumActiveRoots) {
        if (maximumActiveRoots < 1 || maximumActiveRoots > 32) {
            throw new IllegalArgumentException("maximumActiveRoots must be between 1 and 32");
        }
        this.maximumActiveRoots = maximumActiveRoots;
    }

    public boolean isPlatformTargetingEnabled() {
        return platformTargetingEnabled;
    }

    public void setPlatformTargetingEnabled(boolean platformTargetingEnabled) {
        this.platformTargetingEnabled = platformTargetingEnabled;
    }

    public Set<String> getComponentKeys() {
        return Set.copyOf(componentKeys);
    }

    public void setComponentKeys(Set<String> componentKeys) {
        this.componentKeys = new LinkedHashSet<>(componentKeys == null
                ? Set.of() : componentKeys);
    }
}
