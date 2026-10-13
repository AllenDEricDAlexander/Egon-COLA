package top.egon.cola.platform.tianquan.jianshen.admin.authorization.resource.domain.command;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/** Explicit mechanical deployment facts; this command carries no grants or field-write policy. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
public class DeploymentCatalogRegistrationCommand {
    public interface Registration { }

    @NotBlank(groups = Registration.class) @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._-]{0,127}", groups = Registration.class)
    private String businessCode;
    @NotBlank(groups = Registration.class) @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._-]{0,127}", groups = Registration.class)
    private String applicationCode;
    @NotBlank(groups = Registration.class) @Size(max = 256, groups = Registration.class)
    private String buildId;
    @NotBlank(groups = Registration.class) @Pattern(regexp = "[0-9a-f]{64}", groups = Registration.class)
    private String checksum;
    @NotNull(groups = Registration.class) @Min(value = 0, groups = Registration.class)
    private Long expectedApplicationVersion;
    @NotNull(groups = Registration.class) @Size(max = 64, groups = Registration.class) @Valid
    private List<@NotNull(groups = Registration.class) PermissionDefinitionBO> permissions;
    @NotNull(groups = Registration.class) @Size(max = 128, groups = Registration.class) @Valid
    private List<@NotNull(groups = Registration.class) ResourceDefinitionBO> resources;
    @NotNull(groups = Registration.class) @Size(max = 256, groups = Registration.class) @Valid
    private List<@NotNull(groups = Registration.class) FieldDefinitionBO> fields;

    @JsonIgnore
    @AssertTrue(groups = Registration.class, message = "Catalog codes must be unique and fields must refer to declared resources")
    public boolean isDefinitionSetValid() {
        if (permissions == null || resources == null || fields == null
                || permissions.stream().anyMatch(Objects::isNull) || resources.stream().anyMatch(Objects::isNull)
                || fields.stream().anyMatch(Objects::isNull)) { return false; }
        Set<String> permissionCodes = permissions.stream().map(PermissionDefinitionBO::getPermissionCode).collect(Collectors.toSet());
        Set<String> resourceCodes = resources.stream().map(ResourceDefinitionBO::getResourceCode).collect(Collectors.toSet());
        Set<List<String>> fieldCodes = fields.stream().map(field -> java.util.Arrays.asList(field.getResourceCode(), field.getFieldCode())).collect(Collectors.toSet());
        return permissionCodes.size() == permissions.size() && resourceCodes.size() == resources.size()
                && fieldCodes.size() == fields.size() && fields.stream().allMatch(field -> resourceCodes.contains(field.getResourceCode()));
    }

    public DeploymentCatalogRegistrationCommand setPermissions(List<PermissionDefinitionBO> values) {
        permissions = values == null ? null : java.util.Collections.unmodifiableList(new java.util.ArrayList<>(values)); return this;
    }
    public DeploymentCatalogRegistrationCommand setResources(List<ResourceDefinitionBO> values) {
        resources = values == null ? null : java.util.Collections.unmodifiableList(new java.util.ArrayList<>(values)); return this;
    }
    public DeploymentCatalogRegistrationCommand setFields(List<FieldDefinitionBO> values) {
        fields = values == null ? null : java.util.Collections.unmodifiableList(new java.util.ArrayList<>(values)); return this;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain = true) @Builder
    public static class PermissionDefinitionBO {
        @NotBlank(groups = Registration.class) @Size(max = 128, groups = Registration.class)
        @Pattern(regexp = "[A-Za-z0-9._-]+(?::[A-Za-z0-9._-]+)+", groups = Registration.class)
        private String permissionCode;
        @NotBlank(groups = Registration.class) @Size(max = 200, groups = Registration.class) private String permissionName;
        @NotBlank(groups = Registration.class) @Pattern(regexp = "LOW|MEDIUM|HIGH|CRITICAL", groups = Registration.class) private String riskLevel;
        @Size(max = 4096, groups = Registration.class) private String description;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain = true) @Builder
    public static class ResourceDefinitionBO {
        @NotBlank(groups = Registration.class) @Size(max = 128, groups = Registration.class)
        @Pattern(regexp = "[A-Za-z0-9._-]+", groups = Registration.class) private String resourceCode;
        @NotBlank(groups = Registration.class) @Size(max = 200, groups = Registration.class) private String name;
        @NotBlank(groups = Registration.class) @Pattern(regexp = "REST|GRAPHQL|RPC", groups = Registration.class) private String transport;
        @Pattern(regexp = "GET|POST|PUT|PATCH|DELETE|HEAD|OPTIONS", groups = Registration.class) private String method;
        @Size(max = 2048, groups = Registration.class) private String path;
        @Size(max = 128, groups = Registration.class) private String operationId;
        @Size(max = 512, groups = Registration.class) private String fullMethodName;
        @Size(max = 128, groups = Registration.class) private String suggestedPermissionCode;

        @JsonIgnore
        @AssertTrue(groups = Registration.class, message = "Catalog transport selectors must describe one bounded operation")
        public boolean isTransportValid() {
            if ("RPC".equals(transport)) {
                return method == null && path == null && operationId == null && fullMethodName != null
                        && fullMethodName.matches("[A-Za-z_][A-Za-z0-9_.]*/[A-Za-z_][A-Za-z0-9_]*");
            }
            return ("REST".equals(transport) || "GRAPHQL".equals(transport)) && method != null
                    && path != null && path.startsWith("/") && !path.startsWith("//") && !path.contains("?") && !path.contains("#")
                    && !path.contains("%") && !path.contains("\\") && path.indexOf('\r') < 0 && path.indexOf('\n') < 0
                    && operationId != null && operationId.matches("[A-Za-z][A-Za-z0-9_]{0,127}") && fullMethodName == null
                    && (!"GRAPHQL".equals(transport) || "POST".equals(method));
        }
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain = true) @Builder
    public static class FieldDefinitionBO {
        @NotBlank(groups = Registration.class) @Size(max = 128, groups = Registration.class) private String resourceCode;
        @NotBlank(groups = Registration.class) @Pattern(regexp = "[A-Za-z_][A-Za-z0-9._-]{0,127}", groups = Registration.class) private String fieldCode;
        @NotBlank(groups = Registration.class) @Size(max = 512, groups = Registration.class)
        @Pattern(regexp = "\\$\\.[A-Za-z_][A-Za-z0-9_.]*", groups = Registration.class) private String jsonPath;
        @NotBlank(groups = Registration.class) @Pattern(regexp = "STRING|NUMBER|BOOLEAN|DATE|DATETIME|OBJECT|ARRAY", groups = Registration.class) private String dataType;
    }
}
