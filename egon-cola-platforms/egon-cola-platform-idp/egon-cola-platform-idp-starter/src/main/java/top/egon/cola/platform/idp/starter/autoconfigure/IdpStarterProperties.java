package top.egon.cola.platform.idp.starter.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashSet;
import java.util.Set;

@ConfigurationProperties("egon.cola.platform.idp")
public class IdpStarterProperties {

    private boolean enabled;
    private boolean registerFilter = true;
    private String issuer;
    private String jwkSetUri;
    private Set<String> audiences = new LinkedHashSet<>();
    private Set<String> clientIds = new LinkedHashSet<>();
    private String userStateKeyPrefix = "identity:v1:user:";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isRegisterFilter() {
        return registerFilter;
    }

    public void setRegisterFilter(boolean registerFilter) {
        this.registerFilter = registerFilter;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getJwkSetUri() {
        return jwkSetUri;
    }

    public void setJwkSetUri(String jwkSetUri) {
        this.jwkSetUri = jwkSetUri;
    }

    public Set<String> getAudiences() {
        return audiences;
    }

    public void setAudiences(Set<String> audiences) {
        this.audiences = audiences;
    }

    public Set<String> getClientIds() {
        return clientIds;
    }

    public void setClientIds(Set<String> clientIds) {
        this.clientIds = clientIds;
    }

    public String getUserStateKeyPrefix() {
        return userStateKeyPrefix;
    }

    public void setUserStateKeyPrefix(String userStateKeyPrefix) {
        this.userStateKeyPrefix = userStateKeyPrefix;
    }

    public void validate() {
        required(issuer, "issuer");
        required(jwkSetUri, "jwkSetUri");
        required(userStateKeyPrefix, "userStateKeyPrefix");
        requiredValues(audiences, "audiences");
        requiredValues(clientIds, "clientIds");
    }

    private void required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "egon.cola.platform.idp." + field + " is required");
        }
    }

    private void requiredValues(Set<String> values, String field) {
        if (values == null || values.isEmpty()
                || values.stream().anyMatch(
                        value -> value == null || value.isBlank())) {
            throw new IllegalStateException(
                    "egon.cola.platform.idp." + field
                            + " must contain non-blank values");
        }
    }
}
