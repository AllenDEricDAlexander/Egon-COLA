package top.egon.cola.platform.rbac3.core.activation;

import top.egon.cola.platform.rbac3.contract.authorization.FieldAccessLevel;

import java.util.List;

public record AuthorizationRuleFacts(
        List<PermissionBinding> permissionBindings,
        List<DataScopeFact> dataScopeFacts,
        List<FieldRuleFact> fieldRuleFacts,
        List<FieldDefinitionFact> fieldDefinitions,
        List<ResourceFact> resources,
        List<LandingRouteFact> landingRoutes
) {

    public AuthorizationRuleFacts(
            List<PermissionBinding> permissionBindings,
            List<DataScopeFact> dataScopeFacts,
            List<FieldRuleFact> fieldRuleFacts,
            List<FieldDefinitionFact> fieldDefinitions,
            List<ResourceFact> resources
    ) {
        this(permissionBindings, dataScopeFacts, fieldRuleFacts,
                fieldDefinitions, resources, List.of());
    }

    public AuthorizationRuleFacts {
        permissionBindings = List.copyOf(permissionBindings);
        dataScopeFacts = List.copyOf(dataScopeFacts);
        fieldRuleFacts = List.copyOf(fieldRuleFacts);
        fieldDefinitions = List.copyOf(fieldDefinitions);
        resources = List.copyOf(resources);
        landingRoutes = List.copyOf(landingRoutes);
    }

    public record PermissionBinding(String roleId, String permissionCode) {
        public PermissionBinding {
            roleId = required(roleId, "roleId");
            permissionCode = required(permissionCode, "permissionCode");
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

    public record ResourceFact(String code, String requiredPermissionCode) {
        public ResourceFact {
            code = required(code, "code");
            requiredPermissionCode = required(requiredPermissionCode,
                    "requiredPermissionCode");
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
}
