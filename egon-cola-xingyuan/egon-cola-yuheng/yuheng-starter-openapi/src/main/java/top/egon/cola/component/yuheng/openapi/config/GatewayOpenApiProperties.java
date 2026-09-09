package top.egon.cola.component.yuheng.openapi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.yuheng.contract.reporting.openapi.GatewayOpenApiGroupManifestDTO;

import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

/**
 * Typed Provider-side OpenAPI publication configuration.
 */
@Validated
@ConfigurationProperties("egon.cola.component.gateway.openapi")
public class GatewayOpenApiProperties {

    private boolean enabled;

    private boolean publishToDdc = true;

    @Size(max = GatewayOpenApiGroupManifestDTO.MAX_GROUPS)
    private List<String> publishedGroups = new ArrayList<>();

    private String bizCode;

    private String applicationCode;

    private String resourceUri;

    private String artifactVersion;

    private String buildId;

    /**
     * Validates and normalizes all values required by an enabled provider.
     */
    public void validate() {
        if (!enabled) {
            return;
        }
        required(bizCode, "bizCode");
        required(applicationCode, "applicationCode");
        GatewayOpenApiGroupManifestDTO manifest =
                new GatewayOpenApiGroupManifestDTO(
                        publishedGroups,
                        GatewayOpenApiGroupManifestDTO.PATH_TEMPLATE,
                        resourceUri,
                        artifactVersion,
                        buildId
                );
        publishedGroups = new ArrayList<>(manifest.groups());
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isPublishToDdc() {
        return publishToDdc;
    }

    public void setPublishToDdc(boolean publishToDdc) {
        this.publishToDdc = publishToDdc;
    }

    public List<String> getPublishedGroups() {
        return List.copyOf(publishedGroups);
    }

    public GatewayOpenApiGroupManifestDTO groupManifest() {
        validate();
        return new GatewayOpenApiGroupManifestDTO(
                publishedGroups,
                GatewayOpenApiGroupManifestDTO.PATH_TEMPLATE,
                resourceUri,
                artifactVersion,
                buildId
        );
    }

    public void setPublishedGroups(List<String> publishedGroups) {
        this.publishedGroups = publishedGroups == null
                ? new ArrayList<>()
                : new ArrayList<>(publishedGroups);
    }

    public String getBizCode() {
        return bizCode;
    }

    public void setBizCode(String bizCode) {
        this.bizCode = bizCode;
    }

    public String getApplicationCode() {
        return applicationCode;
    }

    public void setApplicationCode(String applicationCode) {
        this.applicationCode = applicationCode;
    }

    public String getResourceUri() {
        return resourceUri;
    }

    public void setResourceUri(String resourceUri) {
        this.resourceUri = resourceUri;
    }

    public String getArtifactVersion() {
        return artifactVersion;
    }

    public void setArtifactVersion(String artifactVersion) {
        this.artifactVersion = artifactVersion;
    }

    public String getBuildId() {
        return buildId;
    }

    public void setBuildId(String buildId) {
        this.buildId = buildId;
    }

    private static void required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "gateway openapi " + field + " is required"
            );
        }
    }
}
