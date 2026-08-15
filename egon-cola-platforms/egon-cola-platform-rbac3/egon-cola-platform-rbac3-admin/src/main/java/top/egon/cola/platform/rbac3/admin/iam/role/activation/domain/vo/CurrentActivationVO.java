package top.egon.cola.platform.rbac3.admin.iam.role.activation.domain.vo;

import java.util.List;

/**
     * 类型 `CurrentActivationVO` 位于 `ActiveRoleSetRevalidator` 内，是记录类型，用于承载 `Current Activation` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `CurrentActivationVO` is a record inside `ActiveRoleSetRevalidator` and carries the responsibility, state, or contract for `Current Activation`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `CurrentActivationVO` 作为 `ActiveRoleSetRevalidator` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `CurrentActivationVO` as the responsibility boundary of `ActiveRoleSetRevalidator`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param rootRoleIds 记录组件 `rootRoleIds` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `rootRoleIds` carries constructor data whose meaning is defined by the record contract.
 * @param authVersion 记录组件 `authVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `authVersion` carries constructor data whose meaning is defined by the record contract.
     */
    public record CurrentActivationVO(/**
 * 字段 `rootRoleIds` 表示 `CurrentActivationVO` 中与 `root Role Ids` 相关的状态、依赖、配置或结果（声明类型 `List&lt;String&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `rootRoleIds` stores the `root Role Ids`-related state, dependency, configuration, or result of `CurrentActivationVO` (declared type `List&lt;String&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `rootRoleIds` 时应保持 `CurrentActivationVO` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `rootRoleIds`, preserve `CurrentActivationVO`'s lifecycle, immutability, and thread-safety constraints.
 */ List<String> rootRoleIds, /**
 * 字段 `authVersion` 表示当前用户授权版本；Field `authVersion` is the current user authorization version.
 *
 * 含义与用法：读取、传递或更新 `authVersion` 时应保持 `CurrentActivationVO` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: preserve the lifecycle, immutability, and thread-safety constraints of `CurrentActivationVO` when reading or passing `authVersion`.
 */long authVersion) {
        /**
         * 构造器 `CurrentActivationVO` 用于创建并初始化 `CurrentActivationVO` 实例，建立该类型后续方法所依赖的状态和不变量。
         * Constructor `CurrentActivationVO` creates and initializes `CurrentActivationVO`, establishing the state and invariants required by subsequent operations.
         *
         * 用法：通过 `CurrentActivationVO` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `CurrentActivationVO`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         *
         * @param rootRoleIds 输入参数 `rootRoleIds`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param authVersion 输入参数 `authVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public CurrentActivationVO {
            rootRoleIds = List.copyOf(rootRoleIds);
        }
    }
