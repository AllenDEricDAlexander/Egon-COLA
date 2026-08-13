package top.egon.cola.platform.rbac3.admin.bootstrap.application;

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
            "system:session:logout",
            "system:session:read",
            "system:session:revoke",
            "system:tenant:manage",
            "system:tenant:read",
            "system:tenant:target",
            "system:user-status:manage",
            "system:user:read");

    /** 本地应用定义；local application definitions.
     * 含义与用法：读取、传递或更新 `APPLICATIONS` 时应保持 `Rbac3DevelopmentTopology` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `APPLICATIONS`, preserve `Rbac3DevelopmentTopology`'s lifecycle, immutability, and thread-safety constraints.
     */
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
            new ApplicationDefinition(
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
            new ApplicationDefinition(
                    "ddc-admin", "Dynamic Configuration Administration",
                    "DDC_LOCAL_ADMIN", 30,
                    List.of("DDC_READ", "DDC_WRITE", "DDC_PUBLISH", "DDC_CACHE")),
            new ApplicationDefinition(
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
            new ApplicationDefinition(
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
    public static List<ApplicationDefinition> applications() {
        return APPLICATIONS;
    }

    /**
     * 一个应用及其本地角色权限定义。
     *
     * <p>Defines one application and one local role's permissions.</p>
     *
     * @param applicationCode 应用编码；application code
     * @param applicationName 应用名称；application name
     * @param roleCode 本地角色编码；local role code
     * @param displayPriority 展示优先级；display priority
     * @param permissions 角色权限；role permissions
     * 语义与用法：将 `ApplicationDefinition` 作为 `Rbac3DevelopmentTopology` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `ApplicationDefinition` as the responsibility boundary of `Rbac3DevelopmentTopology`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public record ApplicationDefinition(
            /**
             * 字段 `applicationCode` 表示 `ApplicationDefinition` 中与 `application Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `applicationCode` stores the `application Code`-related state, dependency, configuration, or result of `ApplicationDefinition` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `applicationCode` 时应保持 `ApplicationDefinition` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `applicationCode`, preserve `ApplicationDefinition`'s lifecycle, immutability, and thread-safety constraints.
             */
            String applicationCode,
            /**
             * 字段 `applicationName` 表示 `ApplicationDefinition` 中与 `application Name` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `applicationName` stores the `application Name`-related state, dependency, configuration, or result of `ApplicationDefinition` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `applicationName` 时应保持 `ApplicationDefinition` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `applicationName`, preserve `ApplicationDefinition`'s lifecycle, immutability, and thread-safety constraints.
             */
            String applicationName,
            /**
             * 字段 `roleCode` 表示 `ApplicationDefinition` 中与 `role Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `roleCode` stores the `role Code`-related state, dependency, configuration, or result of `ApplicationDefinition` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `roleCode` 时应保持 `ApplicationDefinition` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `roleCode`, preserve `ApplicationDefinition`'s lifecycle, immutability, and thread-safety constraints.
             */
            String roleCode,
            /**
             * 字段 `displayPriority` 表示 `ApplicationDefinition` 中与 `display Priority` 相关的状态、依赖、配置或结果（声明类型 `int`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `displayPriority` stores the `display Priority`-related state, dependency, configuration, or result of `ApplicationDefinition` (declared type `int`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `displayPriority` 时应保持 `ApplicationDefinition` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `displayPriority`, preserve `ApplicationDefinition`'s lifecycle, immutability, and thread-safety constraints.
             */
            int displayPriority,
            /**
             * 字段 `permissions` 表示 `ApplicationDefinition` 中与 `permissions` 相关的状态、依赖、配置或结果（声明类型 `List&lt;String&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `permissions` stores the `permissions`-related state, dependency, configuration, or result of `ApplicationDefinition` (declared type `List&lt;String&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `permissions` 时应保持 `ApplicationDefinition` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `permissions`, preserve `ApplicationDefinition`'s lifecycle, immutability, and thread-safety constraints.
             */
            List<String> permissions
    ) {
        /**
         * 复制权限集合以保持拓扑不可变。
         *
         * <p>Copies permissions to keep the topology immutable.</p>
         * 用法：通过 `ApplicationDefinition` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `ApplicationDefinition`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         * @param applicationCode 输入参数 `applicationCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param applicationName 输入参数 `applicationName`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param roleCode 输入参数 `roleCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param displayPriority 输入参数 `displayPriority`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param permissions 输入参数 `permissions`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public ApplicationDefinition {
            permissions = List.copyOf(permissions);
        }
    }
}
