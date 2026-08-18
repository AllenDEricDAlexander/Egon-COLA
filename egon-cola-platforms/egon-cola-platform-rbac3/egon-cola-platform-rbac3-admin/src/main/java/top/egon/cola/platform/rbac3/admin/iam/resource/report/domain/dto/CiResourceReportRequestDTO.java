package top.egon.cola.platform.rbac3.admin.iam.resource.report.domain.dto;

import top.egon.cola.platform.rbac3.admin.iam.resource.report.domain.FrontendResourceType;

import java.util.List;
import java.util.Objects;

/** Complete local frontend registry projection sent by a release pipeline. */
public record CiResourceReportRequestDTO(
        String buildId,
        String checksum,
        long expectedApplicationVersion,
        List<Resource> resources,
        List<Field> fields) {

    public CiResourceReportRequestDTO {
        buildId = required(buildId, "buildId");
        checksum = required(checksum, "checksum");
        if (expectedApplicationVersion < 0L) {
            throw new IllegalArgumentException("expectedApplicationVersion must not be negative");
        }
        resources = List.copyOf(Objects.requireNonNull(resources, "resources"));
        fields = List.copyOf(Objects.requireNonNull(fields, "fields"));
        if (resources.size() > 2_000) {
            throw new IllegalArgumentException("resources must contain at most 2000 entries");
        }
        if (fields.size() > 5_000) {
            throw new IllegalArgumentException("fields must contain at most 5000 entries");
        }
    }

    public record Resource(
            FrontendResourceType type,
            String code,
            String name,
            String parentCode,
            String permissionCode,
            String path,
            String componentKey,
            String routeCode,
            Integer order,
            boolean hidden) {

        public Resource {
            type = Objects.requireNonNull(type, "type");
            code = required(code, "code");
            name = required(name, "name");
            permissionCode = optional(permissionCode, "permissionCode");
            path = optional(path, "path");
            componentKey = optional(componentKey, "componentKey");
            routeCode = optional(routeCode, "routeCode");
            if (order != null && order < 0) {
                throw new IllegalArgumentException("order must not be negative");
            }
            if (type == FrontendResourceType.ROUTE
                    && (path == null || componentKey == null || permissionCode == null)) {
                throw new IllegalArgumentException(
                        "ROUTE requires path, componentKey and permissionCode");
            }
            if (type == FrontendResourceType.ACTION
                    && (routeCode == null || permissionCode == null)) {
                throw new IllegalArgumentException(
                        "ACTION requires routeCode and permissionCode");
            }
        }
    }

    public record Field(
            String resourceCode,
            String fieldCode,
            String jsonPath,
            String dataType) {

        public Field {
            resourceCode = required(resourceCode, "resourceCode");
            fieldCode = required(fieldCode, "fieldCode");
            jsonPath = required(jsonPath, "jsonPath");
            dataType = required(dataType, "dataType");
        }
    }

    private static String required(String value, String name) {
        if (value == null || value.isBlank() || !value.equals(value.trim())) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value;
    }

    private static String optional(String value, String name) {
        if (value == null) {
            return null;
        }
        if (value.isBlank() || !value.equals(value.trim())) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
