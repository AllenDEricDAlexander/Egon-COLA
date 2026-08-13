package top.egon.cola.platform.rbac3.admin.bootstrap.domain.vo;

import java.util.List;
import top.egon.cola.platform.rbac3.admin.bootstrap.service.Rbac3DevelopmentTopology;

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
     * 语义与用法：将 `ApplicationDefinitionVO` 作为 `Rbac3DevelopmentTopology` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `ApplicationDefinitionVO` as the responsibility boundary of `Rbac3DevelopmentTopology`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public record ApplicationDefinitionVO(
            /**
             * 字段 `applicationCode` 表示 `ApplicationDefinitionVO` 中与 `application Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `applicationCode` stores the `application Code`-related state, dependency, configuration, or result of `ApplicationDefinitionVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `applicationCode` 时应保持 `ApplicationDefinitionVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `applicationCode`, preserve `ApplicationDefinitionVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String applicationCode,
            /**
             * 字段 `applicationName` 表示 `ApplicationDefinitionVO` 中与 `application Name` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `applicationName` stores the `application Name`-related state, dependency, configuration, or result of `ApplicationDefinitionVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `applicationName` 时应保持 `ApplicationDefinitionVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `applicationName`, preserve `ApplicationDefinitionVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String applicationName,
            /**
             * 字段 `roleCode` 表示 `ApplicationDefinitionVO` 中与 `role Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `roleCode` stores the `role Code`-related state, dependency, configuration, or result of `ApplicationDefinitionVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `roleCode` 时应保持 `ApplicationDefinitionVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `roleCode`, preserve `ApplicationDefinitionVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String roleCode,
            /**
             * 字段 `displayPriority` 表示 `ApplicationDefinitionVO` 中与 `display Priority` 相关的状态、依赖、配置或结果（声明类型 `int`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `displayPriority` stores the `display Priority`-related state, dependency, configuration, or result of `ApplicationDefinitionVO` (declared type `int`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `displayPriority` 时应保持 `ApplicationDefinitionVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `displayPriority`, preserve `ApplicationDefinitionVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            int displayPriority,
            /**
             * 字段 `permissions` 表示 `ApplicationDefinitionVO` 中与 `permissions` 相关的状态、依赖、配置或结果（声明类型 `List&lt;String&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `permissions` stores the `permissions`-related state, dependency, configuration, or result of `ApplicationDefinitionVO` (declared type `List&lt;String&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `permissions` 时应保持 `ApplicationDefinitionVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `permissions`, preserve `ApplicationDefinitionVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            List<String> permissions
    ) {
        /**
         * 复制权限集合以保持拓扑不可变。
         *
         * <p>Copies permissions to keep the topology immutable.</p>
         * 用法：通过 `ApplicationDefinitionVO` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `ApplicationDefinitionVO`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         * @param applicationCode 输入参数 `applicationCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param applicationName 输入参数 `applicationName`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param roleCode 输入参数 `roleCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param displayPriority 输入参数 `displayPriority`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param permissions 输入参数 `permissions`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public ApplicationDefinitionVO {
            permissions = List.copyOf(permissions);
        }
    }
