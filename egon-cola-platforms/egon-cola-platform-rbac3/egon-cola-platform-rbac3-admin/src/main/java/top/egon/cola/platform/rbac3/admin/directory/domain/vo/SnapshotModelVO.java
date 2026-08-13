package top.egon.cola.platform.rbac3.admin.directory.domain.vo;

import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import top.egon.cola.platform.rbac3.admin.directory.domain.dto.PositionInputDTO;
import top.egon.cola.platform.rbac3.admin.directory.domain.dto.UserPositionInputDTO;
import top.egon.cola.platform.rbac3.admin.directory.service.DirectorySnapshotProcessor;

/**
     * 类型 `SnapshotModelVO` 位于 `DirectorySnapshotProcessor` 内，是记录类型，用于承载 `Snapshot Model` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `SnapshotModelVO` is a record inside `DirectorySnapshotProcessor` and carries the responsibility, state, or contract for `Snapshot Model`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `SnapshotModelVO` 作为 `DirectorySnapshotProcessor` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `SnapshotModelVO` as the responsibility boundary of `DirectorySnapshotProcessor`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param units 记录组件 `units` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `units` carries constructor data whose meaning is defined by the record contract.
     * @param positions 记录组件 `positions` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `positions` carries constructor data whose meaning is defined by the record contract.
     * @param userPositions 记录组件 `userPositions` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `userPositions` carries constructor data whose meaning is defined by the record contract.
     * @param counts 记录组件 `counts` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `counts` carries constructor data whose meaning is defined by the record contract.
     */
    public record SnapshotModelVO(
            /**
             * 字段 `units` 表示 `SnapshotModelVO` 中与 `units` 相关的状态、依赖、配置或结果（声明类型 `List&lt;ResolvedUnitVO&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `units` stores the `units`-related state, dependency, configuration, or result of `SnapshotModelVO` (declared type `List&lt;ResolvedUnitVO&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `units` 时应保持 `SnapshotModelVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `units`, preserve `SnapshotModelVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            List<ResolvedUnitVO> units,
            /**
             * 字段 `positions` 表示 `SnapshotModelVO` 中与 `positions` 相关的状态、依赖、配置或结果（声明类型 `List&lt;PositionInputDTO&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `positions` stores the `positions`-related state, dependency, configuration, or result of `SnapshotModelVO` (declared type `List&lt;PositionInputDTO&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `positions` 时应保持 `SnapshotModelVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `positions`, preserve `SnapshotModelVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            List<PositionInputDTO> positions,
            /**
             * 字段 `userPositions` 表示 `SnapshotModelVO` 中与 `user Positions` 相关的状态、依赖、配置或结果（声明类型 `List&lt;UserPositionInputDTO&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `userPositions` stores the `user Positions`-related state, dependency, configuration, or result of `SnapshotModelVO` (declared type `List&lt;UserPositionInputDTO&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `userPositions` 时应保持 `SnapshotModelVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `userPositions`, preserve `SnapshotModelVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            List<UserPositionInputDTO> userPositions,
            /**
             * 字段 `counts` 表示 `SnapshotModelVO` 中与 `counts` 相关的状态、依赖、配置或结果（声明类型 `Map&lt;String, Object&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `counts` stores the `counts`-related state, dependency, configuration, or result of `SnapshotModelVO` (declared type `Map&lt;String, Object&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `counts` 时应保持 `SnapshotModelVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `counts`, preserve `SnapshotModelVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            Map<String, Object> counts) {

        /**
         * 构造器 `SnapshotModelVO` 用于创建并初始化 `SnapshotModelVO` 实例，建立该类型后续方法所依赖的状态和不变量。
         * Constructor `SnapshotModelVO` creates and initializes `SnapshotModelVO`, establishing the state and invariants required by subsequent operations.
         *
         * 用法：通过 `SnapshotModelVO` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `SnapshotModelVO`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         *
         * @param units 输入参数 `units`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param positions 输入参数 `positions`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param userPositions 输入参数 `userPositions`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param counts 输入参数 `counts`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public SnapshotModelVO {
            units = List.copyOf(units);
            positions = List.copyOf(positions);
            userPositions = List.copyOf(userPositions);
            counts = Map.copyOf(counts);
        }
    }
