package top.egon.cola.platform.rbac3.admin.authorization.domain.enums;

/**
     * 类型化授权判定维度。
     * Typed authorization-decision dimension.
     * 语义与用法：将 `AuthorizationDecisionDecisionTypeEnum` 作为 `AuthorizationDecisionService` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `AuthorizationDecisionDecisionTypeEnum` as the responsibility boundary of `AuthorizationDecisionService`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public enum AuthorizationDecisionDecisionTypeEnum {
        /** 函数权限。 / Function permission.
         * 含义与用法：读取、传递或更新 `FUNCTION` 时应保持 `AuthorizationDecisionDecisionTypeEnum` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `FUNCTION`, preserve `AuthorizationDecisionDecisionTypeEnum`'s lifecycle, immutability, and thread-safety constraints.
         */
        FUNCTION,
        /** 数据范围。 / Data scope.
         * 含义与用法：读取、传递或更新 `DATA_SCOPE` 时应保持 `AuthorizationDecisionDecisionTypeEnum` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `DATA_SCOPE`, preserve `AuthorizationDecisionDecisionTypeEnum`'s lifecycle, immutability, and thread-safety constraints.
         */
        DATA_SCOPE,
        /** 字段策略。 / Field policy.
         * 含义与用法：读取、传递或更新 `FIELD` 时应保持 `AuthorizationDecisionDecisionTypeEnum` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `FIELD`, preserve `AuthorizationDecisionDecisionTypeEnum`'s lifecycle, immutability, and thread-safety constraints.
         */
        FIELD,
        /** 参与约束。 / Participation constraint.
         * 含义与用法：读取、传递或更新 `PARTICIPATION` 时应保持 `AuthorizationDecisionDecisionTypeEnum` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `PARTICIPATION`, preserve `AuthorizationDecisionDecisionTypeEnum`'s lifecycle, immutability, and thread-safety constraints.
         */
        PARTICIPATION,
        /** 授权传播 Fence。 / Authorization propagation fence.
         * 含义与用法：读取、传递或更新 `FENCE` 时应保持 `AuthorizationDecisionDecisionTypeEnum` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `FENCE`, preserve `AuthorizationDecisionDecisionTypeEnum`'s lifecycle, immutability, and thread-safety constraints.
         */
        FENCE
    }
