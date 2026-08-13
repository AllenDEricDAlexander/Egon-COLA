package top.egon.cola.platform.rbac3.admin.runtime.domain.vo;

import top.egon.cola.platform.rbac3.admin.runtime.repository.ddc.DdcProviderLeaseStatusRepository;
import top.egon.cola.platform.rbac3.admin.runtime.repository.http.GatewayAdminControlPlaneStatusClient;
import top.egon.cola.platform.rbac3.admin.runtime.repository.http.GatewayDefinitionStatusRepository;
import top.egon.cola.platform.rbac3.admin.runtime.service.ControlPlaneRuntimeStatusPort;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.function.Supplier;
import top.egon.cola.platform.rbac3.admin.runtime.service.GatewayDdcRuntimeStatusService;
import top.egon.cola.platform.rbac3.admin.runtime.domain.GatewayServiceKey;

/**
     * 类型 `ServiceIdentityVO` 位于 `GatewayDdcRuntimeStatusService` 内，是记录类型，用于承载 `Service Identity` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `ServiceIdentityVO` is a record inside `GatewayDdcRuntimeStatusService` and carries the responsibility, state, or contract for `Service Identity`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `ServiceIdentityVO` 作为 `GatewayDdcRuntimeStatusService` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `ServiceIdentityVO` as the responsibility boundary of `GatewayDdcRuntimeStatusService`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param bizCode 记录组件 `bizCode` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `bizCode` carries constructor data whose meaning is defined by the record contract.
     * @param appCode 记录组件 `appCode` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `appCode` carries constructor data whose meaning is defined by the record contract.
     * @param env 记录组件 `env` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `env` carries constructor data whose meaning is defined by the record contract.
     * @param namespace 记录组件 `namespace` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `namespace` carries constructor data whose meaning is defined by the record contract.
     * @param serviceKind 记录组件 `serviceKind` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `serviceKind` carries constructor data whose meaning is defined by the record contract.
     * @param protocol 记录组件 `protocol` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `protocol` carries constructor data whose meaning is defined by the record contract.
     * @param serviceName 记录组件 `serviceName` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `serviceName` carries constructor data whose meaning is defined by the record contract.
     * @param group 记录组件 `group` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `group` carries constructor data whose meaning is defined by the record contract.
     * @param version 记录组件 `version` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `version` carries constructor data whose meaning is defined by the record contract.
     */
    public record ServiceIdentityVO(
            /**
             * 字段 `bizCode` 表示 `ServiceIdentityVO` 中与 `biz Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `bizCode` stores the `biz Code`-related state, dependency, configuration, or result of `ServiceIdentityVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `bizCode` 时应保持 `ServiceIdentityVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `bizCode`, preserve `ServiceIdentityVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String bizCode,
            /**
             * 字段 `appCode` 表示 `ServiceIdentityVO` 中与 `app Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `appCode` stores the `app Code`-related state, dependency, configuration, or result of `ServiceIdentityVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `appCode` 时应保持 `ServiceIdentityVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `appCode`, preserve `ServiceIdentityVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String appCode,
            /**
             * 字段 `env` 表示 `ServiceIdentityVO` 中与 `env` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `env` stores the `env`-related state, dependency, configuration, or result of `ServiceIdentityVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `env` 时应保持 `ServiceIdentityVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `env`, preserve `ServiceIdentityVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String env,
            /**
             * 字段 `namespace` 表示 `ServiceIdentityVO` 中与 `namespace` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `namespace` stores the `namespace`-related state, dependency, configuration, or result of `ServiceIdentityVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `namespace` 时应保持 `ServiceIdentityVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `namespace`, preserve `ServiceIdentityVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String namespace,
            /**
             * 字段 `serviceKind` 表示 `ServiceIdentityVO` 中与 `service Kind` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `serviceKind` stores the `service Kind`-related state, dependency, configuration, or result of `ServiceIdentityVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `serviceKind` 时应保持 `ServiceIdentityVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `serviceKind`, preserve `ServiceIdentityVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String serviceKind,
            /**
             * 字段 `protocol` 表示 `ServiceIdentityVO` 中与 `protocol` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `protocol` stores the `protocol`-related state, dependency, configuration, or result of `ServiceIdentityVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `protocol` 时应保持 `ServiceIdentityVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `protocol`, preserve `ServiceIdentityVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String protocol,
            /**
             * 字段 `serviceName` 表示 `ServiceIdentityVO` 中与 `service Name` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `serviceName` stores the `service Name`-related state, dependency, configuration, or result of `ServiceIdentityVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `serviceName` 时应保持 `ServiceIdentityVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `serviceName`, preserve `ServiceIdentityVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String serviceName,
            /**
             * 字段 `group` 表示 `ServiceIdentityVO` 中与 `group` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `group` stores the `group`-related state, dependency, configuration, or result of `ServiceIdentityVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `group` 时应保持 `ServiceIdentityVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `group`, preserve `ServiceIdentityVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String group,
            /**
             * 字段 `version` 表示 `ServiceIdentityVO` 中与 `version` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `version` stores the `version`-related state, dependency, configuration, or result of `ServiceIdentityVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `version` 时应保持 `ServiceIdentityVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `version`, preserve `ServiceIdentityVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String version) {

        /**
         * 构造器 `ServiceIdentityVO` 用于创建并初始化 `ServiceIdentityVO` 实例，建立该类型后续方法所依赖的状态和不变量。
         * Constructor `ServiceIdentityVO` creates and initializes `ServiceIdentityVO`, establishing the state and invariants required by subsequent operations.
         *
         * 用法：通过 `ServiceIdentityVO` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `ServiceIdentityVO`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         *
         * @param bizCode 输入参数 `bizCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param appCode 输入参数 `appCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param env 输入参数 `env`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param namespace 输入参数 `namespace`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param serviceKind 输入参数 `serviceKind`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param protocol 输入参数 `protocol`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param serviceName 输入参数 `serviceName`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param group 输入参数 `group`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param version 输入参数 `version`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public ServiceIdentityVO {
            bizCode = required(bizCode, "bizCode");
            appCode = required(appCode, "appCode");
            env = required(env, "env");
            namespace = required(namespace, "namespace");
            serviceKind = required(serviceKind, "serviceKind");
            protocol = required(protocol, "protocol");
            serviceName = required(serviceName, "serviceName");
            group = required(group, "group");
            version = required(version, "version");
        }

        /**
         * 方法 `matches` 按照 `ServiceIdentityVO` 的职责处理输入，完成 `matches` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `matches` processes its inputs according to `ServiceIdentityVO`'s responsibility, performs the `matches` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `matches` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `matches`, then continue the business flow using its result, exception, or side effect.
         *
         * @param key 输入参数 `key`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        public boolean matches(GatewayServiceKey key) {
            return key != null
                    && bizCode.equals(key.bizCode())
                    && appCode.equals(key.appCode())
                    && env.equals(key.env())
                    && namespace.equals(key.namespace())
                    && serviceKind.equals(key.serviceKind())
                    && protocol.equals(key.protocol())
                    && serviceName.equals(key.serviceName())
                    && group.equals(key.group())
                    && version.equals(key.version());
        }

        /**
         * 方法 `required` 按照 `ServiceIdentityVO` 的职责处理输入，完成 `required` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `required` processes its inputs according to `ServiceIdentityVO`'s responsibility, performs the `required` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `required` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `required`, then continue the business flow using its result, exception, or side effect.
         *
         * @param value 输入参数 `value`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param field 输入参数 `field`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        private static String required(String value, String field) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException(field + " is required");
            }
            return value.trim();
        }
    }
