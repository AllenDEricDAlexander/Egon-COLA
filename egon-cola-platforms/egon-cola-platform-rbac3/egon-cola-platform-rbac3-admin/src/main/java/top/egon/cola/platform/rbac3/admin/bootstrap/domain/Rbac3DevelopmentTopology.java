package top.egon.cola.platform.rbac3.admin.bootstrap.domain;

import top.egon.cola.platform.rbac3.admin.bootstrap.domain.vo.ApplicationDefinitionVO;

import java.util.List;

/**
 * 本地开发环境使用的统一身份应用及管理权限拓扑。
 *
 * <p>Unified identity applications and administrative permission topology
 * used by the local development profile.</p>
 */
public final class Rbac3DevelopmentTopology {

    /** RBAC3 管理应用权限；RBAC3 administration permissions.
     * 含义与用法：读取、传递或更新 `RBAC3_PERMISSIONS` 时应保持 `Rbac3DevelopmentTopology` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `RBAC3_PERMISSIONS`, preserve `Rbac3DevelopmentTopology`'s lifecycle, immutability, and thread-safety constraints.
     */
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
            "system:tenant:manage",
            "system:tenant:read",
            "system:tenant:target",
            "system:user-status:manage",
            "system:user:read");

    /** 本地应用定义；local application definitions.
     * 含义与用法：读取、传递或更新 `APPLICATIONS` 时应保持 `Rbac3DevelopmentTopology` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `APPLICATIONS`, preserve `Rbac3DevelopmentTopology`'s lifecycle, immutability, and thread-safety constraints.
     */
    private static final List<ApplicationDefinitionVO> APPLICATIONS = List.of(
            new ApplicationDefinitionVO(
                    "rbac3-admin", "RBAC3 Administration", "RBAC3_LOCAL_ADMIN",
                    0, RBAC3_PERMISSIONS),
            new ApplicationDefinitionVO(
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
                    "idp:resource-server:create",
                    "idp:resource-server:grant",
                    "idp:resource-server:key",
                    "idp:resource-server:read",
                    "idp:resource-server:status",
                    "idp:resource-server:update",
                    "idp:signing-key:activate",
                    "idp:signing-key:publish",
                    "idp:signing-key:read",
                    "idp:signing-key:retire")),
            new ApplicationDefinitionVO(
                    "gateway-admin", "Gateway Administration", "GATEWAY_LOCAL_ADMIN",
                    20, List.of(
                    "gateway:read",
                    "gateway:applications:write",
                    "gateway:catalog:write",
                    "gateway:credentials:write",
                    "gateway:drafts:write",
                    "gateway:groups:write",
                    "gateway:mcp:approve",
                    "gateway:mcp:read",
                    "gateway:mcp:runtime:read",
                    "gateway:mcp:test",
                    "gateway:mcp:write",
                    "gateway:releases:write")),
            new ApplicationDefinitionVO(
                    "ddc-admin", "Dynamic Configuration Administration",
                    "DDC_LOCAL_ADMIN", 30,
                    List.of("DDC_READ", "DDC_WRITE", "DDC_PUBLISH", "DDC_CACHE")),
            new ApplicationDefinitionVO(
                    "mock-backend", "Unified Identity Mock Backend",
                    "MOCK_LOCAL_ADMIN", 40,
                    List.of(
                    "mock:read",
                    "mock:admin",
                    "mcp:unified-local:tool:local_query:call",
                    "mcp:unified-local:tool:local_echo_task:call",
                    "mcp:unified-local:tool:local_echo_task:task:get",
                    "mcp:unified-local:tool:local_echo_task:task:update",
                    "mcp:unified-local:tool:local_echo_task:task:cancel",
                    "mcp:unified-local:tool:high_risk_query:call",
                    "mcp:unified-local:tool:stable.remote_echo:call",
                    "mcp:unified-local:tool:rc.remote_echo:call",
                    "mcp:unified-local:resource:local_status:read",
                    "mcp:unified-local:resource:stable.remote_text:read",
                    "mcp:unified-local:resource:local_item:read",
                    "mcp:unified-local:resource:qa_dashboard:read",
                    "mcp:unified-local:prompt:review_item:get",
                    "mcp:unified-local:prompt:rc.remote_summary:get")),
            new ApplicationDefinitionVO(
                    "mock-backend", "Unified Identity Mock Backend",
                    "MOCK_LOCAL_ENTRY", 40,
                    List.of("mock:read")));

    /** 禁止实例化静态拓扑；prevents instantiation of the static topology.
     * 用法：通过 `Rbac3DevelopmentTopology` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `Rbac3DevelopmentTopology`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     */
    private Rbac3DevelopmentTopology() {
    }

    /**
     * 返回本地应用拓扑。
     *
     * @return 不可变应用定义；immutable application definitions
     * 用法：调用 `applications` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `applications`, then continue the business flow using its result, exception, or side effect.
     */
    public static List<ApplicationDefinitionVO> applications() {
        return APPLICATIONS;
    }

    }
