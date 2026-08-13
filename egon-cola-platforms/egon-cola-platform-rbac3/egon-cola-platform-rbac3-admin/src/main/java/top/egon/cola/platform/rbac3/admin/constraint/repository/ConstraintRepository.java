package top.egon.cola.platform.rbac3.admin.constraint.repository;

import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import top.egon.cola.platform.rbac3.admin.constraint.domain.dto.SaveSodCommandDTO;
import top.egon.cola.platform.rbac3.admin.constraint.domain.dto.PrerequisiteGroupCommandDTO;
import top.egon.cola.platform.rbac3.admin.constraint.domain.dto.CardinalityCommandDTO;
import top.egon.cola.platform.rbac3.admin.constraint.domain.dto.DataRuleCommandDTO;
import top.egon.cola.platform.rbac3.admin.constraint.domain.dto.FieldRuleCommandDTO;
import top.egon.cola.platform.rbac3.admin.constraint.domain.dto.OperationSodRuleCommandDTO;
import top.egon.cola.platform.rbac3.admin.constraint.domain.vo.MutationResultVO;
import top.egon.cola.platform.rbac3.admin.constraint.domain.vo.SodVO;
import top.egon.cola.platform.rbac3.admin.constraint.domain.vo.DataRuleVO;
import top.egon.cola.platform.rbac3.admin.constraint.domain.vo.FieldRuleVO;
import top.egon.cola.platform.rbac3.admin.constraint.domain.vo.OperationSodRuleVO;
import top.egon.cola.platform.rbac3.admin.constraint.service.ConstraintFacade;

/**
     * 类型 `ConstraintRepository` 位于 `ConstraintFacade` 内，是接口，用于承载 `Constraint Store` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `ConstraintRepository` is an interface inside `ConstraintFacade` and carries the responsibility, state, or contract for `Constraint Store`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `ConstraintRepository` 作为 `ConstraintFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `ConstraintRepository` as the responsibility boundary of `ConstraintFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public interface ConstraintRepository {

        /**
         * 方法 `sodSets` 按照 `ConstraintRepository` 的职责处理输入，完成 `sod Sets` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `sodSets` processes its inputs according to `ConstraintRepository`'s responsibility, performs the `sod Sets` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `sodSets` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `sodSets`, then continue the business flow using its result, exception, or side effect.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        List<SodVO> sodSets(String tenantId);

        /**
         * 方法 `saveSod` 按照 `ConstraintRepository` 的职责处理输入，完成 `save Sod` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `saveSod` processes its inputs according to `ConstraintRepository`'s responsibility, performs the `save Sod` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `saveSod` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `saveSod`, then continue the business flow using its result, exception, or side effect.
         *
         * @param command 输入参数 `command`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        MutationResultVO saveSod(SaveSodCommandDTO command);

        /**
         * 方法 `savePrerequisites` 按照 `ConstraintRepository` 的职责处理输入，完成 `save Prerequisites` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `savePrerequisites` processes its inputs according to `ConstraintRepository`'s responsibility, performs the `save Prerequisites` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `savePrerequisites` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `savePrerequisites`, then continue the business flow using its result, exception, or side effect.
         *
         * @param command 输入参数 `command`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        MutationResultVO savePrerequisites(PrerequisiteGroupCommandDTO command);

        /**
         * 方法 `saveCardinality` 按照 `ConstraintRepository` 的职责处理输入，完成 `save Cardinality` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `saveCardinality` processes its inputs according to `ConstraintRepository`'s responsibility, performs the `save Cardinality` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `saveCardinality` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `saveCardinality`, then continue the business flow using its result, exception, or side effect.
         *
         * @param command 输入参数 `command`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        MutationResultVO saveCardinality(CardinalityCommandDTO command);

        /**
         * 方法 `dataRules` 按照 `ConstraintRepository` 的职责处理输入，完成 `data Rules` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `dataRules` processes its inputs according to `ConstraintRepository`'s responsibility, performs the `data Rules` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `dataRules` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `dataRules`, then continue the business flow using its result, exception, or side effect.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        List<DataRuleVO> dataRules(String tenantId);

        /**
         * 方法 `saveDataRule` 按照 `ConstraintRepository` 的职责处理输入，完成 `save Data Rule` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `saveDataRule` processes its inputs according to `ConstraintRepository`'s responsibility, performs the `save Data Rule` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `saveDataRule` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `saveDataRule`, then continue the business flow using its result, exception, or side effect.
         *
         * @param command 输入参数 `command`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        MutationResultVO saveDataRule(DataRuleCommandDTO command);

        /**
         * 方法 `fieldRules` 按照 `ConstraintRepository` 的职责处理输入，完成 `field Rules` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `fieldRules` processes its inputs according to `ConstraintRepository`'s responsibility, performs the `field Rules` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `fieldRules` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `fieldRules`, then continue the business flow using its result, exception, or side effect.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        List<FieldRuleVO> fieldRules(String tenantId);

        /**
         * 方法 `saveFieldRule` 按照 `ConstraintRepository` 的职责处理输入，完成 `save Field Rule` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `saveFieldRule` processes its inputs according to `ConstraintRepository`'s responsibility, performs the `save Field Rule` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `saveFieldRule` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `saveFieldRule`, then continue the business flow using its result, exception, or side effect.
         *
         * @param command 输入参数 `command`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        MutationResultVO saveFieldRule(FieldRuleCommandDTO command);

        /**
         * 方法 `operationSodRules` 按照 `ConstraintRepository` 的职责处理输入，完成 `operation Sod Rules` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `operationSodRules` processes its inputs according to `ConstraintRepository`'s responsibility, performs the `operation Sod Rules` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `operationSodRules` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `operationSodRules`, then continue the business flow using its result, exception, or side effect.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        List<OperationSodRuleVO> operationSodRules(String tenantId);

        /**
         * 方法 `saveOperationSodRule` 按照 `ConstraintRepository` 的职责处理输入，完成 `save Operation Sod Rule` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `saveOperationSodRule` processes its inputs according to `ConstraintRepository`'s responsibility, performs the `save Operation Sod Rule` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `saveOperationSodRule` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `saveOperationSodRule`, then continue the business flow using its result, exception, or side effect.
         *
         * @param command 输入参数 `command`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        MutationResultVO saveOperationSodRule(OperationSodRuleCommandDTO command);
    }
