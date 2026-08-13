package top.egon.cola.platform.rbac3.admin.constraint.application;

import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/**
 * 类型 `ConstraintFacade` 位于当前包内，是类型，用于承载 `Constraint Facade` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `ConstraintFacade` is a type in its package and carries the responsibility, state, or contract for `Constraint Facade`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 * Enforces the different qualification boundaries of SSD and DSD sets.
 */
public final class ConstraintFacade {

    /**
     * 字段 `roleFactSource` 表示 `ConstraintFacade` 中与 `role Fact Source` 相关的状态、依赖、配置或结果（声明类型 `RoleFactSource`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `roleFactSource` stores the `role Fact Source`-related state, dependency, configuration, or result of `ConstraintFacade` (declared type `RoleFactSource`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `roleFactSource` 时应保持 `ConstraintFacade` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `roleFactSource`, preserve `ConstraintFacade`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final RoleFactSource roleFactSource;
    /**
     * 字段 `constraintStore` 表示 `ConstraintFacade` 中与 `constraint Store` 相关的状态、依赖、配置或结果（声明类型 `ConstraintStore`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `constraintStore` stores the `constraint Store`-related state, dependency, configuration, or result of `ConstraintFacade` (declared type `ConstraintStore`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `constraintStore` 时应保持 `ConstraintFacade` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `constraintStore`, preserve `ConstraintFacade`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final ConstraintStore constraintStore;

    /**
     * 构造器 `ConstraintFacade` 用于创建并初始化 `ConstraintFacade` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `ConstraintFacade` creates and initializes `ConstraintFacade`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `ConstraintFacade` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `ConstraintFacade`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param roleFactSource 输入参数 `roleFactSource`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public ConstraintFacade(RoleFactSource roleFactSource) {
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
            RoleFactSource roleFactSource,
            ConstraintStore constraintStore) {
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
    public void validate(SodCommand command) {
        Objects.requireNonNull(command, "command");
        if (command.roleIds().isEmpty()
                || new HashSet<>(command.roleIds()).size() != command.roleIds().size()) {
            throw new Rbac3RuleViolation("REQUEST_INVALID");
        }
        if (command.maximumActiveRoles() < 1
                || command.maximumActiveRoles() >= command.roleIds().size()) {
            throw new Rbac3RuleViolation("REQUEST_INVALID");
        }
        if (command.constraintType() == ConstraintType.DSD) {
            if (command.applicationId() == null) {
                throw new Rbac3RuleViolation("ROLE_APPLICATION_MISMATCH");
            }
            for (String roleId : command.roleIds()) {
                RoleFact role = roleFactSource.require(roleId);
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
    public List<SodView> sodSets(String tenantId) {
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
    public MutationResult saveSod(SaveSodCommand command) {
        validate(new SodCommand(
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
    public MutationResult savePrerequisites(PrerequisiteGroupCommand command) {
        if (command.prerequisiteRoleIds().isEmpty()
                || command.prerequisiteRoleIds().contains(command.targetRoleId())) {
            throw new Rbac3RuleViolation("REQUEST_INVALID");
        }
        RoleFact target = roleFactSource.require(command.targetRoleId());
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
    public MutationResult saveCardinality(CardinalityCommand command) {
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
    public List<DataRuleView> dataRules(String tenantId) {
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
    public MutationResult saveDataRule(DataRuleCommand command) {
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
    public List<FieldRuleView> fieldRules(String tenantId) {
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
    public MutationResult saveFieldRule(FieldRuleCommand command) {
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
    public List<OperationSodRuleView> operationSodRules(String tenantId) {
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
    public MutationResult saveOperationSodRule(OperationSodRuleCommand command) {
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
    private ConstraintStore requiredStore() {
        if (constraintStore == null) {
            throw new IllegalStateException("constraint store is not configured");
        }
        return constraintStore;
    }

    /**
     * 类型 `RoleFactSource` 位于 `ConstraintFacade` 内，是接口，用于承载 `Role Fact Source` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `RoleFactSource` is an interface inside `ConstraintFacade` and carries the responsibility, state, or contract for `Role Fact Source`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `RoleFactSource` 作为 `ConstraintFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `RoleFactSource` as the responsibility boundary of `ConstraintFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    @FunctionalInterface
    public interface RoleFactSource {

        /**
         * 方法 `require` 按照 `RoleFactSource` 的职责处理输入，完成 `require` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `require` processes its inputs according to `RoleFactSource`'s responsibility, performs the `require` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `require` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `require`, then continue the business flow using its result, exception, or side effect.
         *
         * @param roleId 输入参数 `roleId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        RoleFact require(String roleId);
    }

    /**
     * 类型 `ConstraintStore` 位于 `ConstraintFacade` 内，是接口，用于承载 `Constraint Store` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `ConstraintStore` is an interface inside `ConstraintFacade` and carries the responsibility, state, or contract for `Constraint Store`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `ConstraintStore` 作为 `ConstraintFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `ConstraintStore` as the responsibility boundary of `ConstraintFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public interface ConstraintStore {

        /**
         * 方法 `sodSets` 按照 `ConstraintStore` 的职责处理输入，完成 `sod Sets` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `sodSets` processes its inputs according to `ConstraintStore`'s responsibility, performs the `sod Sets` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `sodSets` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `sodSets`, then continue the business flow using its result, exception, or side effect.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        List<SodView> sodSets(String tenantId);

        /**
         * 方法 `saveSod` 按照 `ConstraintStore` 的职责处理输入，完成 `save Sod` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `saveSod` processes its inputs according to `ConstraintStore`'s responsibility, performs the `save Sod` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `saveSod` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `saveSod`, then continue the business flow using its result, exception, or side effect.
         *
         * @param command 输入参数 `command`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        MutationResult saveSod(SaveSodCommand command);

        /**
         * 方法 `savePrerequisites` 按照 `ConstraintStore` 的职责处理输入，完成 `save Prerequisites` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `savePrerequisites` processes its inputs according to `ConstraintStore`'s responsibility, performs the `save Prerequisites` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `savePrerequisites` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `savePrerequisites`, then continue the business flow using its result, exception, or side effect.
         *
         * @param command 输入参数 `command`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        MutationResult savePrerequisites(PrerequisiteGroupCommand command);

        /**
         * 方法 `saveCardinality` 按照 `ConstraintStore` 的职责处理输入，完成 `save Cardinality` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `saveCardinality` processes its inputs according to `ConstraintStore`'s responsibility, performs the `save Cardinality` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `saveCardinality` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `saveCardinality`, then continue the business flow using its result, exception, or side effect.
         *
         * @param command 输入参数 `command`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        MutationResult saveCardinality(CardinalityCommand command);

        /**
         * 方法 `dataRules` 按照 `ConstraintStore` 的职责处理输入，完成 `data Rules` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `dataRules` processes its inputs according to `ConstraintStore`'s responsibility, performs the `data Rules` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `dataRules` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `dataRules`, then continue the business flow using its result, exception, or side effect.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        List<DataRuleView> dataRules(String tenantId);

        /**
         * 方法 `saveDataRule` 按照 `ConstraintStore` 的职责处理输入，完成 `save Data Rule` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `saveDataRule` processes its inputs according to `ConstraintStore`'s responsibility, performs the `save Data Rule` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `saveDataRule` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `saveDataRule`, then continue the business flow using its result, exception, or side effect.
         *
         * @param command 输入参数 `command`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        MutationResult saveDataRule(DataRuleCommand command);

        /**
         * 方法 `fieldRules` 按照 `ConstraintStore` 的职责处理输入，完成 `field Rules` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `fieldRules` processes its inputs according to `ConstraintStore`'s responsibility, performs the `field Rules` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `fieldRules` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `fieldRules`, then continue the business flow using its result, exception, or side effect.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        List<FieldRuleView> fieldRules(String tenantId);

        /**
         * 方法 `saveFieldRule` 按照 `ConstraintStore` 的职责处理输入，完成 `save Field Rule` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `saveFieldRule` processes its inputs according to `ConstraintStore`'s responsibility, performs the `save Field Rule` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `saveFieldRule` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `saveFieldRule`, then continue the business flow using its result, exception, or side effect.
         *
         * @param command 输入参数 `command`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        MutationResult saveFieldRule(FieldRuleCommand command);

        /**
         * 方法 `operationSodRules` 按照 `ConstraintStore` 的职责处理输入，完成 `operation Sod Rules` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `operationSodRules` processes its inputs according to `ConstraintStore`'s responsibility, performs the `operation Sod Rules` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `operationSodRules` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `operationSodRules`, then continue the business flow using its result, exception, or side effect.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        List<OperationSodRuleView> operationSodRules(String tenantId);

        /**
         * 方法 `saveOperationSodRule` 按照 `ConstraintStore` 的职责处理输入，完成 `save Operation Sod Rule` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `saveOperationSodRule` processes its inputs according to `ConstraintStore`'s responsibility, performs the `save Operation Sod Rule` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `saveOperationSodRule` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `saveOperationSodRule`, then continue the business flow using its result, exception, or side effect.
         *
         * @param command 输入参数 `command`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        MutationResult saveOperationSodRule(OperationSodRuleCommand command);
    }

    /**
     * 类型 `RoleFact` 位于 `ConstraintFacade` 内，是记录类型，用于承载 `Role Fact` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `RoleFact` is a record inside `ConstraintFacade` and carries the responsibility, state, or contract for `Role Fact`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `RoleFact` 作为 `ConstraintFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `RoleFact` as the responsibility boundary of `ConstraintFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param roleId 记录组件 `roleId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `roleId` carries constructor data whose meaning is defined by the record contract.
     * @param applicationId 记录组件 `applicationId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `applicationId` carries constructor data whose meaning is defined by the record contract.
     * @param activationRoot 记录组件 `activationRoot` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `activationRoot` carries constructor data whose meaning is defined by the record contract.
     */
    public record RoleFact(/**
 * 字段 `roleId` 表示 `RoleFact` 中与 `role Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `roleId` stores the `role Id`-related state, dependency, configuration, or result of `RoleFact` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `roleId` 时应保持 `RoleFact` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `roleId`, preserve `RoleFact`'s lifecycle, immutability, and thread-safety constraints.
 */ String roleId, /**
 * 字段 `applicationId` 表示 `RoleFact` 中与 `application Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `applicationId` stores the `application Id`-related state, dependency, configuration, or result of `RoleFact` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `applicationId` 时应保持 `RoleFact` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `applicationId`, preserve `RoleFact`'s lifecycle, immutability, and thread-safety constraints.
 */ String applicationId, /**
 * 字段 `activationRoot` 表示 `RoleFact` 中与 `activation Root` 相关的状态、依赖、配置或结果（声明类型 `boolean`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `activationRoot` stores the `activation Root`-related state, dependency, configuration, or result of `RoleFact` (declared type `boolean`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `activationRoot` 时应保持 `RoleFact` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `activationRoot`, preserve `RoleFact`'s lifecycle, immutability, and thread-safety constraints.
 */ boolean activationRoot) {
    }

    /**
     * 类型 `SodCommand` 位于 `ConstraintFacade` 内，是记录类型，用于承载 `Sod Command` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `SodCommand` is a record inside `ConstraintFacade` and carries the responsibility, state, or contract for `Sod Command`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `SodCommand` 作为 `ConstraintFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `SodCommand` as the responsibility boundary of `ConstraintFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param constraintType 记录组件 `constraintType` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `constraintType` carries constructor data whose meaning is defined by the record contract.
     * @param applicationId 记录组件 `applicationId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `applicationId` carries constructor data whose meaning is defined by the record contract.
     * @param roleIds 记录组件 `roleIds` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `roleIds` carries constructor data whose meaning is defined by the record contract.
     * @param maximumActiveRoles 记录组件 `maximumActiveRoles` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `maximumActiveRoles` carries constructor data whose meaning is defined by the record contract.
     */
    public record SodCommand(
            /**
             * 字段 `constraintType` 表示 `SodCommand` 中与 `constraint Type` 相关的状态、依赖、配置或结果（声明类型 `ConstraintType`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `constraintType` stores the `constraint Type`-related state, dependency, configuration, or result of `SodCommand` (declared type `ConstraintType`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `constraintType` 时应保持 `SodCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `constraintType`, preserve `SodCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            ConstraintType constraintType,
            /**
             * 字段 `applicationId` 表示 `SodCommand` 中与 `application Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `applicationId` stores the `application Id`-related state, dependency, configuration, or result of `SodCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `applicationId` 时应保持 `SodCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `applicationId`, preserve `SodCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String applicationId,
            /**
             * 字段 `roleIds` 表示 `SodCommand` 中与 `role Ids` 相关的状态、依赖、配置或结果（声明类型 `List&lt;String&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `roleIds` stores the `role Ids`-related state, dependency, configuration, or result of `SodCommand` (declared type `List&lt;String&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `roleIds` 时应保持 `SodCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `roleIds`, preserve `SodCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            List<String> roleIds,
            /**
             * 字段 `maximumActiveRoles` 表示 `SodCommand` 中与 `maximum Active Roles` 相关的状态、依赖、配置或结果（声明类型 `int`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `maximumActiveRoles` stores the `maximum Active Roles`-related state, dependency, configuration, or result of `SodCommand` (declared type `int`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `maximumActiveRoles` 时应保持 `SodCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `maximumActiveRoles`, preserve `SodCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            int maximumActiveRoles
    ) {

        /**
         * 构造器 `SodCommand` 用于创建并初始化 `SodCommand` 实例，建立该类型后续方法所依赖的状态和不变量。
         * Constructor `SodCommand` creates and initializes `SodCommand`, establishing the state and invariants required by subsequent operations.
         *
         * 用法：通过 `SodCommand` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `SodCommand`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         *
         * @param constraintType 输入参数 `constraintType`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param applicationId 输入参数 `applicationId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param roleIds 输入参数 `roleIds`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param maximumActiveRoles 输入参数 `maximumActiveRoles`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public SodCommand {
            constraintType = Objects.requireNonNull(constraintType, "constraintType");
            roleIds = List.copyOf(Objects.requireNonNull(roleIds, "roleIds"));
        }
    }

    /**
     * 类型 `SaveSodCommand` 位于 `ConstraintFacade` 内，是记录类型，用于承载 `Save Sod Command` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `SaveSodCommand` is a record inside `ConstraintFacade` and carries the responsibility, state, or contract for `Save Sod Command`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `SaveSodCommand` 作为 `ConstraintFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `SaveSodCommand` as the responsibility boundary of `ConstraintFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param tenantId 记录组件 `tenantId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantId` carries constructor data whose meaning is defined by the record contract.
     * @param setId 记录组件 `setId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `setId` carries constructor data whose meaning is defined by the record contract.
     * @param setCode 记录组件 `setCode` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `setCode` carries constructor data whose meaning is defined by the record contract.
     * @param constraintType 记录组件 `constraintType` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `constraintType` carries constructor data whose meaning is defined by the record contract.
     * @param applicationId 记录组件 `applicationId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `applicationId` carries constructor data whose meaning is defined by the record contract.
     * @param maximumActiveRoles 记录组件 `maximumActiveRoles` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `maximumActiveRoles` carries constructor data whose meaning is defined by the record contract.
     * @param roleIds 记录组件 `roleIds` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `roleIds` carries constructor data whose meaning is defined by the record contract.
     * @param validFrom 记录组件 `validFrom` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `validFrom` carries constructor data whose meaning is defined by the record contract.
     * @param validTo 记录组件 `validTo` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `validTo` carries constructor data whose meaning is defined by the record contract.
     * @param expectedVersion 记录组件 `expectedVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `expectedVersion` carries constructor data whose meaning is defined by the record contract.
     * @param actorId 记录组件 `actorId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `actorId` carries constructor data whose meaning is defined by the record contract.
     */
    public record SaveSodCommand(
            /**
             * 字段 `tenantId` 表示 `SaveSodCommand` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `SaveSodCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `SaveSodCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `SaveSodCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String tenantId,
            /**
             * 字段 `setId` 表示 `SaveSodCommand` 中与 `set Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `setId` stores the `set Id`-related state, dependency, configuration, or result of `SaveSodCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `setId` 时应保持 `SaveSodCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `setId`, preserve `SaveSodCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String setId,
            /**
             * 字段 `setCode` 表示 `SaveSodCommand` 中与 `set Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `setCode` stores the `set Code`-related state, dependency, configuration, or result of `SaveSodCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `setCode` 时应保持 `SaveSodCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `setCode`, preserve `SaveSodCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String setCode,
            /**
             * 字段 `constraintType` 表示 `SaveSodCommand` 中与 `constraint Type` 相关的状态、依赖、配置或结果（声明类型 `ConstraintType`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `constraintType` stores the `constraint Type`-related state, dependency, configuration, or result of `SaveSodCommand` (declared type `ConstraintType`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `constraintType` 时应保持 `SaveSodCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `constraintType`, preserve `SaveSodCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            ConstraintType constraintType,
            /**
             * 字段 `applicationId` 表示 `SaveSodCommand` 中与 `application Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `applicationId` stores the `application Id`-related state, dependency, configuration, or result of `SaveSodCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `applicationId` 时应保持 `SaveSodCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `applicationId`, preserve `SaveSodCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String applicationId,
            /**
             * 字段 `maximumActiveRoles` 表示 `SaveSodCommand` 中与 `maximum Active Roles` 相关的状态、依赖、配置或结果（声明类型 `int`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `maximumActiveRoles` stores the `maximum Active Roles`-related state, dependency, configuration, or result of `SaveSodCommand` (declared type `int`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `maximumActiveRoles` 时应保持 `SaveSodCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `maximumActiveRoles`, preserve `SaveSodCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            int maximumActiveRoles,
            /**
             * 字段 `roleIds` 表示 `SaveSodCommand` 中与 `role Ids` 相关的状态、依赖、配置或结果（声明类型 `List&lt;String&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `roleIds` stores the `role Ids`-related state, dependency, configuration, or result of `SaveSodCommand` (declared type `List&lt;String&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `roleIds` 时应保持 `SaveSodCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `roleIds`, preserve `SaveSodCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            List<String> roleIds,
            /**
             * 字段 `validFrom` 表示 `SaveSodCommand` 中与 `valid From` 相关的状态、依赖、配置或结果（声明类型 `java.time.Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `validFrom` stores the `valid From`-related state, dependency, configuration, or result of `SaveSodCommand` (declared type `java.time.Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `validFrom` 时应保持 `SaveSodCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `validFrom`, preserve `SaveSodCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            java.time.Instant validFrom,
            /**
             * 字段 `validTo` 表示 `SaveSodCommand` 中与 `valid To` 相关的状态、依赖、配置或结果（声明类型 `java.time.Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `validTo` stores the `valid To`-related state, dependency, configuration, or result of `SaveSodCommand` (declared type `java.time.Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `validTo` 时应保持 `SaveSodCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `validTo`, preserve `SaveSodCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            java.time.Instant validTo,
            /**
             * 字段 `expectedVersion` 表示 `SaveSodCommand` 中与 `expected Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `expectedVersion` stores the `expected Version`-related state, dependency, configuration, or result of `SaveSodCommand` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `expectedVersion` 时应保持 `SaveSodCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `expectedVersion`, preserve `SaveSodCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            long expectedVersion,
            /**
             * 字段 `actorId` 表示 `SaveSodCommand` 中与 `actor Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `actorId` stores the `actor Id`-related state, dependency, configuration, or result of `SaveSodCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `actorId` 时应保持 `SaveSodCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `actorId`, preserve `SaveSodCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String actorId
    ) {

        /**
         * 构造器 `SaveSodCommand` 用于创建并初始化 `SaveSodCommand` 实例，建立该类型后续方法所依赖的状态和不变量。
         * Constructor `SaveSodCommand` creates and initializes `SaveSodCommand`, establishing the state and invariants required by subsequent operations.
         *
         * 用法：通过 `SaveSodCommand` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `SaveSodCommand`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param setId 输入参数 `setId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param setCode 输入参数 `setCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param constraintType 输入参数 `constraintType`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param applicationId 输入参数 `applicationId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param maximumActiveRoles 输入参数 `maximumActiveRoles`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param roleIds 输入参数 `roleIds`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param validFrom 输入参数 `validFrom`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param validTo 输入参数 `validTo`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param expectedVersion 输入参数 `expectedVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public SaveSodCommand {
            roleIds = List.copyOf(roleIds);
        }
    }

    /**
     * 类型 `PrerequisiteGroupCommand` 位于 `ConstraintFacade` 内，是记录类型，用于承载 `Prerequisite Group Command` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `PrerequisiteGroupCommand` is a record inside `ConstraintFacade` and carries the responsibility, state, or contract for `Prerequisite Group Command`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `PrerequisiteGroupCommand` 作为 `ConstraintFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `PrerequisiteGroupCommand` as the responsibility boundary of `ConstraintFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param tenantId 记录组件 `tenantId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantId` carries constructor data whose meaning is defined by the record contract.
     * @param targetRoleId 记录组件 `targetRoleId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `targetRoleId` carries constructor data whose meaning is defined by the record contract.
     * @param groupCode 记录组件 `groupCode` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `groupCode` carries constructor data whose meaning is defined by the record contract.
     * @param matchMode 记录组件 `matchMode` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `matchMode` carries constructor data whose meaning is defined by the record contract.
     * @param prerequisiteRoleIds 记录组件 `prerequisiteRoleIds` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `prerequisiteRoleIds` carries constructor data whose meaning is defined by the record contract.
     * @param expectedRoleVersion 记录组件 `expectedRoleVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `expectedRoleVersion` carries constructor data whose meaning is defined by the record contract.
     * @param actorId 记录组件 `actorId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `actorId` carries constructor data whose meaning is defined by the record contract.
     */
    public record PrerequisiteGroupCommand(
            /**
             * 字段 `tenantId` 表示 `PrerequisiteGroupCommand` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `PrerequisiteGroupCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `PrerequisiteGroupCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `PrerequisiteGroupCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String tenantId,
            /**
             * 字段 `targetRoleId` 表示 `PrerequisiteGroupCommand` 中与 `target Role Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `targetRoleId` stores the `target Role Id`-related state, dependency, configuration, or result of `PrerequisiteGroupCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `targetRoleId` 时应保持 `PrerequisiteGroupCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `targetRoleId`, preserve `PrerequisiteGroupCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String targetRoleId,
            /**
             * 字段 `groupCode` 表示 `PrerequisiteGroupCommand` 中与 `group Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `groupCode` stores the `group Code`-related state, dependency, configuration, or result of `PrerequisiteGroupCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `groupCode` 时应保持 `PrerequisiteGroupCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `groupCode`, preserve `PrerequisiteGroupCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String groupCode,
            /**
             * 字段 `matchMode` 表示 `PrerequisiteGroupCommand` 中与 `match Mode` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `matchMode` stores the `match Mode`-related state, dependency, configuration, or result of `PrerequisiteGroupCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `matchMode` 时应保持 `PrerequisiteGroupCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `matchMode`, preserve `PrerequisiteGroupCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String matchMode,
            /**
             * 字段 `prerequisiteRoleIds` 表示 `PrerequisiteGroupCommand` 中与 `prerequisite Role Ids` 相关的状态、依赖、配置或结果（声明类型 `List&lt;String&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `prerequisiteRoleIds` stores the `prerequisite Role Ids`-related state, dependency, configuration, or result of `PrerequisiteGroupCommand` (declared type `List&lt;String&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `prerequisiteRoleIds` 时应保持 `PrerequisiteGroupCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `prerequisiteRoleIds`, preserve `PrerequisiteGroupCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            List<String> prerequisiteRoleIds,
            /**
             * 字段 `expectedRoleVersion` 表示 `PrerequisiteGroupCommand` 中与 `expected Role Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `expectedRoleVersion` stores the `expected Role Version`-related state, dependency, configuration, or result of `PrerequisiteGroupCommand` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `expectedRoleVersion` 时应保持 `PrerequisiteGroupCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `expectedRoleVersion`, preserve `PrerequisiteGroupCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            long expectedRoleVersion,
            /**
             * 字段 `actorId` 表示 `PrerequisiteGroupCommand` 中与 `actor Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `actorId` stores the `actor Id`-related state, dependency, configuration, or result of `PrerequisiteGroupCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `actorId` 时应保持 `PrerequisiteGroupCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `actorId`, preserve `PrerequisiteGroupCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String actorId
    ) {

        /**
         * 构造器 `PrerequisiteGroupCommand` 用于创建并初始化 `PrerequisiteGroupCommand` 实例，建立该类型后续方法所依赖的状态和不变量。
         * Constructor `PrerequisiteGroupCommand` creates and initializes `PrerequisiteGroupCommand`, establishing the state and invariants required by subsequent operations.
         *
         * 用法：通过 `PrerequisiteGroupCommand` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `PrerequisiteGroupCommand`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param targetRoleId 输入参数 `targetRoleId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param groupCode 输入参数 `groupCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param matchMode 输入参数 `matchMode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param prerequisiteRoleIds 输入参数 `prerequisiteRoleIds`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param expectedRoleVersion 输入参数 `expectedRoleVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public PrerequisiteGroupCommand {
            prerequisiteRoleIds = List.copyOf(prerequisiteRoleIds);
        }
    }

    /**
     * 类型 `CardinalityCommand` 位于 `ConstraintFacade` 内，是记录类型，用于承载 `Cardinality Command` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `CardinalityCommand` is a record inside `ConstraintFacade` and carries the responsibility, state, or contract for `Cardinality Command`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `CardinalityCommand` 作为 `ConstraintFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `CardinalityCommand` as the responsibility boundary of `ConstraintFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param tenantId 记录组件 `tenantId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantId` carries constructor data whose meaning is defined by the record contract.
     * @param roleId 记录组件 `roleId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `roleId` carries constructor data whose meaning is defined by the record contract.
     * @param scopeType 记录组件 `scopeType` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `scopeType` carries constructor data whose meaning is defined by the record contract.
     * @param maximumActive 记录组件 `maximumActive` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `maximumActive` carries constructor data whose meaning is defined by the record contract.
     * @param validFrom 记录组件 `validFrom` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `validFrom` carries constructor data whose meaning is defined by the record contract.
     * @param validTo 记录组件 `validTo` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `validTo` carries constructor data whose meaning is defined by the record contract.
     * @param expectedVersion 记录组件 `expectedVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `expectedVersion` carries constructor data whose meaning is defined by the record contract.
     * @param actorId 记录组件 `actorId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `actorId` carries constructor data whose meaning is defined by the record contract.
     */
    public record CardinalityCommand(
            /**
             * 字段 `tenantId` 表示 `CardinalityCommand` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `CardinalityCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `CardinalityCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `CardinalityCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String tenantId,
            /**
             * 字段 `roleId` 表示 `CardinalityCommand` 中与 `role Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `roleId` stores the `role Id`-related state, dependency, configuration, or result of `CardinalityCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `roleId` 时应保持 `CardinalityCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `roleId`, preserve `CardinalityCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String roleId,
            /**
             * 字段 `scopeType` 表示 `CardinalityCommand` 中与 `scope Type` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `scopeType` stores the `scope Type`-related state, dependency, configuration, or result of `CardinalityCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `scopeType` 时应保持 `CardinalityCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `scopeType`, preserve `CardinalityCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String scopeType,
            /**
             * 字段 `maximumActive` 表示 `CardinalityCommand` 中与 `maximum Active` 相关的状态、依赖、配置或结果（声明类型 `int`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `maximumActive` stores the `maximum Active`-related state, dependency, configuration, or result of `CardinalityCommand` (declared type `int`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `maximumActive` 时应保持 `CardinalityCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `maximumActive`, preserve `CardinalityCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            int maximumActive,
            /**
             * 字段 `validFrom` 表示 `CardinalityCommand` 中与 `valid From` 相关的状态、依赖、配置或结果（声明类型 `java.time.Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `validFrom` stores the `valid From`-related state, dependency, configuration, or result of `CardinalityCommand` (declared type `java.time.Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `validFrom` 时应保持 `CardinalityCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `validFrom`, preserve `CardinalityCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            java.time.Instant validFrom,
            /**
             * 字段 `validTo` 表示 `CardinalityCommand` 中与 `valid To` 相关的状态、依赖、配置或结果（声明类型 `java.time.Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `validTo` stores the `valid To`-related state, dependency, configuration, or result of `CardinalityCommand` (declared type `java.time.Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `validTo` 时应保持 `CardinalityCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `validTo`, preserve `CardinalityCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            java.time.Instant validTo,
            /**
             * 字段 `expectedVersion` 表示 `CardinalityCommand` 中与 `expected Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `expectedVersion` stores the `expected Version`-related state, dependency, configuration, or result of `CardinalityCommand` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `expectedVersion` 时应保持 `CardinalityCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `expectedVersion`, preserve `CardinalityCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            long expectedVersion,
            /**
             * 字段 `actorId` 表示 `CardinalityCommand` 中与 `actor Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `actorId` stores the `actor Id`-related state, dependency, configuration, or result of `CardinalityCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `actorId` 时应保持 `CardinalityCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `actorId`, preserve `CardinalityCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String actorId
    ) {
    }

    /**
     * 类型 `DataRuleCommand` 位于 `ConstraintFacade` 内，是记录类型，用于承载 `Data Rule Command` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `DataRuleCommand` is a record inside `ConstraintFacade` and carries the responsibility, state, or contract for `Data Rule Command`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `DataRuleCommand` 作为 `ConstraintFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `DataRuleCommand` as the responsibility boundary of `ConstraintFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param tenantId 记录组件 `tenantId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantId` carries constructor data whose meaning is defined by the record contract.
     * @param ruleId 记录组件 `ruleId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `ruleId` carries constructor data whose meaning is defined by the record contract.
     * @param applicationId 记录组件 `applicationId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `applicationId` carries constructor data whose meaning is defined by the record contract.
     * @param roleId 记录组件 `roleId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `roleId` carries constructor data whose meaning is defined by the record contract.
     * @param permissionId 记录组件 `permissionId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `permissionId` carries constructor data whose meaning is defined by the record contract.
     * @param scopeType 记录组件 `scopeType` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `scopeType` carries constructor data whose meaning is defined by the record contract.
     * @param directorySnapshotVersion 记录组件 `directorySnapshotVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `directorySnapshotVersion` carries constructor data whose meaning is defined by the record contract.
     * @param references 记录组件 `references` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `references` carries constructor data whose meaning is defined by the record contract.
     * @param validFrom 记录组件 `validFrom` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `validFrom` carries constructor data whose meaning is defined by the record contract.
     * @param validTo 记录组件 `validTo` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `validTo` carries constructor data whose meaning is defined by the record contract.
     * @param expectedVersion 记录组件 `expectedVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `expectedVersion` carries constructor data whose meaning is defined by the record contract.
     * @param actorId 记录组件 `actorId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `actorId` carries constructor data whose meaning is defined by the record contract.
     */
    public record DataRuleCommand(
            /**
             * 字段 `tenantId` 表示 `DataRuleCommand` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `DataRuleCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `DataRuleCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `DataRuleCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String tenantId,
            /**
             * 字段 `ruleId` 表示 `DataRuleCommand` 中与 `rule Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `ruleId` stores the `rule Id`-related state, dependency, configuration, or result of `DataRuleCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `ruleId` 时应保持 `DataRuleCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `ruleId`, preserve `DataRuleCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String ruleId,
            /**
             * 字段 `applicationId` 表示 `DataRuleCommand` 中与 `application Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `applicationId` stores the `application Id`-related state, dependency, configuration, or result of `DataRuleCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `applicationId` 时应保持 `DataRuleCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `applicationId`, preserve `DataRuleCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String applicationId,
            /**
             * 字段 `roleId` 表示 `DataRuleCommand` 中与 `role Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `roleId` stores the `role Id`-related state, dependency, configuration, or result of `DataRuleCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `roleId` 时应保持 `DataRuleCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `roleId`, preserve `DataRuleCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String roleId,
            /**
             * 字段 `permissionId` 表示 `DataRuleCommand` 中与 `permission Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `permissionId` stores the `permission Id`-related state, dependency, configuration, or result of `DataRuleCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `permissionId` 时应保持 `DataRuleCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `permissionId`, preserve `DataRuleCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String permissionId,
            /**
             * 字段 `scopeType` 表示 `DataRuleCommand` 中与 `scope Type` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `scopeType` stores the `scope Type`-related state, dependency, configuration, or result of `DataRuleCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `scopeType` 时应保持 `DataRuleCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `scopeType`, preserve `DataRuleCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String scopeType,
            /**
             * 字段 `directorySnapshotVersion` 表示 `DataRuleCommand` 中与 `directory Snapshot Version` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `directorySnapshotVersion` stores the `directory Snapshot Version`-related state, dependency, configuration, or result of `DataRuleCommand` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `directorySnapshotVersion` 时应保持 `DataRuleCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `directorySnapshotVersion`, preserve `DataRuleCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            Long directorySnapshotVersion,
            /**
             * 字段 `references` 表示 `DataRuleCommand` 中与 `references` 相关的状态、依赖、配置或结果（声明类型 `List&lt;RuleReference&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `references` stores the `references`-related state, dependency, configuration, or result of `DataRuleCommand` (declared type `List&lt;RuleReference&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `references` 时应保持 `DataRuleCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `references`, preserve `DataRuleCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            List<RuleReference> references,
            /**
             * 字段 `validFrom` 表示 `DataRuleCommand` 中与 `valid From` 相关的状态、依赖、配置或结果（声明类型 `java.time.Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `validFrom` stores the `valid From`-related state, dependency, configuration, or result of `DataRuleCommand` (declared type `java.time.Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `validFrom` 时应保持 `DataRuleCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `validFrom`, preserve `DataRuleCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            java.time.Instant validFrom,
            /**
             * 字段 `validTo` 表示 `DataRuleCommand` 中与 `valid To` 相关的状态、依赖、配置或结果（声明类型 `java.time.Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `validTo` stores the `valid To`-related state, dependency, configuration, or result of `DataRuleCommand` (declared type `java.time.Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `validTo` 时应保持 `DataRuleCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `validTo`, preserve `DataRuleCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            java.time.Instant validTo,
            /**
             * 字段 `expectedVersion` 表示 `DataRuleCommand` 中与 `expected Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `expectedVersion` stores the `expected Version`-related state, dependency, configuration, or result of `DataRuleCommand` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `expectedVersion` 时应保持 `DataRuleCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `expectedVersion`, preserve `DataRuleCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            long expectedVersion,
            /**
             * 字段 `actorId` 表示 `DataRuleCommand` 中与 `actor Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `actorId` stores the `actor Id`-related state, dependency, configuration, or result of `DataRuleCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `actorId` 时应保持 `DataRuleCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `actorId`, preserve `DataRuleCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String actorId
    ) {

        /**
         * 构造器 `DataRuleCommand` 用于创建并初始化 `DataRuleCommand` 实例，建立该类型后续方法所依赖的状态和不变量。
         * Constructor `DataRuleCommand` creates and initializes `DataRuleCommand`, establishing the state and invariants required by subsequent operations.
         *
         * 用法：通过 `DataRuleCommand` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `DataRuleCommand`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param ruleId 输入参数 `ruleId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param applicationId 输入参数 `applicationId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param roleId 输入参数 `roleId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param permissionId 输入参数 `permissionId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param scopeType 输入参数 `scopeType`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param directorySnapshotVersion 输入参数 `directorySnapshotVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param references 输入参数 `references`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param validFrom 输入参数 `validFrom`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param validTo 输入参数 `validTo`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param expectedVersion 输入参数 `expectedVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public DataRuleCommand {
            references = List.copyOf(references);
        }
    }

    /**
     * 类型 `FieldRuleCommand` 位于 `ConstraintFacade` 内，是记录类型，用于承载 `Field Rule Command` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `FieldRuleCommand` is a record inside `ConstraintFacade` and carries the responsibility, state, or contract for `Field Rule Command`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `FieldRuleCommand` 作为 `ConstraintFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `FieldRuleCommand` as the responsibility boundary of `ConstraintFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param tenantId 记录组件 `tenantId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantId` carries constructor data whose meaning is defined by the record contract.
     * @param ruleId 记录组件 `ruleId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `ruleId` carries constructor data whose meaning is defined by the record contract.
     * @param applicationId 记录组件 `applicationId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `applicationId` carries constructor data whose meaning is defined by the record contract.
     * @param roleId 记录组件 `roleId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `roleId` carries constructor data whose meaning is defined by the record contract.
     * @param permissionId 记录组件 `permissionId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `permissionId` carries constructor data whose meaning is defined by the record contract.
     * @param fieldDefinitionId 记录组件 `fieldDefinitionId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `fieldDefinitionId` carries constructor data whose meaning is defined by the record contract.
     * @param accessLevel 记录组件 `accessLevel` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `accessLevel` carries constructor data whose meaning is defined by the record contract.
     * @param validFrom 记录组件 `validFrom` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `validFrom` carries constructor data whose meaning is defined by the record contract.
     * @param validTo 记录组件 `validTo` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `validTo` carries constructor data whose meaning is defined by the record contract.
     * @param expectedVersion 记录组件 `expectedVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `expectedVersion` carries constructor data whose meaning is defined by the record contract.
     * @param actorId 记录组件 `actorId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `actorId` carries constructor data whose meaning is defined by the record contract.
     */
    public record FieldRuleCommand(
            /**
             * 字段 `tenantId` 表示 `FieldRuleCommand` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `FieldRuleCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `FieldRuleCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `FieldRuleCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String tenantId,
            /**
             * 字段 `ruleId` 表示 `FieldRuleCommand` 中与 `rule Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `ruleId` stores the `rule Id`-related state, dependency, configuration, or result of `FieldRuleCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `ruleId` 时应保持 `FieldRuleCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `ruleId`, preserve `FieldRuleCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String ruleId,
            /**
             * 字段 `applicationId` 表示 `FieldRuleCommand` 中与 `application Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `applicationId` stores the `application Id`-related state, dependency, configuration, or result of `FieldRuleCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `applicationId` 时应保持 `FieldRuleCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `applicationId`, preserve `FieldRuleCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String applicationId,
            /**
             * 字段 `roleId` 表示 `FieldRuleCommand` 中与 `role Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `roleId` stores the `role Id`-related state, dependency, configuration, or result of `FieldRuleCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `roleId` 时应保持 `FieldRuleCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `roleId`, preserve `FieldRuleCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String roleId,
            /**
             * 字段 `permissionId` 表示 `FieldRuleCommand` 中与 `permission Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `permissionId` stores the `permission Id`-related state, dependency, configuration, or result of `FieldRuleCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `permissionId` 时应保持 `FieldRuleCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `permissionId`, preserve `FieldRuleCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String permissionId,
            /**
             * 字段 `fieldDefinitionId` 表示 `FieldRuleCommand` 中与 `field Definition Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `fieldDefinitionId` stores the `field Definition Id`-related state, dependency, configuration, or result of `FieldRuleCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `fieldDefinitionId` 时应保持 `FieldRuleCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `fieldDefinitionId`, preserve `FieldRuleCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String fieldDefinitionId,
            /**
             * 字段 `accessLevel` 表示 `FieldRuleCommand` 中与 `access Level` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `accessLevel` stores the `access Level`-related state, dependency, configuration, or result of `FieldRuleCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `accessLevel` 时应保持 `FieldRuleCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `accessLevel`, preserve `FieldRuleCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String accessLevel,
            /**
             * 字段 `validFrom` 表示 `FieldRuleCommand` 中与 `valid From` 相关的状态、依赖、配置或结果（声明类型 `java.time.Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `validFrom` stores the `valid From`-related state, dependency, configuration, or result of `FieldRuleCommand` (declared type `java.time.Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `validFrom` 时应保持 `FieldRuleCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `validFrom`, preserve `FieldRuleCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            java.time.Instant validFrom,
            /**
             * 字段 `validTo` 表示 `FieldRuleCommand` 中与 `valid To` 相关的状态、依赖、配置或结果（声明类型 `java.time.Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `validTo` stores the `valid To`-related state, dependency, configuration, or result of `FieldRuleCommand` (declared type `java.time.Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `validTo` 时应保持 `FieldRuleCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `validTo`, preserve `FieldRuleCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            java.time.Instant validTo,
            /**
             * 字段 `expectedVersion` 表示 `FieldRuleCommand` 中与 `expected Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `expectedVersion` stores the `expected Version`-related state, dependency, configuration, or result of `FieldRuleCommand` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `expectedVersion` 时应保持 `FieldRuleCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `expectedVersion`, preserve `FieldRuleCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            long expectedVersion,
            /**
             * 字段 `actorId` 表示 `FieldRuleCommand` 中与 `actor Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `actorId` stores the `actor Id`-related state, dependency, configuration, or result of `FieldRuleCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `actorId` 时应保持 `FieldRuleCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `actorId`, preserve `FieldRuleCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String actorId
    ) {
    }

    /**
     * 类型 `OperationSodRuleCommand` 位于 `ConstraintFacade` 内，是记录类型，用于承载 `Operation Sod Rule Command` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `OperationSodRuleCommand` is a record inside `ConstraintFacade` and carries the responsibility, state, or contract for `Operation Sod Rule Command`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `OperationSodRuleCommand` 作为 `ConstraintFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `OperationSodRuleCommand` as the responsibility boundary of `ConstraintFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param tenantId 记录组件 `tenantId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantId` carries constructor data whose meaning is defined by the record contract.
     * @param ruleId 记录组件 `ruleId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `ruleId` carries constructor data whose meaning is defined by the record contract.
     * @param applicationCode 记录组件 `applicationCode` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `applicationCode` carries constructor data whose meaning is defined by the record contract.
     * @param businessResource 记录组件 `businessResource` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `businessResource` carries constructor data whose meaning is defined by the record contract.
     * @param priorActionCode 记录组件 `priorActionCode` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `priorActionCode` carries constructor data whose meaning is defined by the record contract.
     * @param forbiddenLaterActionCode 记录组件 `forbiddenLaterActionCode` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `forbiddenLaterActionCode` carries constructor data whose meaning is defined by the record contract.
     * @param lookbackFrom 记录组件 `lookbackFrom` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `lookbackFrom` carries constructor data whose meaning is defined by the record contract.
     * @param validFrom 记录组件 `validFrom` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `validFrom` carries constructor data whose meaning is defined by the record contract.
     * @param validTo 记录组件 `validTo` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `validTo` carries constructor data whose meaning is defined by the record contract.
     * @param expectedVersion 记录组件 `expectedVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `expectedVersion` carries constructor data whose meaning is defined by the record contract.
     * @param actorId 记录组件 `actorId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `actorId` carries constructor data whose meaning is defined by the record contract.
     */
    public record OperationSodRuleCommand(
            /**
             * 字段 `tenantId` 表示 `OperationSodRuleCommand` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `OperationSodRuleCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `OperationSodRuleCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `OperationSodRuleCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String tenantId,
            /**
             * 字段 `ruleId` 表示 `OperationSodRuleCommand` 中与 `rule Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `ruleId` stores the `rule Id`-related state, dependency, configuration, or result of `OperationSodRuleCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `ruleId` 时应保持 `OperationSodRuleCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `ruleId`, preserve `OperationSodRuleCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String ruleId,
            /**
             * 字段 `applicationCode` 表示 `OperationSodRuleCommand` 中与 `application Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `applicationCode` stores the `application Code`-related state, dependency, configuration, or result of `OperationSodRuleCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `applicationCode` 时应保持 `OperationSodRuleCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `applicationCode`, preserve `OperationSodRuleCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String applicationCode,
            /**
             * 字段 `businessResource` 表示 `OperationSodRuleCommand` 中与 `business Resource` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `businessResource` stores the `business Resource`-related state, dependency, configuration, or result of `OperationSodRuleCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `businessResource` 时应保持 `OperationSodRuleCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `businessResource`, preserve `OperationSodRuleCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String businessResource,
            /**
             * 字段 `priorActionCode` 表示 `OperationSodRuleCommand` 中与 `prior Action Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `priorActionCode` stores the `prior Action Code`-related state, dependency, configuration, or result of `OperationSodRuleCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `priorActionCode` 时应保持 `OperationSodRuleCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `priorActionCode`, preserve `OperationSodRuleCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String priorActionCode,
            /**
             * 字段 `forbiddenLaterActionCode` 表示 `OperationSodRuleCommand` 中与 `forbidden Later Action Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `forbiddenLaterActionCode` stores the `forbidden Later Action Code`-related state, dependency, configuration, or result of `OperationSodRuleCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `forbiddenLaterActionCode` 时应保持 `OperationSodRuleCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `forbiddenLaterActionCode`, preserve `OperationSodRuleCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String forbiddenLaterActionCode,
            /**
             * 字段 `lookbackFrom` 表示 `OperationSodRuleCommand` 中与 `lookback From` 相关的状态、依赖、配置或结果（声明类型 `java.time.Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `lookbackFrom` stores the `lookback From`-related state, dependency, configuration, or result of `OperationSodRuleCommand` (declared type `java.time.Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `lookbackFrom` 时应保持 `OperationSodRuleCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `lookbackFrom`, preserve `OperationSodRuleCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            java.time.Instant lookbackFrom,
            /**
             * 字段 `validFrom` 表示 `OperationSodRuleCommand` 中与 `valid From` 相关的状态、依赖、配置或结果（声明类型 `java.time.Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `validFrom` stores the `valid From`-related state, dependency, configuration, or result of `OperationSodRuleCommand` (declared type `java.time.Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `validFrom` 时应保持 `OperationSodRuleCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `validFrom`, preserve `OperationSodRuleCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            java.time.Instant validFrom,
            /**
             * 字段 `validTo` 表示 `OperationSodRuleCommand` 中与 `valid To` 相关的状态、依赖、配置或结果（声明类型 `java.time.Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `validTo` stores the `valid To`-related state, dependency, configuration, or result of `OperationSodRuleCommand` (declared type `java.time.Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `validTo` 时应保持 `OperationSodRuleCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `validTo`, preserve `OperationSodRuleCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            java.time.Instant validTo,
            /**
             * 字段 `expectedVersion` 表示 `OperationSodRuleCommand` 中与 `expected Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `expectedVersion` stores the `expected Version`-related state, dependency, configuration, or result of `OperationSodRuleCommand` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `expectedVersion` 时应保持 `OperationSodRuleCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `expectedVersion`, preserve `OperationSodRuleCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            long expectedVersion,
            /**
             * 字段 `actorId` 表示 `OperationSodRuleCommand` 中与 `actor Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `actorId` stores the `actor Id`-related state, dependency, configuration, or result of `OperationSodRuleCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `actorId` 时应保持 `OperationSodRuleCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `actorId`, preserve `OperationSodRuleCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String actorId
    ) {
    }

    /**
     * 类型 `RuleReference` 位于 `ConstraintFacade` 内，是记录类型，用于承载 `Rule Reference` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `RuleReference` is a record inside `ConstraintFacade` and carries the responsibility, state, or contract for `Rule Reference`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `RuleReference` 作为 `ConstraintFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `RuleReference` as the responsibility boundary of `ConstraintFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param referenceType 记录组件 `referenceType` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `referenceType` carries constructor data whose meaning is defined by the record contract.
     * @param referenceId 记录组件 `referenceId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `referenceId` carries constructor data whose meaning is defined by the record contract.
     */
    public record RuleReference(/**
 * 字段 `referenceType` 表示 `RuleReference` 中与 `reference Type` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `referenceType` stores the `reference Type`-related state, dependency, configuration, or result of `RuleReference` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `referenceType` 时应保持 `RuleReference` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `referenceType`, preserve `RuleReference`'s lifecycle, immutability, and thread-safety constraints.
 */ String referenceType, /**
 * 字段 `referenceId` 表示 `RuleReference` 中与 `reference Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `referenceId` stores the `reference Id`-related state, dependency, configuration, or result of `RuleReference` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `referenceId` 时应保持 `RuleReference` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `referenceId`, preserve `RuleReference`'s lifecycle, immutability, and thread-safety constraints.
 */ String referenceId) {
    }

    /**
     * 类型 `MutationResult` 位于 `ConstraintFacade` 内，是记录类型，用于承载 `Mutation Result` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `MutationResult` is a record inside `ConstraintFacade` and carries the responsibility, state, or contract for `Mutation Result`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `MutationResult` 作为 `ConstraintFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `MutationResult` as the responsibility boundary of `ConstraintFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param resourceId 记录组件 `resourceId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `resourceId` carries constructor data whose meaning is defined by the record contract.
     * @param policyVersion 记录组件 `policyVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `policyVersion` carries constructor data whose meaning is defined by the record contract.
     * @param propagationId 记录组件 `propagationId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `propagationId` carries constructor data whose meaning is defined by the record contract.
     * @param propagationPending 记录组件 `propagationPending` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `propagationPending` carries constructor data whose meaning is defined by the record contract.
     */
    public record MutationResult(
            /**
             * 字段 `resourceId` 表示 `MutationResult` 中与 `resource Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `resourceId` stores the `resource Id`-related state, dependency, configuration, or result of `MutationResult` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `resourceId` 时应保持 `MutationResult` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `resourceId`, preserve `MutationResult`'s lifecycle, immutability, and thread-safety constraints.
             */
            String resourceId,
            /**
             * 字段 `policyVersion` 表示 `MutationResult` 中与 `policy Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `policyVersion` stores the `policy Version`-related state, dependency, configuration, or result of `MutationResult` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `policyVersion` 时应保持 `MutationResult` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `policyVersion`, preserve `MutationResult`'s lifecycle, immutability, and thread-safety constraints.
             */
            long policyVersion,
            /**
             * 字段 `propagationId` 表示 `MutationResult` 中与 `propagation Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `propagationId` stores the `propagation Id`-related state, dependency, configuration, or result of `MutationResult` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `propagationId` 时应保持 `MutationResult` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `propagationId`, preserve `MutationResult`'s lifecycle, immutability, and thread-safety constraints.
             */
            String propagationId,
            /**
             * 字段 `propagationPending` 表示 `MutationResult` 中与 `propagation Pending` 相关的状态、依赖、配置或结果（声明类型 `boolean`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `propagationPending` stores the `propagation Pending`-related state, dependency, configuration, or result of `MutationResult` (declared type `boolean`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `propagationPending` 时应保持 `MutationResult` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `propagationPending`, preserve `MutationResult`'s lifecycle, immutability, and thread-safety constraints.
             */
            boolean propagationPending
    ) {
    }

    /**
     * 类型 `SodView` 位于 `ConstraintFacade` 内，是记录类型，用于承载 `Sod View` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `SodView` is a record inside `ConstraintFacade` and carries the responsibility, state, or contract for `Sod View`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `SodView` 作为 `ConstraintFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `SodView` as the responsibility boundary of `ConstraintFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param setId 记录组件 `setId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `setId` carries constructor data whose meaning is defined by the record contract.
     * @param setCode 记录组件 `setCode` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `setCode` carries constructor data whose meaning is defined by the record contract.
     * @param constraintType 记录组件 `constraintType` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `constraintType` carries constructor data whose meaning is defined by the record contract.
     * @param applicationId 记录组件 `applicationId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `applicationId` carries constructor data whose meaning is defined by the record contract.
     * @param maximumActiveRoles 记录组件 `maximumActiveRoles` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `maximumActiveRoles` carries constructor data whose meaning is defined by the record contract.
     * @param roleIds 记录组件 `roleIds` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `roleIds` carries constructor data whose meaning is defined by the record contract.
     * @param status 记录组件 `status` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `status` carries constructor data whose meaning is defined by the record contract.
     * @param version 记录组件 `version` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `version` carries constructor data whose meaning is defined by the record contract.
     */
    public record SodView(
            /**
             * 字段 `setId` 表示 `SodView` 中与 `set Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `setId` stores the `set Id`-related state, dependency, configuration, or result of `SodView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `setId` 时应保持 `SodView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `setId`, preserve `SodView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String setId,
            /**
             * 字段 `setCode` 表示 `SodView` 中与 `set Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `setCode` stores the `set Code`-related state, dependency, configuration, or result of `SodView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `setCode` 时应保持 `SodView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `setCode`, preserve `SodView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String setCode,
            /**
             * 字段 `constraintType` 表示 `SodView` 中与 `constraint Type` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `constraintType` stores the `constraint Type`-related state, dependency, configuration, or result of `SodView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `constraintType` 时应保持 `SodView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `constraintType`, preserve `SodView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String constraintType,
            /**
             * 字段 `applicationId` 表示 `SodView` 中与 `application Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `applicationId` stores the `application Id`-related state, dependency, configuration, or result of `SodView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `applicationId` 时应保持 `SodView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `applicationId`, preserve `SodView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String applicationId,
            /**
             * 字段 `maximumActiveRoles` 表示 `SodView` 中与 `maximum Active Roles` 相关的状态、依赖、配置或结果（声明类型 `int`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `maximumActiveRoles` stores the `maximum Active Roles`-related state, dependency, configuration, or result of `SodView` (declared type `int`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `maximumActiveRoles` 时应保持 `SodView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `maximumActiveRoles`, preserve `SodView`'s lifecycle, immutability, and thread-safety constraints.
             */
            int maximumActiveRoles,
            /**
             * 字段 `roleIds` 表示 `SodView` 中与 `role Ids` 相关的状态、依赖、配置或结果（声明类型 `List&lt;String&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `roleIds` stores the `role Ids`-related state, dependency, configuration, or result of `SodView` (declared type `List&lt;String&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `roleIds` 时应保持 `SodView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `roleIds`, preserve `SodView`'s lifecycle, immutability, and thread-safety constraints.
             */
            List<String> roleIds,
            /**
             * 字段 `status` 表示 `SodView` 中与 `status` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `status` stores the `status`-related state, dependency, configuration, or result of `SodView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `status` 时应保持 `SodView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `status`, preserve `SodView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String status,
            /**
             * 字段 `version` 表示 `SodView` 中与 `version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `version` stores the `version`-related state, dependency, configuration, or result of `SodView` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `version` 时应保持 `SodView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `version`, preserve `SodView`'s lifecycle, immutability, and thread-safety constraints.
             */
            long version
    ) {

        /**
         * 构造器 `SodView` 用于创建并初始化 `SodView` 实例，建立该类型后续方法所依赖的状态和不变量。
         * Constructor `SodView` creates and initializes `SodView`, establishing the state and invariants required by subsequent operations.
         *
         * 用法：通过 `SodView` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `SodView`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         *
         * @param setId 输入参数 `setId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param setCode 输入参数 `setCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param constraintType 输入参数 `constraintType`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param applicationId 输入参数 `applicationId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param maximumActiveRoles 输入参数 `maximumActiveRoles`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param roleIds 输入参数 `roleIds`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param status 输入参数 `status`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param version 输入参数 `version`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public SodView {
            roleIds = List.copyOf(roleIds);
        }
    }

    /**
     * 类型 `DataRuleView` 位于 `ConstraintFacade` 内，是记录类型，用于承载 `Data Rule View` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `DataRuleView` is a record inside `ConstraintFacade` and carries the responsibility, state, or contract for `Data Rule View`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `DataRuleView` 作为 `ConstraintFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `DataRuleView` as the responsibility boundary of `ConstraintFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param ruleId 记录组件 `ruleId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `ruleId` carries constructor data whose meaning is defined by the record contract.
     * @param applicationId 记录组件 `applicationId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `applicationId` carries constructor data whose meaning is defined by the record contract.
     * @param roleId 记录组件 `roleId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `roleId` carries constructor data whose meaning is defined by the record contract.
     * @param permissionId 记录组件 `permissionId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `permissionId` carries constructor data whose meaning is defined by the record contract.
     * @param scopeType 记录组件 `scopeType` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `scopeType` carries constructor data whose meaning is defined by the record contract.
     * @param references 记录组件 `references` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `references` carries constructor data whose meaning is defined by the record contract.
     * @param status 记录组件 `status` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `status` carries constructor data whose meaning is defined by the record contract.
     * @param version 记录组件 `version` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `version` carries constructor data whose meaning is defined by the record contract.
     */
    public record DataRuleView(
            /**
             * 字段 `ruleId` 表示 `DataRuleView` 中与 `rule Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `ruleId` stores the `rule Id`-related state, dependency, configuration, or result of `DataRuleView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `ruleId` 时应保持 `DataRuleView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `ruleId`, preserve `DataRuleView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String ruleId,
            /**
             * 字段 `applicationId` 表示 `DataRuleView` 中与 `application Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `applicationId` stores the `application Id`-related state, dependency, configuration, or result of `DataRuleView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `applicationId` 时应保持 `DataRuleView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `applicationId`, preserve `DataRuleView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String applicationId,
            /**
             * 字段 `roleId` 表示 `DataRuleView` 中与 `role Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `roleId` stores the `role Id`-related state, dependency, configuration, or result of `DataRuleView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `roleId` 时应保持 `DataRuleView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `roleId`, preserve `DataRuleView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String roleId,
            /**
             * 字段 `permissionId` 表示 `DataRuleView` 中与 `permission Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `permissionId` stores the `permission Id`-related state, dependency, configuration, or result of `DataRuleView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `permissionId` 时应保持 `DataRuleView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `permissionId`, preserve `DataRuleView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String permissionId,
            /**
             * 字段 `scopeType` 表示 `DataRuleView` 中与 `scope Type` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `scopeType` stores the `scope Type`-related state, dependency, configuration, or result of `DataRuleView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `scopeType` 时应保持 `DataRuleView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `scopeType`, preserve `DataRuleView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String scopeType,
            /**
             * 字段 `references` 表示 `DataRuleView` 中与 `references` 相关的状态、依赖、配置或结果（声明类型 `List&lt;RuleReference&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `references` stores the `references`-related state, dependency, configuration, or result of `DataRuleView` (declared type `List&lt;RuleReference&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `references` 时应保持 `DataRuleView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `references`, preserve `DataRuleView`'s lifecycle, immutability, and thread-safety constraints.
             */
            List<RuleReference> references,
            /**
             * 字段 `status` 表示 `DataRuleView` 中与 `status` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `status` stores the `status`-related state, dependency, configuration, or result of `DataRuleView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `status` 时应保持 `DataRuleView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `status`, preserve `DataRuleView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String status,
            /**
             * 字段 `version` 表示 `DataRuleView` 中与 `version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `version` stores the `version`-related state, dependency, configuration, or result of `DataRuleView` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `version` 时应保持 `DataRuleView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `version`, preserve `DataRuleView`'s lifecycle, immutability, and thread-safety constraints.
             */
            long version
    ) {

        /**
         * 构造器 `DataRuleView` 用于创建并初始化 `DataRuleView` 实例，建立该类型后续方法所依赖的状态和不变量。
         * Constructor `DataRuleView` creates and initializes `DataRuleView`, establishing the state and invariants required by subsequent operations.
         *
         * 用法：通过 `DataRuleView` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `DataRuleView`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         *
         * @param ruleId 输入参数 `ruleId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param applicationId 输入参数 `applicationId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param roleId 输入参数 `roleId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param permissionId 输入参数 `permissionId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param scopeType 输入参数 `scopeType`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param references 输入参数 `references`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param status 输入参数 `status`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param version 输入参数 `version`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public DataRuleView {
            references = List.copyOf(references);
        }
    }

    /**
     * 类型 `FieldRuleView` 位于 `ConstraintFacade` 内，是记录类型，用于承载 `Field Rule View` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `FieldRuleView` is a record inside `ConstraintFacade` and carries the responsibility, state, or contract for `Field Rule View`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `FieldRuleView` 作为 `ConstraintFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `FieldRuleView` as the responsibility boundary of `ConstraintFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param ruleId 记录组件 `ruleId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `ruleId` carries constructor data whose meaning is defined by the record contract.
     * @param applicationId 记录组件 `applicationId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `applicationId` carries constructor data whose meaning is defined by the record contract.
     * @param roleId 记录组件 `roleId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `roleId` carries constructor data whose meaning is defined by the record contract.
     * @param permissionId 记录组件 `permissionId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `permissionId` carries constructor data whose meaning is defined by the record contract.
     * @param fieldDefinitionId 记录组件 `fieldDefinitionId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `fieldDefinitionId` carries constructor data whose meaning is defined by the record contract.
     * @param accessLevel 记录组件 `accessLevel` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `accessLevel` carries constructor data whose meaning is defined by the record contract.
     * @param status 记录组件 `status` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `status` carries constructor data whose meaning is defined by the record contract.
     * @param version 记录组件 `version` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `version` carries constructor data whose meaning is defined by the record contract.
     */
    public record FieldRuleView(
            /**
             * 字段 `ruleId` 表示 `FieldRuleView` 中与 `rule Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `ruleId` stores the `rule Id`-related state, dependency, configuration, or result of `FieldRuleView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `ruleId` 时应保持 `FieldRuleView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `ruleId`, preserve `FieldRuleView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String ruleId,
            /**
             * 字段 `applicationId` 表示 `FieldRuleView` 中与 `application Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `applicationId` stores the `application Id`-related state, dependency, configuration, or result of `FieldRuleView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `applicationId` 时应保持 `FieldRuleView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `applicationId`, preserve `FieldRuleView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String applicationId,
            /**
             * 字段 `roleId` 表示 `FieldRuleView` 中与 `role Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `roleId` stores the `role Id`-related state, dependency, configuration, or result of `FieldRuleView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `roleId` 时应保持 `FieldRuleView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `roleId`, preserve `FieldRuleView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String roleId,
            /**
             * 字段 `permissionId` 表示 `FieldRuleView` 中与 `permission Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `permissionId` stores the `permission Id`-related state, dependency, configuration, or result of `FieldRuleView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `permissionId` 时应保持 `FieldRuleView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `permissionId`, preserve `FieldRuleView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String permissionId,
            /**
             * 字段 `fieldDefinitionId` 表示 `FieldRuleView` 中与 `field Definition Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `fieldDefinitionId` stores the `field Definition Id`-related state, dependency, configuration, or result of `FieldRuleView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `fieldDefinitionId` 时应保持 `FieldRuleView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `fieldDefinitionId`, preserve `FieldRuleView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String fieldDefinitionId,
            /**
             * 字段 `accessLevel` 表示 `FieldRuleView` 中与 `access Level` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `accessLevel` stores the `access Level`-related state, dependency, configuration, or result of `FieldRuleView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `accessLevel` 时应保持 `FieldRuleView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `accessLevel`, preserve `FieldRuleView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String accessLevel,
            /**
             * 字段 `status` 表示 `FieldRuleView` 中与 `status` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `status` stores the `status`-related state, dependency, configuration, or result of `FieldRuleView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `status` 时应保持 `FieldRuleView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `status`, preserve `FieldRuleView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String status,
            /**
             * 字段 `version` 表示 `FieldRuleView` 中与 `version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `version` stores the `version`-related state, dependency, configuration, or result of `FieldRuleView` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `version` 时应保持 `FieldRuleView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `version`, preserve `FieldRuleView`'s lifecycle, immutability, and thread-safety constraints.
             */
            long version
    ) {
    }

    /**
     * 类型 `OperationSodRuleView` 位于 `ConstraintFacade` 内，是记录类型，用于承载 `Operation Sod Rule View` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `OperationSodRuleView` is a record inside `ConstraintFacade` and carries the responsibility, state, or contract for `Operation Sod Rule View`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `OperationSodRuleView` 作为 `ConstraintFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `OperationSodRuleView` as the responsibility boundary of `ConstraintFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param ruleId 记录组件 `ruleId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `ruleId` carries constructor data whose meaning is defined by the record contract.
     * @param applicationCode 记录组件 `applicationCode` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `applicationCode` carries constructor data whose meaning is defined by the record contract.
     * @param businessResource 记录组件 `businessResource` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `businessResource` carries constructor data whose meaning is defined by the record contract.
     * @param priorActionCode 记录组件 `priorActionCode` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `priorActionCode` carries constructor data whose meaning is defined by the record contract.
     * @param forbiddenLaterActionCode 记录组件 `forbiddenLaterActionCode` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `forbiddenLaterActionCode` carries constructor data whose meaning is defined by the record contract.
     * @param status 记录组件 `status` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `status` carries constructor data whose meaning is defined by the record contract.
     * @param version 记录组件 `version` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `version` carries constructor data whose meaning is defined by the record contract.
     */
    public record OperationSodRuleView(
            /**
             * 字段 `ruleId` 表示 `OperationSodRuleView` 中与 `rule Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `ruleId` stores the `rule Id`-related state, dependency, configuration, or result of `OperationSodRuleView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `ruleId` 时应保持 `OperationSodRuleView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `ruleId`, preserve `OperationSodRuleView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String ruleId,
            /**
             * 字段 `applicationCode` 表示 `OperationSodRuleView` 中与 `application Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `applicationCode` stores the `application Code`-related state, dependency, configuration, or result of `OperationSodRuleView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `applicationCode` 时应保持 `OperationSodRuleView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `applicationCode`, preserve `OperationSodRuleView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String applicationCode,
            /**
             * 字段 `businessResource` 表示 `OperationSodRuleView` 中与 `business Resource` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `businessResource` stores the `business Resource`-related state, dependency, configuration, or result of `OperationSodRuleView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `businessResource` 时应保持 `OperationSodRuleView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `businessResource`, preserve `OperationSodRuleView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String businessResource,
            /**
             * 字段 `priorActionCode` 表示 `OperationSodRuleView` 中与 `prior Action Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `priorActionCode` stores the `prior Action Code`-related state, dependency, configuration, or result of `OperationSodRuleView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `priorActionCode` 时应保持 `OperationSodRuleView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `priorActionCode`, preserve `OperationSodRuleView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String priorActionCode,
            /**
             * 字段 `forbiddenLaterActionCode` 表示 `OperationSodRuleView` 中与 `forbidden Later Action Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `forbiddenLaterActionCode` stores the `forbidden Later Action Code`-related state, dependency, configuration, or result of `OperationSodRuleView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `forbiddenLaterActionCode` 时应保持 `OperationSodRuleView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `forbiddenLaterActionCode`, preserve `OperationSodRuleView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String forbiddenLaterActionCode,
            /**
             * 字段 `status` 表示 `OperationSodRuleView` 中与 `status` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `status` stores the `status`-related state, dependency, configuration, or result of `OperationSodRuleView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `status` 时应保持 `OperationSodRuleView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `status`, preserve `OperationSodRuleView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String status,
            /**
             * 字段 `version` 表示 `OperationSodRuleView` 中与 `version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `version` stores the `version`-related state, dependency, configuration, or result of `OperationSodRuleView` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `version` 时应保持 `OperationSodRuleView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `version`, preserve `OperationSodRuleView`'s lifecycle, immutability, and thread-safety constraints.
             */
            long version
    ) {
    }

    /**
     * 类型 `ConstraintType` 位于 `ConstraintFacade` 内，是枚举，用于承载 `Constraint Type` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `ConstraintType` is an enum inside `ConstraintFacade` and carries the responsibility, state, or contract for `Constraint Type`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `ConstraintType` 作为 `ConstraintFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `ConstraintType` as the responsibility boundary of `ConstraintFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public enum ConstraintType {
        /**
         * 字段 `SSD` 表示 `ConstraintType` 中与 `SSD` 相关的状态、依赖、配置或结果（声明类型 `ConstraintType`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `SSD` stores the `SSD`-related state, dependency, configuration, or result of `ConstraintType` (declared type `ConstraintType`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `SSD` 时应保持 `ConstraintType` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `SSD`, preserve `ConstraintType`'s lifecycle, immutability, and thread-safety constraints.
         */
        SSD,
        /**
         * 字段 `DSD` 表示 `ConstraintType` 中与 `DSD` 相关的状态、依赖、配置或结果（声明类型 `ConstraintType`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `DSD` stores the `DSD`-related state, dependency, configuration, or result of `ConstraintType` (declared type `ConstraintType`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `DSD` 时应保持 `ConstraintType` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `DSD`, preserve `ConstraintType`'s lifecycle, immutability, and thread-safety constraints.
         */
        DSD
    }
}
