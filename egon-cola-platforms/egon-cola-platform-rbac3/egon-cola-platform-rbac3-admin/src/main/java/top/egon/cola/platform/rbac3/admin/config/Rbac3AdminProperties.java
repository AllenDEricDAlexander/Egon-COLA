package top.egon.cola.platform.rbac3.admin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "egon.rbac3")
public class Rbac3AdminProperties {

    private Duration accessTokenTtl = Duration.ofMinutes(15);
    private Duration refreshTokenTtl = Duration.ofDays(7);
    private int maximumActiveRoots = 16;
    private boolean platformTargetingEnabled;

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
}
