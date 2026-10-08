package top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import top.egon.cola.platform.tianquan.shoubing.core.resource.ResourceGrantType;

/** Input for adding or deleting Client Resource Grants for explicit applications. */
@Data
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Accessors(chain = true)
public class BatchClientResourceGrantDTO {

    @NotBlank
    private String bizCode;
    @NotBlank
    private String environment;
    @NotEmpty
    private List<@NotBlank String> appCodes;
    @NotNull
    private Action action;
    @NotNull
    private ResourceGrantType grantType;
    private String tenantId;
    @NotNull
    private Set<String> allowedScopes;
    @NotNull
    private Map<String, Long> expectedResourceVersions;
    @NotNull
    private Map<String, Long> expectedGrantVersions;

    /** Builds an input with defensive copies of its collections. */
    @Builder
    public static BatchClientResourceGrantDTO of(String bizCode, String environment, List<String> appCodes,
            Action action, ResourceGrantType grantType, String tenantId, Set<String> allowedScopes,
            Map<String, Long> expectedResourceVersions, Map<String, Long> expectedGrantVersions) {
        return new BatchClientResourceGrantDTO(bizCode, environment,
                appCodes == null ? null : List.copyOf(appCodes), action, grantType, tenantId,
                allowedScopes == null ? null : Set.copyOf(allowedScopes),
                expectedResourceVersions == null ? null : Map.copyOf(expectedResourceVersions),
                expectedGrantVersions == null ? null : Map.copyOf(expectedGrantVersions));
    }

    public List<String> getAppCodes() {
        return appCodes == null ? null : List.copyOf(appCodes);
    }

    public BatchClientResourceGrantDTO setAppCodes(List<String> values) {
        this.appCodes = values == null ? null : List.copyOf(values);
        return this;
    }

    public Set<String> getAllowedScopes() {
        return allowedScopes == null ? null : Set.copyOf(allowedScopes);
    }

    public BatchClientResourceGrantDTO setAllowedScopes(Set<String> values) {
        this.allowedScopes = values == null ? null : Set.copyOf(values);
        return this;
    }

    public Map<String, Long> getExpectedResourceVersions() {
        return expectedResourceVersions == null ? null : Map.copyOf(expectedResourceVersions);
    }

    public BatchClientResourceGrantDTO setExpectedResourceVersions(Map<String, Long> values) {
        this.expectedResourceVersions = values == null ? null : Map.copyOf(values);
        return this;
    }

    public Map<String, Long> getExpectedGrantVersions() {
        return expectedGrantVersions == null ? null : Map.copyOf(expectedGrantVersions);
    }

    public BatchClientResourceGrantDTO setExpectedGrantVersions(Map<String, Long> values) {
        this.expectedGrantVersions = values == null ? null : Map.copyOf(values);
        return this;
    }

    /** Grant batch action. */
    public enum Action {
        /** Create or update per-application Grants. */
        UPSERT,
        /** Delete per-application Grants. */
        DELETE
    }
}
