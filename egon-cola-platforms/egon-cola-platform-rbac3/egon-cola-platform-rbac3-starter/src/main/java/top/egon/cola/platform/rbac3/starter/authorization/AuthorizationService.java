package top.egon.cola.platform.rbac3.starter.authorization;

import top.egon.cola.platform.idp.contract.IdentityPrincipal;
import top.egon.cola.platform.rbac3.contract.authorization.AuthorizationDecision;
import top.egon.cola.platform.rbac3.contract.authorization.AuthorizationFenceDecision;
import top.egon.cola.platform.rbac3.contract.authorization.DataScopeDecision;
import top.egon.cola.platform.rbac3.contract.authorization.FieldPolicyDecision;
import top.egon.cola.platform.rbac3.contract.authorization.OperationSodDecision;
import top.egon.cola.platform.rbac3.contract.authorization.PermissionRequest;
import top.egon.cola.platform.rbac3.contract.authorization.SystemAuthorizationSnapshot;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * 类型 `AuthorizationService` 位于当前包内，是接口，用于承载 `Authorization Service` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `AuthorizationService` is an interface in its package and carries the responsibility, state, or contract for `Authorization Service`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 * Final policy-enforcement API exposed to business applications.
 */
public interface AuthorizationService {

    /**
     * 方法 `requirePermission` 按照 `AuthorizationService` 的职责处理输入，完成 `require Permission` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `requirePermission` processes its inputs according to `AuthorizationService`'s responsibility, performs the `require Permission` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `requirePermission` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `requirePermission`, then continue the business flow using its result, exception, or side effect.
     *
     * @param request 输入参数 `request`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    AuthorizationDecision requirePermission(PermissionRequest request);

    /**
     * 方法 `decideDataScope` 按照 `AuthorizationService` 的职责处理输入，完成 `decide Data Scope` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `decideDataScope` processes its inputs according to `AuthorizationService`'s responsibility, performs the `decide Data Scope` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `decideDataScope` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `decideDataScope`, then continue the business flow using its result, exception, or side effect.
     *
     * @param request 输入参数 `request`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    DataScopeDecision decideDataScope(DataScopeRequest request);

    /**
     * 方法 `decideFields` 按照 `AuthorizationService` 的职责处理输入，完成 `decide Fields` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `decideFields` processes its inputs according to `AuthorizationService`'s responsibility, performs the `decide Fields` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `decideFields` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `decideFields`, then continue the business flow using its result, exception, or side effect.
     *
     * @param request 输入参数 `request`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    FieldPolicyDecision decideFields(FieldPolicyRequest request);

    /**
     * 方法 `checkParticipation` 按照 `AuthorizationService` 的职责处理输入，完成 `check Participation` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `checkParticipation` processes its inputs according to `AuthorizationService`'s responsibility, performs the `check Participation` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `checkParticipation` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `checkParticipation`, then continue the business flow using its result, exception, or side effect.
     *
     * @param request 输入参数 `request`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    OperationSodDecision checkParticipation(OperationSodRequest request);

    /**
     * 方法 `verifyFence` 按照 `AuthorizationService` 的职责处理输入，完成 `verify Fence` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `verifyFence` processes its inputs according to `AuthorizationService`'s responsibility, performs the `verify Fence` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `verifyFence` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `verifyFence`, then continue the business flow using its result, exception, or side effect.
     *
     * @param request 输入参数 `request`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    AuthorizationFenceDecision verifyFence(AuthorizationFenceRequest request);

    /**
     * 类型 `DataScopeRequest` 位于 `AuthorizationService` 内，是记录类型，用于承载 `Data Scope Request` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `DataScopeRequest` is a record inside `AuthorizationService` and carries the responsibility, state, or contract for `Data Scope Request`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `DataScopeRequest` 作为 `AuthorizationService` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `DataScopeRequest` as the responsibility boundary of `AuthorizationService`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param permissionCode 记录组件 `permissionCode` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `permissionCode` carries constructor data whose meaning is defined by the record contract.
     */
    record DataScopeRequest(/**
 * 字段 `permissionCode` 表示 `DataScopeRequest` 中与 `permission Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `permissionCode` stores the `permission Code`-related state, dependency, configuration, or result of `DataScopeRequest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `permissionCode` 时应保持 `DataScopeRequest` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `permissionCode`, preserve `DataScopeRequest`'s lifecycle, immutability, and thread-safety constraints.
 */ String permissionCode) {
        /**
         * 构造器 `DataScopeRequest` 用于创建并初始化 `DataScopeRequest` 实例，建立该类型后续方法所依赖的状态和不变量。
         * Constructor `DataScopeRequest` creates and initializes `DataScopeRequest`, establishing the state and invariants required by subsequent operations.
         *
         * 用法：通过 `DataScopeRequest` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `DataScopeRequest`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         *
         * @param permissionCode 输入参数 `permissionCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public DataScopeRequest {
            permissionCode = required(permissionCode, "permissionCode");
        }
    }

    /**
     * 类型 `FieldPolicyRequest` 位于 `AuthorizationService` 内，是记录类型，用于承载 `Field Policy Request` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `FieldPolicyRequest` is a record inside `AuthorizationService` and carries the responsibility, state, or contract for `Field Policy Request`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `FieldPolicyRequest` 作为 `AuthorizationService` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `FieldPolicyRequest` as the responsibility boundary of `AuthorizationService`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param permissionCode 记录组件 `permissionCode` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `permissionCode` carries constructor data whose meaning is defined by the record contract.
     * @param applicationCode 记录组件 `applicationCode` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `applicationCode` carries constructor data whose meaning is defined by the record contract.
     * @param resourceCode 记录组件 `resourceCode` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `resourceCode` carries constructor data whose meaning is defined by the record contract.
     */
    record FieldPolicyRequest(
            /**
             * 字段 `permissionCode` 表示 `FieldPolicyRequest` 中与 `permission Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `permissionCode` stores the `permission Code`-related state, dependency, configuration, or result of `FieldPolicyRequest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `permissionCode` 时应保持 `FieldPolicyRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `permissionCode`, preserve `FieldPolicyRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            String permissionCode,
            /**
             * 字段 `applicationCode` 表示 `FieldPolicyRequest` 中与 `application Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `applicationCode` stores the `application Code`-related state, dependency, configuration, or result of `FieldPolicyRequest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `applicationCode` 时应保持 `FieldPolicyRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `applicationCode`, preserve `FieldPolicyRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            String applicationCode,
            /**
             * 字段 `resourceCode` 表示 `FieldPolicyRequest` 中与 `resource Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `resourceCode` stores the `resource Code`-related state, dependency, configuration, or result of `FieldPolicyRequest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `resourceCode` 时应保持 `FieldPolicyRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `resourceCode`, preserve `FieldPolicyRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            String resourceCode
    ) {
        /**
         * 构造器 `FieldPolicyRequest` 用于创建并初始化 `FieldPolicyRequest` 实例，建立该类型后续方法所依赖的状态和不变量。
         * Constructor `FieldPolicyRequest` creates and initializes `FieldPolicyRequest`, establishing the state and invariants required by subsequent operations.
         *
         * 用法：通过 `FieldPolicyRequest` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `FieldPolicyRequest`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         *
         * @param permissionCode 输入参数 `permissionCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param applicationCode 输入参数 `applicationCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param resourceCode 输入参数 `resourceCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public FieldPolicyRequest {
            permissionCode = required(permissionCode, "permissionCode");
            applicationCode = required(applicationCode, "applicationCode");
            resourceCode = required(resourceCode, "resourceCode");
        }
    }

    /**
     * 类型 `OperationSodRequest` 位于 `AuthorizationService` 内，是记录类型，用于承载 `Operation Sod Request` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `OperationSodRequest` is a record inside `AuthorizationService` and carries the responsibility, state, or contract for `Operation Sod Request`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `OperationSodRequest` 作为 `AuthorizationService` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `OperationSodRequest` as the responsibility boundary of `AuthorizationService`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param permissionCode 记录组件 `permissionCode` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `permissionCode` carries constructor data whose meaning is defined by the record contract.
     * @param applicationCode 记录组件 `applicationCode` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `applicationCode` carries constructor data whose meaning is defined by the record contract.
     * @param businessResource 记录组件 `businessResource` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `businessResource` carries constructor data whose meaning is defined by the record contract.
     * @param businessId 记录组件 `businessId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `businessId` carries constructor data whose meaning is defined by the record contract.
     * @param actionCode 记录组件 `actionCode` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `actionCode` carries constructor data whose meaning is defined by the record contract.
     */
    record OperationSodRequest(
            /**
             * 字段 `permissionCode` 表示 `OperationSodRequest` 中与 `permission Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `permissionCode` stores the `permission Code`-related state, dependency, configuration, or result of `OperationSodRequest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `permissionCode` 时应保持 `OperationSodRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `permissionCode`, preserve `OperationSodRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            String permissionCode,
            /**
             * 字段 `applicationCode` 表示 `OperationSodRequest` 中与 `application Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `applicationCode` stores the `application Code`-related state, dependency, configuration, or result of `OperationSodRequest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `applicationCode` 时应保持 `OperationSodRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `applicationCode`, preserve `OperationSodRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            String applicationCode,
            /**
             * 字段 `businessResource` 表示 `OperationSodRequest` 中与 `business Resource` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `businessResource` stores the `business Resource`-related state, dependency, configuration, or result of `OperationSodRequest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `businessResource` 时应保持 `OperationSodRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `businessResource`, preserve `OperationSodRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            String businessResource,
            /**
             * 字段 `businessId` 表示 `OperationSodRequest` 中与 `business Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `businessId` stores the `business Id`-related state, dependency, configuration, or result of `OperationSodRequest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `businessId` 时应保持 `OperationSodRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `businessId`, preserve `OperationSodRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            String businessId,
            /**
             * 字段 `actionCode` 表示 `OperationSodRequest` 中与 `action Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `actionCode` stores the `action Code`-related state, dependency, configuration, or result of `OperationSodRequest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `actionCode` 时应保持 `OperationSodRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `actionCode`, preserve `OperationSodRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            String actionCode
    ) {
        /**
         * 构造器 `OperationSodRequest` 用于创建并初始化 `OperationSodRequest` 实例，建立该类型后续方法所依赖的状态和不变量。
         * Constructor `OperationSodRequest` creates and initializes `OperationSodRequest`, establishing the state and invariants required by subsequent operations.
         *
         * 用法：通过 `OperationSodRequest` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `OperationSodRequest`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         *
         * @param permissionCode 输入参数 `permissionCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param applicationCode 输入参数 `applicationCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param businessResource 输入参数 `businessResource`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param businessId 输入参数 `businessId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param actionCode 输入参数 `actionCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public OperationSodRequest {
            permissionCode = required(permissionCode, "permissionCode");
            applicationCode = required(applicationCode, "applicationCode");
            businessResource = required(businessResource, "businessResource");
            businessId = required(businessId, "businessId");
            actionCode = required(actionCode, "actionCode");
        }
    }

    /**
     * 类型 `AuthorizationFenceRequest` 位于 `AuthorizationService` 内，是记录类型，用于承载 `Authorization Fence Request` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `AuthorizationFenceRequest` is a record inside `AuthorizationService` and carries the responsibility, state, or contract for `Authorization Fence Request`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `AuthorizationFenceRequest` 作为 `AuthorizationService` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `AuthorizationFenceRequest` as the responsibility boundary of `AuthorizationService`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param permissionCode 记录组件 `permissionCode` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `permissionCode` carries constructor data whose meaning is defined by the record contract.
     * @param businessResource 记录组件 `businessResource` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `businessResource` carries constructor data whose meaning is defined by the record contract.
     * @param businessId 记录组件 `businessId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `businessId` carries constructor data whose meaning is defined by the record contract.
     * @param traceId 记录组件 `traceId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `traceId` carries constructor data whose meaning is defined by the record contract.
     */
    record AuthorizationFenceRequest(
            /**
             * 字段 `permissionCode` 表示 `AuthorizationFenceRequest` 中与 `permission Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `permissionCode` stores the `permission Code`-related state, dependency, configuration, or result of `AuthorizationFenceRequest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `permissionCode` 时应保持 `AuthorizationFenceRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `permissionCode`, preserve `AuthorizationFenceRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            String permissionCode,
            /**
             * 字段 `businessResource` 表示 `AuthorizationFenceRequest` 中与 `business Resource` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `businessResource` stores the `business Resource`-related state, dependency, configuration, or result of `AuthorizationFenceRequest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `businessResource` 时应保持 `AuthorizationFenceRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `businessResource`, preserve `AuthorizationFenceRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            String businessResource,
            /**
             * 字段 `businessId` 表示 `AuthorizationFenceRequest` 中与 `business Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `businessId` stores the `business Id`-related state, dependency, configuration, or result of `AuthorizationFenceRequest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `businessId` 时应保持 `AuthorizationFenceRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `businessId`, preserve `AuthorizationFenceRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            String businessId,
            /**
             * 字段 `traceId` 表示 `AuthorizationFenceRequest` 中与 `trace Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `traceId` stores the `trace Id`-related state, dependency, configuration, or result of `AuthorizationFenceRequest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `traceId` 时应保持 `AuthorizationFenceRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `traceId`, preserve `AuthorizationFenceRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            String traceId
    ) {
        /**
         * 构造器 `AuthorizationFenceRequest` 用于创建并初始化 `AuthorizationFenceRequest` 实例，建立该类型后续方法所依赖的状态和不变量。
         * Constructor `AuthorizationFenceRequest` creates and initializes `AuthorizationFenceRequest`, establishing the state and invariants required by subsequent operations.
         *
         * 用法：通过 `AuthorizationFenceRequest` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `AuthorizationFenceRequest`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         *
         * @param permissionCode 输入参数 `permissionCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param businessResource 输入参数 `businessResource`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param businessId 输入参数 `businessId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param traceId 输入参数 `traceId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public AuthorizationFenceRequest {
            permissionCode = required(permissionCode, "permissionCode");
            businessResource = required(businessResource, "businessResource");
            businessId = required(businessId, "businessId");
            traceId = required(traceId, "traceId");
        }
    }

    /**
     * 类型 `RuntimeAuthorizationContext` 位于 `AuthorizationService` 内，是记录类型，用于承载 `Runtime Authorization Context` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `RuntimeAuthorizationContext` is a record inside `AuthorizationService` and carries the responsibility, state, or contract for `Runtime Authorization Context`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `RuntimeAuthorizationContext` 作为 `AuthorizationService` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `RuntimeAuthorizationContext` as the responsibility boundary of `AuthorizationService`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param identity 记录组件 `identity` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `identity` carries constructor data whose meaning is defined by the record contract.
     * @param snapshot 记录组件 `snapshot` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `snapshot` carries constructor data whose meaning is defined by the record contract.
     * @param fenced 记录组件 `fenced` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `fenced` carries constructor data whose meaning is defined by the record contract.
     */
    record RuntimeAuthorizationContext(
            /**
             * 字段 `identity` 表示 `RuntimeAuthorizationContext` 中与 `identity` 相关的状态、依赖、配置或结果（声明类型 `IdentityPrincipal`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `identity` stores the `identity`-related state, dependency, configuration, or result of `RuntimeAuthorizationContext` (declared type `IdentityPrincipal`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `identity` 时应保持 `RuntimeAuthorizationContext` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `identity`, preserve `RuntimeAuthorizationContext`'s lifecycle, immutability, and thread-safety constraints.
             */
            IdentityPrincipal identity,
            /**
             * 字段 `snapshot` 表示 `RuntimeAuthorizationContext` 中与 `snapshot` 相关的状态、依赖、配置或结果（声明类型 `SystemAuthorizationSnapshot`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `snapshot` stores the `snapshot`-related state, dependency, configuration, or result of `RuntimeAuthorizationContext` (declared type `SystemAuthorizationSnapshot`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `snapshot` 时应保持 `RuntimeAuthorizationContext` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `snapshot`, preserve `RuntimeAuthorizationContext`'s lifecycle, immutability, and thread-safety constraints.
             */
            SystemAuthorizationSnapshot snapshot,
            /**
             * 字段 `fenced` 表示 `RuntimeAuthorizationContext` 中与 `fenced` 相关的状态、依赖、配置或结果（声明类型 `boolean`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `fenced` stores the `fenced`-related state, dependency, configuration, or result of `RuntimeAuthorizationContext` (declared type `boolean`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `fenced` 时应保持 `RuntimeAuthorizationContext` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `fenced`, preserve `RuntimeAuthorizationContext`'s lifecycle, immutability, and thread-safety constraints.
             */
            boolean fenced
    ) {
        /**
         * 构造器 `RuntimeAuthorizationContext` 用于创建并初始化 `RuntimeAuthorizationContext` 实例，建立该类型后续方法所依赖的状态和不变量。
         * Constructor `RuntimeAuthorizationContext` creates and initializes `RuntimeAuthorizationContext`, establishing the state and invariants required by subsequent operations.
         *
         * 用法：通过 `RuntimeAuthorizationContext` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `RuntimeAuthorizationContext`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         *
         * @param identity 输入参数 `identity`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param snapshot 输入参数 `snapshot`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param fenced 输入参数 `fenced`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public RuntimeAuthorizationContext {
            identity = Objects.requireNonNull(identity, "identity");
            snapshot = Objects.requireNonNull(snapshot, "snapshot");
            if (!identity.subject().equals(snapshot.identitySub())
                    || !identity.tenantId().equals(snapshot.tenantId())
                    || !identity.sessionId().equals(snapshot.sessionId())) {
                throw new RuntimeUnavailableException(
                        "AUTHORIZATION_IDENTITY_MISMATCH", identity);
            }
        }
    }

    /**
     * 类型 `RuntimeContextSource` 位于 `AuthorizationService` 内，是接口，用于承载 `Runtime Context Source` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `RuntimeContextSource` is an interface inside `AuthorizationService` and carries the responsibility, state, or contract for `Runtime Context Source`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `RuntimeContextSource` 作为 `AuthorizationService` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `RuntimeContextSource` as the responsibility boundary of `AuthorizationService`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    @FunctionalInterface
    interface RuntimeContextSource {
        /**
         * 方法 `load` 按照 `RuntimeContextSource` 的职责处理输入，完成 `load` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `load` processes its inputs according to `RuntimeContextSource`'s responsibility, performs the `load` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `load` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `load`, then continue the business flow using its result, exception, or side effect.
         *
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        RuntimeAuthorizationContext load();
    }

    /**
     * 类型 `OperationSodEvaluator` 位于 `AuthorizationService` 内，是接口，用于承载 `Operation Sod Evaluator` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `OperationSodEvaluator` is an interface inside `AuthorizationService` and carries the responsibility, state, or contract for `Operation Sod Evaluator`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `OperationSodEvaluator` 作为 `AuthorizationService` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `OperationSodEvaluator` as the responsibility boundary of `AuthorizationService`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    @FunctionalInterface
    interface OperationSodEvaluator {
        /**
         * 方法 `evaluate` 按照 `OperationSodEvaluator` 的职责处理输入，完成 `evaluate` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `evaluate` processes its inputs according to `OperationSodEvaluator`'s responsibility, performs the `evaluate` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `evaluate` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `evaluate`, then continue the business flow using its result, exception, or side effect.
         *
         * @param request 输入参数 `request`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        OperationSodResult evaluate(OperationSodRequest request);
    }

    /**
     * 类型 `FenceVerifier` 位于 `AuthorizationService` 内，是接口，用于承载 `Fence Verifier` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `FenceVerifier` is an interface inside `AuthorizationService` and carries the responsibility, state, or contract for `Fence Verifier`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `FenceVerifier` 作为 `AuthorizationService` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `FenceVerifier` as the responsibility boundary of `AuthorizationService`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    @FunctionalInterface
    interface FenceVerifier {
        /**
         * 方法 `verify` 按照 `FenceVerifier` 的职责处理输入，完成 `verify` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `verify` processes its inputs according to `FenceVerifier`'s responsibility, performs the `verify` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `verify` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `verify`, then continue the business flow using its result, exception, or side effect.
         *
         * @param request 输入参数 `request`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        FenceResult verify(AuthorizationFenceRequest request);
    }

    /**
     * 类型 `OperationSodResult` 位于 `AuthorizationService` 内，是记录类型，用于承载 `Operation Sod Result` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `OperationSodResult` is a record inside `AuthorizationService` and carries the responsibility, state, or contract for `Operation Sod Result`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `OperationSodResult` 作为 `AuthorizationService` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `OperationSodResult` as the responsibility boundary of `AuthorizationService`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param permitted 记录组件 `permitted` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `permitted` carries constructor data whose meaning is defined by the record contract.
     * @param reasonCode 记录组件 `reasonCode` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `reasonCode` carries constructor data whose meaning is defined by the record contract.
     * @param conflictingActionCodes 记录组件 `conflictingActionCodes` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `conflictingActionCodes` carries constructor data whose meaning is defined by the record contract.
     * @param evidenceIds 记录组件 `evidenceIds` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `evidenceIds` carries constructor data whose meaning is defined by the record contract.
     */
    record OperationSodResult(
            /**
             * 字段 `permitted` 表示 `OperationSodResult` 中与 `permitted` 相关的状态、依赖、配置或结果（声明类型 `boolean`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `permitted` stores the `permitted`-related state, dependency, configuration, or result of `OperationSodResult` (declared type `boolean`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `permitted` 时应保持 `OperationSodResult` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `permitted`, preserve `OperationSodResult`'s lifecycle, immutability, and thread-safety constraints.
             */
            boolean permitted,
            /**
             * 字段 `reasonCode` 表示 `OperationSodResult` 中与 `reason Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `reasonCode` stores the `reason Code`-related state, dependency, configuration, or result of `OperationSodResult` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `reasonCode` 时应保持 `OperationSodResult` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `reasonCode`, preserve `OperationSodResult`'s lifecycle, immutability, and thread-safety constraints.
             */
            String reasonCode,
            /**
             * 字段 `conflictingActionCodes` 表示 `OperationSodResult` 中与 `conflicting Action Codes` 相关的状态、依赖、配置或结果（声明类型 `List&lt;String&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `conflictingActionCodes` stores the `conflicting Action Codes`-related state, dependency, configuration, or result of `OperationSodResult` (declared type `List&lt;String&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `conflictingActionCodes` 时应保持 `OperationSodResult` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `conflictingActionCodes`, preserve `OperationSodResult`'s lifecycle, immutability, and thread-safety constraints.
             */
            List<String> conflictingActionCodes,
            /**
             * 字段 `evidenceIds` 表示 `OperationSodResult` 中与 `evidence Ids` 相关的状态、依赖、配置或结果（声明类型 `List&lt;String&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `evidenceIds` stores the `evidence Ids`-related state, dependency, configuration, or result of `OperationSodResult` (declared type `List&lt;String&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `evidenceIds` 时应保持 `OperationSodResult` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `evidenceIds`, preserve `OperationSodResult`'s lifecycle, immutability, and thread-safety constraints.
             */
            List<String> evidenceIds
    ) {
        /**
         * 构造器 `OperationSodResult` 用于创建并初始化 `OperationSodResult` 实例，建立该类型后续方法所依赖的状态和不变量。
         * Constructor `OperationSodResult` creates and initializes `OperationSodResult`, establishing the state and invariants required by subsequent operations.
         *
         * 用法：通过 `OperationSodResult` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `OperationSodResult`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         *
         * @param permitted 输入参数 `permitted`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param reasonCode 输入参数 `reasonCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param conflictingActionCodes 输入参数 `conflictingActionCodes`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param evidenceIds 输入参数 `evidenceIds`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public OperationSodResult {
            reasonCode = required(reasonCode, "reasonCode");
            conflictingActionCodes = List.copyOf(conflictingActionCodes);
            evidenceIds = List.copyOf(evidenceIds);
        }

        /**
         * 方法 `allowed` 按照 `OperationSodResult` 的职责处理输入，完成 `allowed` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `allowed` processes its inputs according to `OperationSodResult`'s responsibility, performs the `allowed` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `allowed` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `allowed`, then continue the business flow using its result, exception, or side effect.
         *
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        public static OperationSodResult allowed() {
            return new OperationSodResult(true, "ALLOW", List.of(), List.of());
        }
    }

    /**
     * 类型 `FenceResult` 位于 `AuthorizationService` 内，是记录类型，用于承载 `Fence Result` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `FenceResult` is a record inside `AuthorizationService` and carries the responsibility, state, or contract for `Fence Result`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `FenceResult` 作为 `AuthorizationService` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `FenceResult` as the responsibility boundary of `AuthorizationService`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param permitted 记录组件 `permitted` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `permitted` carries constructor data whose meaning is defined by the record contract.
     * @param reasonCode 记录组件 `reasonCode` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `reasonCode` carries constructor data whose meaning is defined by the record contract.
     * @param verifiedAt 记录组件 `verifiedAt` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `verifiedAt` carries constructor data whose meaning is defined by the record contract.
     * @param evidenceIds 记录组件 `evidenceIds` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `evidenceIds` carries constructor data whose meaning is defined by the record contract.
     */
    record FenceResult(
            /**
             * 字段 `permitted` 表示 `FenceResult` 中与 `permitted` 相关的状态、依赖、配置或结果（声明类型 `boolean`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `permitted` stores the `permitted`-related state, dependency, configuration, or result of `FenceResult` (declared type `boolean`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `permitted` 时应保持 `FenceResult` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `permitted`, preserve `FenceResult`'s lifecycle, immutability, and thread-safety constraints.
             */
            boolean permitted,
            /**
             * 字段 `reasonCode` 表示 `FenceResult` 中与 `reason Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `reasonCode` stores the `reason Code`-related state, dependency, configuration, or result of `FenceResult` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `reasonCode` 时应保持 `FenceResult` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `reasonCode`, preserve `FenceResult`'s lifecycle, immutability, and thread-safety constraints.
             */
            String reasonCode,
            /**
             * 字段 `verifiedAt` 表示 `FenceResult` 中与 `verified At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `verifiedAt` stores the `verified At`-related state, dependency, configuration, or result of `FenceResult` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `verifiedAt` 时应保持 `FenceResult` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `verifiedAt`, preserve `FenceResult`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant verifiedAt,
            /**
             * 字段 `evidenceIds` 表示 `FenceResult` 中与 `evidence Ids` 相关的状态、依赖、配置或结果（声明类型 `List&lt;String&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `evidenceIds` stores the `evidence Ids`-related state, dependency, configuration, or result of `FenceResult` (declared type `List&lt;String&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `evidenceIds` 时应保持 `FenceResult` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `evidenceIds`, preserve `FenceResult`'s lifecycle, immutability, and thread-safety constraints.
             */
            List<String> evidenceIds
    ) {
        /**
         * 构造器 `FenceResult` 用于创建并初始化 `FenceResult` 实例，建立该类型后续方法所依赖的状态和不变量。
         * Constructor `FenceResult` creates and initializes `FenceResult`, establishing the state and invariants required by subsequent operations.
         *
         * 用法：通过 `FenceResult` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `FenceResult`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         *
         * @param permitted 输入参数 `permitted`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param reasonCode 输入参数 `reasonCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param verifiedAt 输入参数 `verifiedAt`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param evidenceIds 输入参数 `evidenceIds`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public FenceResult {
            reasonCode = required(reasonCode, "reasonCode");
            verifiedAt = Objects.requireNonNull(verifiedAt, "verifiedAt");
            evidenceIds = List.copyOf(evidenceIds);
        }

        /**
         * 方法 `allowed` 按照 `FenceResult` 的职责处理输入，完成 `allowed` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `allowed` processes its inputs according to `FenceResult`'s responsibility, performs the `allowed` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `allowed` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `allowed`, then continue the business flow using its result, exception, or side effect.
         *
         * @param verifiedAt 输入参数 `verifiedAt`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        public static FenceResult allowed(Instant verifiedAt) {
            return new FenceResult(true, "ALLOW", verifiedAt, List.of());
        }
    }

    /**
     * 类型 `RuntimeUnavailableException` 位于 `AuthorizationService` 内，是类型，用于承载 `Runtime Unavailable Exception` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `RuntimeUnavailableException` is a type inside `AuthorizationService` and carries the responsibility, state, or contract for `Runtime Unavailable Exception`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `RuntimeUnavailableException` 作为 `AuthorizationService` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `RuntimeUnavailableException` as the responsibility boundary of `AuthorizationService`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    final class RuntimeUnavailableException extends RuntimeException {

        /**
         * 字段 `reasonCode` 表示 `RuntimeUnavailableException` 中与 `reason Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `reasonCode` stores the `reason Code`-related state, dependency, configuration, or result of `RuntimeUnavailableException` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `reasonCode` 时应保持 `RuntimeUnavailableException` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `reasonCode`, preserve `RuntimeUnavailableException`'s lifecycle, immutability, and thread-safety constraints.
         */
        private final String reasonCode;
        /**
         * 字段 `identity` 表示 `RuntimeUnavailableException` 中与 `identity` 相关的状态、依赖、配置或结果（声明类型 `IdentityPrincipal`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `identity` stores the `identity`-related state, dependency, configuration, or result of `RuntimeUnavailableException` (declared type `IdentityPrincipal`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `identity` 时应保持 `RuntimeUnavailableException` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `identity`, preserve `RuntimeUnavailableException`'s lifecycle, immutability, and thread-safety constraints.
         */
        private final IdentityPrincipal identity;

        /**
         * 构造器 `RuntimeUnavailableException` 用于创建并初始化 `RuntimeUnavailableException` 实例，建立该类型后续方法所依赖的状态和不变量。
         * Constructor `RuntimeUnavailableException` creates and initializes `RuntimeUnavailableException`, establishing the state and invariants required by subsequent operations.
         *
         * 用法：通过 `RuntimeUnavailableException` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `RuntimeUnavailableException`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         *
         * @param reasonCode 输入参数 `reasonCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param identity 输入参数 `identity`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public RuntimeUnavailableException(
                String reasonCode,
                IdentityPrincipal identity
        ) {
            super(required(reasonCode, "reasonCode"));
            this.reasonCode = reasonCode;
            this.identity = Objects.requireNonNull(identity, "identity");
        }

        /**
         * 方法 `reasonCode` 按照 `RuntimeUnavailableException` 的职责处理输入，完成 `reason Code` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `reasonCode` processes its inputs according to `RuntimeUnavailableException`'s responsibility, performs the `reason Code` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `reasonCode` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `reasonCode`, then continue the business flow using its result, exception, or side effect.
         *
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        public String reasonCode() {
            return reasonCode;
        }

        /**
         * 方法 `identity` 按照 `RuntimeUnavailableException` 的职责处理输入，完成 `identity` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `identity` processes its inputs according to `RuntimeUnavailableException`'s responsibility, performs the `identity` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `identity` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `identity`, then continue the business flow using its result, exception, or side effect.
         *
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        public IdentityPrincipal identity() {
            return identity;
        }
    }

    /**
     * 方法 `required` 按照 `AuthorizationService` 的职责处理输入，完成 `required` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `required` processes its inputs according to `AuthorizationService`'s responsibility, performs the `required` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `required` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `required`, then continue the business flow using its result, exception, or side effect.
     *
     * @param value 输入参数 `value`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param fieldName 输入参数 `fieldName`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
