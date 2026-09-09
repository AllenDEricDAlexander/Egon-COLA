package top.egon.cola.platform.tianquan.jianshen.admin.bootstrap.domain;

import top.egon.cola.platform.tianquan.jianshen.admin.bootstrap.domain.vo.ApplicationDefinitionVO;

import java.util.List;

/**
 * 本地开发环境使用的统一身份应用及管理权限拓扑。
 *
 * <p>Unified identity applications and administrative permission topology
 * used by the local development profile.</p>
 */
public final class Rbac3DevelopmentTopology {

    /** Tianquan-Jianshen 管理应用权限；Tianquan-Jianshen administration permissions.
     * 含义与用法：读取、传递或更新 `TIANQUAN_JIANSHEN_PERMISSIONS` 时应保持 `Rbac3DevelopmentTopology` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `TIANQUAN_JIANSHEN_PERMISSIONS`, preserve `Rbac3DevelopmentTopology`'s lifecycle, immutability, and thread-safety constraints.
     */
    private static final List<String> TIANQUAN_JIANSHEN_PERMISSIONS = List.of(
            "system:application:manage",
            "system:application:read",
            "system:audit:read",
            "system:authorization-constraint:manage",
            "system:authorization-constraint:read",
            "system:authorization-runtime:operate",
            "system:authorization-runtime:read",
            "system:authorization-simulation:execute",
            "system:bootstrap:read",
            "system:business:read",
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
            "system:organization:read",
            "system:organization:manage",
            "system:position:read",
            "system:position:manage",
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
            "system:role-resource:manage",
            "system:role-resource:read",
            "system:role:create",
            "system:role:read",
            "system:role:update",
            "system:tenant:target",
            "system:user-application-access:read",
            "system:user-business-access:manage",
            "system:user-business-access:read",
            "system:user-status:manage",
            "system:user-organization:read",
            "system:user-organization:manage",
            "system:user-position:read",
            "system:user-position:manage",
            "system:user:read",
            "system:user:manage");

    /** 本地应用定义；local application definitions.
     * 含义与用法：读取、传递或更新 `APPLICATIONS` 时应保持 `Rbac3DevelopmentTopology` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `APPLICATIONS`, preserve `Rbac3DevelopmentTopology`'s lifecycle, immutability, and thread-safety constraints.
     */
    private static final List<ApplicationDefinitionVO> APPLICATIONS = List.of(
            new ApplicationDefinitionVO(
                    "tianquan-jianshen-admin", "Tianquan-Jianshen Administration", "TIANQUAN_JIANSHEN_LOCAL_ADMIN",
                    0, TIANQUAN_JIANSHEN_PERMISSIONS),
            new ApplicationDefinitionVO(
                    "tianquan-shoubing-admin", "Tianquan-Shoubing Administration", "TIANQUAN_SHOUBING_LOCAL_ADMIN",
                    10, List.of(
                    "tianquan-shoubing:audit:read",
                    "tianquan-shoubing:bootstrap:read",
                    "tianquan-shoubing:identity:self:read",
                    "tianquan-shoubing:identity-user:create",
                    "tianquan-shoubing:identity-user:password-reset",
                    "tianquan-shoubing:identity-user:read",
                    "tianquan-shoubing:identity-user:revoke-all",
                    "tianquan-shoubing:identity-user:update",
                    "tianquan-shoubing:oauth-client:create",
                    "tianquan-shoubing:oauth-client:read",
                    "tianquan-shoubing:oauth-client:update",
                    "tianquan-shoubing:tenant:manage",
                    "tianquan-shoubing:tenant:read",
                    "tianquan-shoubing:resource-server:create",
                    "tianquan-shoubing:resource-server:grant",
                    "tianquan-shoubing:resource-server:key",
                    "tianquan-shoubing:resource-server:read",
                    "tianquan-shoubing:resource-server:status",
                    "tianquan-shoubing:resource-server:update",
                    "tianquan-shoubing:signing-key:activate",
                    "tianquan-shoubing:signing-key:publish",
                    "tianquan-shoubing:signing-key:read",
                    "tianquan-shoubing:signing-key:retire")),
            new ApplicationDefinitionVO(
                    "yuheng-admin", "Gateway Administration", "YUHENG_LOCAL_ADMIN",
                    20, List.of(
                    "yuheng:read",
                    "yuheng:applications:write",
                    "yuheng:catalog:write",
                    "yuheng:credentials:write",
                    "yuheng:drafts:write",
                    "yuheng:groups:write",
                    "yuheng:mcp:approve",
                    "yuheng:mcp:read",
                    "yuheng:mcp:runtime:read",
                    "yuheng:mcp:test",
                    "yuheng:mcp:write",
                    "yuheng:releases:write")),
            new ApplicationDefinitionVO(
                    "tianshu-admin", "Dynamic Configuration Administration",
                    "TIANSHU_LOCAL_ADMIN", 30,
                    List.of("TIANSHU_READ", "TIANSHU_WRITE", "TIANSHU_PUBLISH", "TIANSHU_CACHE")),
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
