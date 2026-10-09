package top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.Set;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import top.egon.cola.platform.tianquan.shoubing.core.resource.ResourceGrantType;

/** Input for creating or updating one application-level Client Resource Grant. */
@Data
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Accessors(chain = true)
public class UpsertClientResourceGrantDTO {

    @NotNull
    private ResourceGrantType grantType;
    private String tenantId;
    @NotNull
    @Size(max = 64)
    private Set<@NotBlank @Size(max = 128) @Pattern(regexp = "[A-Za-z0-9._:-]{1,128}") String> allowedScopes;
    @PositiveOrZero
    private long expectedResourceVersion;
    @PositiveOrZero
    private Long expectedGrantVersion;

    /** Builds a request with a defensive copy of its scope set. */
    @Builder
    public static UpsertClientResourceGrantDTO of(ResourceGrantType grantType, String tenantId,
            Set<String> allowedScopes, long expectedResourceVersion, Long expectedGrantVersion) {
        return new UpsertClientResourceGrantDTO(grantType, tenantId,
                allowedScopes == null ? null : Set.copyOf(allowedScopes),
                expectedResourceVersion, expectedGrantVersion);
    }

    public Set<String> getAllowedScopes() {
        return allowedScopes == null ? null : Set.copyOf(allowedScopes);
    }

    public UpsertClientResourceGrantDTO setAllowedScopes(Set<String> values) {
        this.allowedScopes = values == null ? null : Set.copyOf(values);
        return this;
    }
}
