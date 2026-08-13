package top.egon.cola.platform.rbac3.admin.simulation.domain.dto;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import top.egon.cola.platform.rbac3.admin.authorization.domain.dto.DecisionRequestDTO;

/**
     * 类型 `SimulationRequestDTO` 位于 `AuthorizationSimulationService` 内，是记录类型，用于承载 `Simulation Request` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `SimulationRequestDTO` is a record inside `AuthorizationSimulationService` and carries the responsibility, state, or contract for `Simulation Request`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `SimulationRequestDTO` 作为 `AuthorizationSimulationService` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `SimulationRequestDTO` as the responsibility boundary of `AuthorizationSimulationService`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param decisionRequest 记录组件 `decisionRequest` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `decisionRequest` carries constructor data whose meaning is defined by the record contract.
     * @param hypothesis 记录组件 `hypothesis` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `hypothesis` carries constructor data whose meaning is defined by the record contract.
     * @param at 记录组件 `at` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `at` carries constructor data whose meaning is defined by the record contract.
     * @param requestId 记录组件 `requestId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `requestId` carries constructor data whose meaning is defined by the record contract.
     * @param traceId 记录组件 `traceId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `traceId` carries constructor data whose meaning is defined by the record contract.
     */
    public record SimulationRequestDTO(
            /**
             * 字段 `decisionRequest` 表示 `SimulationRequestDTO` 中与 `decision Request` 相关的状态、依赖、配置或结果（声明类型 `DecisionRequestDTO`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `decisionRequest` stores the `decision Request`-related state, dependency, configuration, or result of `SimulationRequestDTO` (declared type `DecisionRequestDTO`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `decisionRequest` 时应保持 `SimulationRequestDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `decisionRequest`, preserve `SimulationRequestDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            DecisionRequestDTO decisionRequest,
            /**
             * 字段 `hypothesis` 表示 `SimulationRequestDTO` 中与 `hypothesis` 相关的状态、依赖、配置或结果（声明类型 `HypothesisDTO`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `hypothesis` stores the `hypothesis`-related state, dependency, configuration, or result of `SimulationRequestDTO` (declared type `HypothesisDTO`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `hypothesis` 时应保持 `SimulationRequestDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `hypothesis`, preserve `SimulationRequestDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            HypothesisDTO hypothesis,
            /**
             * 字段 `at` 表示 `SimulationRequestDTO` 中与 `at` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `at` stores the `at`-related state, dependency, configuration, or result of `SimulationRequestDTO` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `at` 时应保持 `SimulationRequestDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `at`, preserve `SimulationRequestDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant at,
            /**
             * 字段 `requestId` 表示 `SimulationRequestDTO` 中与 `request Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `requestId` stores the `request Id`-related state, dependency, configuration, or result of `SimulationRequestDTO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `requestId` 时应保持 `SimulationRequestDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `requestId`, preserve `SimulationRequestDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String requestId,
            /**
             * 字段 `traceId` 表示 `SimulationRequestDTO` 中与 `trace Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `traceId` stores the `trace Id`-related state, dependency, configuration, or result of `SimulationRequestDTO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `traceId` 时应保持 `SimulationRequestDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `traceId`, preserve `SimulationRequestDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String traceId) {
        /**
         * 构造器 `SimulationRequestDTO` 用于创建并初始化 `SimulationRequestDTO` 实例，建立该类型后续方法所依赖的状态和不变量。
         * Constructor `SimulationRequestDTO` creates and initializes `SimulationRequestDTO`, establishing the state and invariants required by subsequent operations.
         *
         * 用法：通过 `SimulationRequestDTO` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `SimulationRequestDTO`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         *
         * @param decisionRequest 输入参数 `decisionRequest`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param hypothesis 输入参数 `hypothesis`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param at 输入参数 `at`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param requestId 输入参数 `requestId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param traceId 输入参数 `traceId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public SimulationRequestDTO {
            decisionRequest = Objects.requireNonNull(decisionRequest, "decisionRequest");
            hypothesis = Objects.requireNonNull(hypothesis, "hypothesis");
            at = Objects.requireNonNull(at, "at");
            requestId = required(requestId, "requestId");
            traceId = required(traceId, "traceId");
        }

        /** 校验必填文本。 / Validates required text. */
        private static String required(String value, String fieldName) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException(fieldName + " is required");
            }
            return value.trim();
        }
    }
