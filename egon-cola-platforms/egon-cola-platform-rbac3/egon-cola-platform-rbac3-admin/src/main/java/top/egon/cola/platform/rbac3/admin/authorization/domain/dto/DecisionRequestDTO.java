package top.egon.cola.platform.rbac3.admin.authorization.domain.dto;

import top.egon.cola.platform.rbac3.admin.authorization.domain.enums.AuthorizationDecisionDecisionTypeEnum;
import top.egon.cola.platform.rbac3.admin.authorization.domain.vo.AuthorizationDecisionResourceVO;
import top.egon.cola.platform.rbac3.admin.authorization.domain.vo.AuthorizationDecisionSubjectVO;
import top.egon.cola.platform.rbac3.admin.authorization.domain.vo.TokenVersionsVO;

import java.util.Objects;
import java.util.Set;

/**
     * 一致快照上的类型化授权判定请求。
     * Typed authorization-decision request evaluated against a consistent snapshot.
     *
     * @param subject 用户主体定位信息 / user-subject locator
     * @param permissionCode 待校验权限编码 / permission code to check
     * @param resource 目标资源 / target resource
     * @param requestedDecisions 请求的判定类型 / requested decision types
     * @param tokenVersions Token 授权版本 / token authorization versions
     * 语义与用法：将 `DecisionRequestDTO` 作为 `AuthorizationDecisionService` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `DecisionRequestDTO` as the responsibility boundary of `AuthorizationDecisionService`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public record DecisionRequestDTO(
            /**
             * 字段 `subject` 表示 `DecisionRequestDTO` 中与 `subject` 相关的状态、依赖、配置或结果（声明类型 `AuthorizationDecisionSubjectVO`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `subject` stores the `subject`-related state, dependency, configuration, or result of `DecisionRequestDTO` (declared type `AuthorizationDecisionSubjectVO`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `subject` 时应保持 `DecisionRequestDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `subject`, preserve `DecisionRequestDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            AuthorizationDecisionSubjectVO subject,
            /**
             * 字段 `permissionCode` 表示 `DecisionRequestDTO` 中与 `permission Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `permissionCode` stores the `permission Code`-related state, dependency, configuration, or result of `DecisionRequestDTO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `permissionCode` 时应保持 `DecisionRequestDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `permissionCode`, preserve `DecisionRequestDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String permissionCode,
            /**
             * 字段 `resource` 表示 `DecisionRequestDTO` 中与 `resource` 相关的状态、依赖、配置或结果（声明类型 `AuthorizationDecisionResourceVO`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `resource` stores the `resource`-related state, dependency, configuration, or result of `DecisionRequestDTO` (declared type `AuthorizationDecisionResourceVO`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `resource` 时应保持 `DecisionRequestDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `resource`, preserve `DecisionRequestDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            AuthorizationDecisionResourceVO resource,
            /**
             * 字段 `requestedDecisions` 表示 `DecisionRequestDTO` 中与 `requested Decisions` 相关的状态、依赖、配置或结果（声明类型 `Set&lt;AuthorizationDecisionDecisionTypeEnum&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `requestedDecisions` stores the `requested Decisions`-related state, dependency, configuration, or result of `DecisionRequestDTO` (declared type `Set&lt;AuthorizationDecisionDecisionTypeEnum&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `requestedDecisions` 时应保持 `DecisionRequestDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `requestedDecisions`, preserve `DecisionRequestDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            Set<AuthorizationDecisionDecisionTypeEnum> requestedDecisions,
            /**
             * 字段 `tokenVersions` 表示 `DecisionRequestDTO` 中与 `token Versions` 相关的状态、依赖、配置或结果（声明类型 `TokenVersionsVO`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tokenVersions` stores the `token Versions`-related state, dependency, configuration, or result of `DecisionRequestDTO` (declared type `TokenVersionsVO`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tokenVersions` 时应保持 `DecisionRequestDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tokenVersions`, preserve `DecisionRequestDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            TokenVersionsVO tokenVersions) {

        /**
         * 校验并固化类型化判定请求。
         * Validates and freezes the typed decision request.
         * 用法：通过 `DecisionRequestDTO` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `DecisionRequestDTO`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         * @param subject 输入参数 `subject`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param permissionCode 输入参数 `permissionCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param resource 输入参数 `resource`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param requestedDecisions 输入参数 `requestedDecisions`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param tokenVersions 输入参数 `tokenVersions`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public DecisionRequestDTO {
            subject = Objects.requireNonNull(subject, "subject");
            permissionCode = required(permissionCode, "permissionCode");
            resource = Objects.requireNonNull(resource, "resource");
            requestedDecisions = Set.copyOf(Objects.requireNonNull(
                    requestedDecisions, "requestedDecisions"));
            if (requestedDecisions.isEmpty()) {
                throw new IllegalArgumentException("requestedDecisions must not be empty");
            }
            tokenVersions = Objects.requireNonNull(tokenVersions, "tokenVersions");
        }

        /** 校验必填文本。 / Validates required text. */
        private static String required(String value, String fieldName) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException(fieldName + " is required");
            }
            return value.trim();
        }
    }
