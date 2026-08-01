package top.egon.cola.platform.rbac3.admin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Set;

@ConfigurationProperties(prefix = "egon.rbac3")
public class Rbac3AdminProperties {

    private Duration accessTokenTtl = Duration.ofMinutes(15);
    private Duration refreshTokenTtl = Duration.ofDays(7);
    private Duration sessionIdleTimeout = Duration.ofMinutes(30);
    private Duration sessionAbsoluteTimeout = Duration.ofHours(12);
    private int maximumActiveRoots = 16;
    private boolean platformTargetingEnabled;
    private Set<String> componentKeys = new LinkedHashSet<>();

    public Duration getAccessTokenTtl() {
        return accessTokenTtl;
    }

    public void setAccessTokenTtl(Duration accessTokenTtl) {
        this.accessTokenTtl = accessTokenTtl;
    }

    public Duration getRefreshTokenTtl() {
        return refreshTokenTtl;
    }

    public void setRefreshTokenTtl(Duration refreshTokenTtl) {
        this.refreshTokenTtl = refreshTokenTtl;
    }

    public Duration getSessionIdleTimeout() {
        return sessionIdleTimeout;
    }

    public void setSessionIdleTimeout(Duration sessionIdleTimeout) {
        if (sessionIdleTimeout == null || sessionIdleTimeout.isNegative()
                || sessionIdleTimeout.isZero()) {
            throw new IllegalArgumentException("sessionIdleTimeout must be positive");
        }
        this.sessionIdleTimeout = sessionIdleTimeout;
    }

    public Duration getSessionAbsoluteTimeout() {
        return sessionAbsoluteTimeout;
    }

    public void setSessionAbsoluteTimeout(Duration sessionAbsoluteTimeout) {
        if (sessionAbsoluteTimeout == null || sessionAbsoluteTimeout.isNegative()
                || sessionAbsoluteTimeout.isZero()) {
            throw new IllegalArgumentException("sessionAbsoluteTimeout must be positive");
        }
        this.sessionAbsoluteTimeout = sessionAbsoluteTimeout;
    }

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
