package top.egon.cola.platform.rbac3.admin.bootstrap.application;

import java.util.List;

/** Unified identity applications and administrative permissions used by the local profile. */
public final class Rbac3DevelopmentTopology {

    private static final List<String> RBAC3_PERMISSIONS = List.of(
            "system:application:read",
            "system:audit:read",
            "system:authorization-constraint:manage",
            "system:authorization-constraint:read",
            "system:authorization-runtime:operate",
            "system:authorization-runtime:read",
            "system:authorization-simulation:execute",
            "system:bootstrap:read",
            "system:data-rule:manage",
            "system:data-rule:read",
            "system:directory-snapshot:read",
            "system:directory:read",
            "system:directory:sync",
            "system:field-rule:manage",
            "system:field-rule:read",
            "system:management-policy:manage",
            "system:management-policy:read",
            "system:operation-sod:manage",
            "system:operation-sod:read",
            "system:resource-manifest:activate",
            "system:resource-manifest:read",
            "system:resource-manifest:submit",
            "system:resource:archive",
            "system:resource:read",
            "system:role-activation:read",
            "system:role-activation:use",
            "system:role-assignment:manage",
            "system:role-assignment:read",
            "system:role-inheritance:manage",
            "system:role-permission:manage",
            "system:role:create",
            "system:role:read",
            "system:role:update",
            "system:session:logout",
            "system:session:read",
            "system:session:revoke",
            "system:tenant:manage",
            "system:tenant:read",
            "system:tenant:target",
            "system:user-status:manage",
            "system:user:read");

    private static final List<ApplicationDefinition> APPLICATIONS = List.of(
            new ApplicationDefinition(
                    "rbac3-admin", "RBAC3 Administration", "RBAC3_LOCAL_ADMIN",
                    0, RBAC3_PERMISSIONS),
            new ApplicationDefinition(
                    "idp-admin", "Identity Platform Administration", "IDP_LOCAL_ADMIN",
                    10, List.of(
                    "idp:audit:read",
                    "idp:bootstrap:read",
                    "idp:identity:self:read",
                    "idp:identity-user:create",
                    "idp:identity-user:password-reset",
                    "idp:identity-user:read",
                    "idp:identity-user:revoke-all",
                    "idp:identity-user:update",
                    "idp:oauth-client:create",
                    "idp:oauth-client:read",
                    "idp:oauth-client:update",
                    "idp:signing-key:activate",
                    "idp:signing-key:publish",
                    "idp:signing-key:read",
                    "idp:signing-key:retire")),
            new ApplicationDefinition(
                    "gateway-admin", "Gateway Administration", "GATEWAY_LOCAL_ADMIN",
                    20, List.of(
                    "gateway:read",
                    "gateway:applications:write",
                    "gateway:catalog:write",
                    "gateway:credentials:write",
                    "gateway:drafts:write",
                    "gateway:groups:write",
                    "gateway:releases:write")),
            new ApplicationDefinition(
                    "ddc-admin", "Dynamic Configuration Administration",
                    "DDC_LOCAL_ADMIN", 30,
                    List.of("DDC_READ", "DDC_WRITE", "DDC_PUBLISH", "DDC_CACHE")),
            new ApplicationDefinition(
                    "mock-backend", "Unified Identity Mock Backend",
                    "MOCK_LOCAL_ADMIN", 40,
                    List.of("mock:read", "mock:admin")));

    private Rbac3DevelopmentTopology() {
    }

    public static List<ApplicationDefinition> applications() {
        return APPLICATIONS;
    }

    public record ApplicationDefinition(
            String applicationCode,
            String applicationName,
            String roleCode,
            int displayPriority,
            List<String> permissions
    ) {
        public ApplicationDefinition {
            permissions = List.copyOf(permissions);
        }
    }
}
