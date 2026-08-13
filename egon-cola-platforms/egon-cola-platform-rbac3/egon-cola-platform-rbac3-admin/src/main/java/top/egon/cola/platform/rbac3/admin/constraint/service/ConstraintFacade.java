package top.egon.cola.platform.rbac3.admin.constraint.service;

import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import top.egon.cola.platform.rbac3.admin.constraint.repository.RoleFactRepository;
import top.egon.cola.platform.rbac3.admin.constraint.repository.ConstraintRepository;
import top.egon.cola.platform.rbac3.admin.constraint.domain.vo.RoleFactVO;
import top.egon.cola.platform.rbac3.admin.constraint.domain.dto.SodCommandDTO;
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
import top.egon.cola.platform.rbac3.admin.constraint.domain.enums.ConstraintTypeEnum;

/**
 * 类型 `ConstraintFacade` 位于当前包内，是类型，用于承载 `Constraint Facade` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `ConstraintFacade` is a type in its package and carries the responsibility, state, or contract for `Constraint Facade`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 * Enforces the different qualification boundaries of SSD and DSD sets.
 */
public final class ConstraintFacade {

    /**
     * 字段 `roleFactSource` 表示 `ConstraintFacade` 中与 `role Fact Source` 相关的状态、依赖、配置或结果（声明类型 `RoleFactRepository`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `roleFactSource` stores the `role Fact Source`-related state, dependency, configuration, or result of `ConstraintFacade` (declared type `RoleFactRepository`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `roleFactSource` 时应保持 `ConstraintFacade` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `roleFactSource`, preserve `ConstraintFacade`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final RoleFactRepository roleFactSource;
    /**
     * 字段 `constraintStore` 表示 `ConstraintFacade` 中与 `constraint Store` 相关的状态、依赖、配置或结果（声明类型 `ConstraintRepository`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `constraintStore` stores the `constraint Store`-related state, dependency, configuration, or result of `ConstraintFacade` (declared type `ConstraintRepository`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `constraintStore` 时应保持 `ConstraintFacade` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `constraintStore`, preserve `ConstraintFacade`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final ConstraintRepository constraintStore;

    /**
     * 构造器 `ConstraintFacade` 用于创建并初始化 `ConstraintFacade` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `ConstraintFacade` creates and initializes `ConstraintFacade`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `ConstraintFacade` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `ConstraintFacade`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param roleFactSource 输入参数 `roleFactSource`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public ConstraintFacade(RoleFactRepository roleFactSource) {
        this(roleFactSource, null);
    }

    /**
     * 构造器 `ConstraintFacade` 用于创建并初始化 `ConstraintFacade` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `ConstraintFacade` creates and initializes `ConstraintFacade`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `ConstraintFacade` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `ConstraintFacade`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param roleFactSource 输入参数 `roleFactSource`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param constraintStore 输入参数 `constraintStore`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public ConstraintFacade(
            RoleFactRepository roleFactSource,
            ConstraintRepository constraintStore) {
        this.roleFactSource = Objects.requireNonNull(roleFactSource, "roleFactSource");
        this.constraintStore = constraintStore;
    }

    /**
     * 方法 `validate` 按照 `ConstraintFacade` 的职责处理输入，完成 `validate` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `validate` processes its inputs according to `ConstraintFacade`'s responsibility, performs the `validate` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `validate` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `validate`, then continue the business flow using its result, exception, or side effect.
     *
     * @param command 输入参数 `command`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public void validate(SodCommandDTO command) {
        Objects.requireNonNull(command, "command");
        if (command.roleIds().isEmpty()
                || new HashSet<>(command.roleIds()).size() != command.roleIds().size()) {
            throw new Rbac3RuleViolation("REQUEST_INVALID");
        }
        if (command.maximumActiveRoles() < 1
                || command.maximumActiveRoles() >= command.roleIds().size()) {
            throw new Rbac3RuleViolation("REQUEST_INVALID");
        }
        if (command.constraintType() == ConstraintTypeEnum.DSD) {
            if (command.applicationId() == null) {
                throw new Rbac3RuleViolation("ROLE_APPLICATION_MISMATCH");
            }
            for (String roleId : command.roleIds()) {
                RoleFactVO role = roleFactSource.require(roleId);
                if (!role.applicationId().equals(command.applicationId())) {
                    throw new Rbac3RuleViolation("ROLE_APPLICATION_MISMATCH");
                }
                if (!role.activationRoot()) {
                    throw new Rbac3RuleViolation("DSD_MEMBER_NOT_ACTIVATION_ROOT");
                }
            }
        } else if (command.applicationId() != null) {
            for (String roleId : command.roleIds()) {
                if (!roleFactSource.require(roleId).applicationId()
                        .equals(command.applicationId())) {
                    throw new Rbac3RuleViolation("ROLE_APPLICATION_MISMATCH");
                }
            }
        }
    }

    /**
     * 方法 `sodSets` 按照 `ConstraintFacade` 的职责处理输入，完成 `sod Sets` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `sodSets` processes its inputs according to `ConstraintFacade`'s responsibility, performs the `sod Sets` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `sodSets` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `sodSets`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public List<SodVO> sodSets(String tenantId) {
        return List.copyOf(requiredStore().sodSets(tenantId));
    }

    /**
     * 方法 `saveSod` 按照 `ConstraintFacade` 的职责处理输入，完成 `save Sod` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `saveSod` processes its inputs according to `ConstraintFacade`'s responsibility, performs the `save Sod` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `saveSod` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `saveSod`, then continue the business flow using its result, exception, or side effect.
     *
     * @param command 输入参数 `command`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public MutationResultVO saveSod(SaveSodCommandDTO command) {
        validate(new SodCommandDTO(
                command.constraintType(),
                command.applicationId(),
                command.roleIds(),
                command.maximumActiveRoles()));
        return requiredStore().saveSod(command);
    }

    /**
     * 方法 `savePrerequisites` 按照 `ConstraintFacade` 的职责处理输入，完成 `save Prerequisites` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `savePrerequisites` processes its inputs according to `ConstraintFacade`'s responsibility, performs the `save Prerequisites` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `savePrerequisites` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `savePrerequisites`, then continue the business flow using its result, exception, or side effect.
     *
     * @param command 输入参数 `command`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public MutationResultVO savePrerequisites(PrerequisiteGroupCommandDTO command) {
        if (command.prerequisiteRoleIds().isEmpty()
                || command.prerequisiteRoleIds().contains(command.targetRoleId())) {
            throw new Rbac3RuleViolation("REQUEST_INVALID");
        }
        RoleFactVO target = roleFactSource.require(command.targetRoleId());
        for (String prerequisiteRoleId : command.prerequisiteRoleIds()) {
            if (!target.applicationId().equals(
                    roleFactSource.require(prerequisiteRoleId).applicationId())) {
                throw new Rbac3RuleViolation("ROLE_APPLICATION_MISMATCH");
            }
        }
        return requiredStore().savePrerequisites(command);
    }

    /**
     * 方法 `saveCardinality` 按照 `ConstraintFacade` 的职责处理输入，完成 `save Cardinality` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `saveCardinality` processes its inputs according to `ConstraintFacade`'s responsibility, performs the `save Cardinality` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `saveCardinality` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `saveCardinality`, then continue the business flow using its result, exception, or side effect.
     *
     * @param command 输入参数 `command`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public MutationResultVO saveCardinality(CardinalityCommandDTO command) {
        if (command.maximumActive() < 1) {
            throw new Rbac3RuleViolation("REQUEST_INVALID");
        }
        roleFactSource.require(command.roleId());
        return requiredStore().saveCardinality(command);
    }

    /**
     * 方法 `dataRules` 按照 `ConstraintFacade` 的职责处理输入，完成 `data Rules` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `dataRules` processes its inputs according to `ConstraintFacade`'s responsibility, performs the `data Rules` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `dataRules` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `dataRules`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public List<DataRuleVO> dataRules(String tenantId) {
        return List.copyOf(requiredStore().dataRules(tenantId));
    }

    /**
     * 方法 `saveDataRule` 按照 `ConstraintFacade` 的职责处理输入，完成 `save Data Rule` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `saveDataRule` processes its inputs according to `ConstraintFacade`'s responsibility, performs the `save Data Rule` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `saveDataRule` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `saveDataRule`, then continue the business flow using its result, exception, or side effect.
     *
     * @param command 输入参数 `command`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public MutationResultVO saveDataRule(DataRuleCommandDTO command) {
        roleFactSource.require(command.roleId());
        return requiredStore().saveDataRule(command);
    }

    /**
     * 方法 `fieldRules` 按照 `ConstraintFacade` 的职责处理输入，完成 `field Rules` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `fieldRules` processes its inputs according to `ConstraintFacade`'s responsibility, performs the `field Rules` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `fieldRules` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `fieldRules`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public List<FieldRuleVO> fieldRules(String tenantId) {
        return List.copyOf(requiredStore().fieldRules(tenantId));
    }

    /**
     * 方法 `saveFieldRule` 按照 `ConstraintFacade` 的职责处理输入，完成 `save Field Rule` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `saveFieldRule` processes its inputs according to `ConstraintFacade`'s responsibility, performs the `save Field Rule` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `saveFieldRule` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `saveFieldRule`, then continue the business flow using its result, exception, or side effect.
     *
     * @param command 输入参数 `command`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public MutationResultVO saveFieldRule(FieldRuleCommandDTO command) {
        roleFactSource.require(command.roleId());
        return requiredStore().saveFieldRule(command);
    }

    /**
     * 方法 `operationSodRules` 按照 `ConstraintFacade` 的职责处理输入，完成 `operation Sod Rules` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `operationSodRules` processes its inputs according to `ConstraintFacade`'s responsibility, performs the `operation Sod Rules` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `operationSodRules` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `operationSodRules`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public List<OperationSodRuleVO> operationSodRules(String tenantId) {
        return List.copyOf(requiredStore().operationSodRules(tenantId));
    }

    /**
     * 方法 `saveOperationSodRule` 按照 `ConstraintFacade` 的职责处理输入，完成 `save Operation Sod Rule` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `saveOperationSodRule` processes its inputs according to `ConstraintFacade`'s responsibility, performs the `save Operation Sod Rule` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `saveOperationSodRule` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `saveOperationSodRule`, then continue the business flow using its result, exception, or side effect.
     *
     * @param command 输入参数 `command`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public MutationResultVO saveOperationSodRule(OperationSodRuleCommandDTO command) {
        if (command.priorActionCode().equals(command.forbiddenLaterActionCode())) {
            throw new Rbac3RuleViolation("REQUEST_INVALID");
        }
        return requiredStore().saveOperationSodRule(command);
    }

    /**
     * 方法 `requiredStore` 按照 `ConstraintFacade` 的职责处理输入，完成 `required Store` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `requiredStore` processes its inputs according to `ConstraintFacade`'s responsibility, performs the `required Store` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `requiredStore` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `requiredStore`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private ConstraintRepository requiredStore() {
        if (constraintStore == null) {
            throw new IllegalStateException("constraint store is not configured");
        }
        return constraintStore;
    }

















    }
