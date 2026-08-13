package top.egon.cola.platform.rbac3.admin.runtime.domain.vo;



/**
     * 类型 `RuntimeProjectionVO` 位于 `RuntimeProjectionPort` 内，是记录类型，用于承载 `Runtime Projection` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `RuntimeProjectionVO` is a record inside `RuntimeProjectionPort` and carries the responsibility, state, or contract for `Runtime Projection`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `RuntimeProjectionVO` 作为 `RuntimeProjectionPort` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `RuntimeProjectionVO` as the responsibility boundary of `RuntimeProjectionPort`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param tenantId 记录组件 `tenantId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantId` carries constructor data whose meaning is defined by the record contract.
     * @param scopeType 记录组件 `scopeType` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `scopeType` carries constructor data whose meaning is defined by the record contract.
     * @param scopeId 记录组件 `scopeId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `scopeId` carries constructor data whose meaning is defined by the record contract.
     * @param version 记录组件 `version` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `version` carries constructor data whose meaning is defined by the record contract.
     * @param checksum 记录组件 `checksum` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `checksum` carries constructor data whose meaning is defined by the record contract.
     * @param payload 记录组件 `payload` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `payload` carries constructor data whose meaning is defined by the record contract.
     */
    public record RuntimeProjectionVO(
            /**
             * 字段 `tenantId` 表示 `RuntimeProjectionVO` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `RuntimeProjectionVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `RuntimeProjectionVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `RuntimeProjectionVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String tenantId,
            /**
             * 字段 `scopeType` 表示 `RuntimeProjectionVO` 中与 `scope Type` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `scopeType` stores the `scope Type`-related state, dependency, configuration, or result of `RuntimeProjectionVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `scopeType` 时应保持 `RuntimeProjectionVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `scopeType`, preserve `RuntimeProjectionVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String scopeType,
            /**
             * 字段 `scopeId` 表示 `RuntimeProjectionVO` 中与 `scope Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `scopeId` stores the `scope Id`-related state, dependency, configuration, or result of `RuntimeProjectionVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `scopeId` 时应保持 `RuntimeProjectionVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `scopeId`, preserve `RuntimeProjectionVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String scopeId,
            /**
             * 字段 `version` 表示 `RuntimeProjectionVO` 中与 `version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `version` stores the `version`-related state, dependency, configuration, or result of `RuntimeProjectionVO` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `version` 时应保持 `RuntimeProjectionVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `version`, preserve `RuntimeProjectionVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            long version,
            /**
             * 字段 `checksum` 表示 `RuntimeProjectionVO` 中与 `checksum` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `checksum` stores the `checksum`-related state, dependency, configuration, or result of `RuntimeProjectionVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `checksum` 时应保持 `RuntimeProjectionVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `checksum`, preserve `RuntimeProjectionVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String checksum,
            /**
             * 字段 `payload` 表示 `RuntimeProjectionVO` 中与 `payload` 相关的状态、依赖、配置或结果（声明类型 `byte[]`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `payload` stores the `payload`-related state, dependency, configuration, or result of `RuntimeProjectionVO` (declared type `byte[]`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `payload` 时应保持 `RuntimeProjectionVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `payload`, preserve `RuntimeProjectionVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            byte[] payload
    ) {
        /**
         * 构造器 `RuntimeProjectionVO` 用于创建并初始化 `RuntimeProjectionVO` 实例，建立该类型后续方法所依赖的状态和不变量。
         * Constructor `RuntimeProjectionVO` creates and initializes `RuntimeProjectionVO`, establishing the state and invariants required by subsequent operations.
         *
         * 用法：通过 `RuntimeProjectionVO` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `RuntimeProjectionVO`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param scopeType 输入参数 `scopeType`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param scopeId 输入参数 `scopeId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param version 输入参数 `version`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param checksum 输入参数 `checksum`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param payload 输入参数 `payload`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public RuntimeProjectionVO {
            payload = payload.clone();
        }
    }
