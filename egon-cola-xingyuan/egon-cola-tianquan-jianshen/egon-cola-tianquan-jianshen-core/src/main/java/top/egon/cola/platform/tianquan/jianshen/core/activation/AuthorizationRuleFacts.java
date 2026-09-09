package top.egon.cola.platform.tianquan.jianshen.core.activation;

import top.egon.cola.platform.tianquan.jianshen.contract.authorization.FieldAccessLevel;

import java.util.List;

public record AuthorizationRuleFacts(
        List<ResourceGrantBinding> resourceGrantBindings,
        List<DataScopeFact> dataScopeFacts,
        List<FieldRuleFact> fieldRuleFacts,
        List<FieldDefinitionFact> fieldDefinitions,
        List<ResourceFact> resources,
        List<LandingRouteFact> landingRoutes
) {

    public AuthorizationRuleFacts(
            List<ResourceGrantBinding> resourceGrantBindings,
            List<DataScopeFact> dataScopeFacts,
            List<FieldRuleFact> fieldRuleFacts,
            List<FieldDefinitionFact> fieldDefinitions,
            List<ResourceFact> resources
    ) {
        this(resourceGrantBindings, dataScopeFacts, fieldRuleFacts,
                fieldDefinitions, resources, List.of());
    }

    public AuthorizationRuleFacts {
        resourceGrantBindings = List.copyOf(resourceGrantBindings);
        dataScopeFacts = List.copyOf(dataScopeFacts);
        fieldRuleFacts = List.copyOf(fieldRuleFacts);
        fieldDefinitions = List.copyOf(fieldDefinitions);
        resources = List.copyOf(resources);
        landingRoutes = List.copyOf(landingRoutes);
    }

    public record ResourceGrantBinding(
            String roleId,
            String resourceId,
            String resourceCode,
            String resourceType,
            String permissionCode) {
        public ResourceGrantBinding {
            roleId = required(roleId, "roleId");
            resourceId = required(resourceId, "resourceId");
            resourceCode = required(resourceCode, "resourceCode");
            resourceType = required(resourceType, "resourceType");
            permissionCode = optional(permissionCode);
        }

        /** Compatibility constructor for pure algebra fixtures that only model a permission. */
        public ResourceGrantBinding(String roleId, String permissionCode) {
            this(roleId, roleId + ":" + permissionCode, permissionCode,
                    "API", permissionCode);
        }
    }

    public record DataScopeFact(
            String roleId,
            String permissionCode,
            String dimension,
            String referenceId,
            long directorySnapshotVersion
    ) {
        public DataScopeFact {
            roleId = required(roleId, "roleId");
            permissionCode = required(permissionCode, "permissionCode");
            dimension = required(dimension, "dimension");
            if (directorySnapshotVersion < 0) {
                throw new IllegalArgumentException("directorySnapshotVersion must not be negative");
            }
        }
    }

    public record FieldRuleFact(
            String roleId,
            String resourceCode,
            String fieldCode,
            FieldAccessLevel accessLevel
    ) {
        public FieldRuleFact {
            roleId = required(roleId, "roleId");
            resourceCode = required(resourceCode, "resourceCode");
            fieldCode = required(fieldCode, "fieldCode");
            if (accessLevel == null) {
                throw new IllegalArgumentException("accessLevel is required");
            }
        }
    }

    public record FieldDefinitionFact(
            String resourceCode,
            String fieldCode,
            FieldAccessLevel maximumAccess
    ) {
        public FieldDefinitionFact {
            resourceCode = required(resourceCode, "resourceCode");
            fieldCode = required(fieldCode, "fieldCode");
            if (maximumAccess == null) {
                throw new IllegalArgumentException("maximumAccess is required");
            }
        }
    }

    public record ResourceFact(
            String resourceId,
            String code,
            String resourceType,
            String parentCode,
            String requiredPermissionCode) {
        public ResourceFact {
            resourceId = required(resourceId, "resourceId");
            code = required(code, "code");
            resourceType = required(resourceType, "resourceType");
            parentCode = optional(parentCode);
            requiredPermissionCode = optional(requiredPermissionCode);
        }

        public ResourceFact(String code, String requiredPermissionCode) {
            this(code, code, "API", null, requiredPermissionCode);
        }
    }

    public record LandingRouteFact(
            String roleId,
            String routeCode,
            int priority,
            String requiredPermissionCode
    ) {
        public LandingRouteFact {
            roleId = required(roleId, "roleId");
            routeCode = required(routeCode, "routeCode");
            requiredPermissionCode = required(requiredPermissionCode,
                    "requiredPermissionCode");
            if (priority < 0) {
                throw new IllegalArgumentException("priority must not be negative");
            }
        }
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    private static String optional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
