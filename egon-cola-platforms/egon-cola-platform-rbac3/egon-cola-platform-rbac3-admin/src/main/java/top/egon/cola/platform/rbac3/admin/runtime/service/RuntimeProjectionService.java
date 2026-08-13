package top.egon.cola.platform.rbac3.admin.runtime.service;

import top.egon.cola.platform.rbac3.admin.runtime.domain.enums.Rbac3RuntimeProjectionDeliveryHandlerProjectionOutcomeEnum;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.EventEnvelopeVO;

/**
     * 类型 `ProjectionSink` 位于 `Rbac3RuntimeProjectionDeliveryHandler` 内，是接口，用于承载 `Projection Sink` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `ProjectionSink` is an interface inside `Rbac3RuntimeProjectionDeliveryHandler` and carries the responsibility, state, or contract for `Projection Sink`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `ProjectionSink` 作为 `Rbac3RuntimeProjectionDeliveryHandler` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `ProjectionSink` as the responsibility boundary of `Rbac3RuntimeProjectionDeliveryHandler`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    @FunctionalInterface
    public interface RuntimeProjectionService {

        /**
         * 方法 `project` 按照 `ProjectionSink` 的职责处理输入，完成 `project` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `project` processes its inputs according to `ProjectionSink`'s responsibility, performs the `project` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `project` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `project`, then continue the business flow using its result, exception, or side effect.
         *
         * @param envelope 输入参数 `envelope`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        Rbac3RuntimeProjectionDeliveryHandlerProjectionOutcomeEnum project(EventEnvelopeVO envelope);
    }
