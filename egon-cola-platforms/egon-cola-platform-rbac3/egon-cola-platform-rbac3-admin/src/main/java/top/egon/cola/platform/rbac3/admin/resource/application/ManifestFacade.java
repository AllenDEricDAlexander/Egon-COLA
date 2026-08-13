package top.egon.cola.platform.rbac3.admin.resource.application;

import top.egon.cola.platform.rbac3.contract.manifest.ManifestResource;
import top.egon.cola.platform.rbac3.contract.manifest.ResourceManifest;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.time.Instant;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * 类型 `ManifestFacade` 位于当前包内，是类型，用于承载 `Manifest Facade` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `ManifestFacade` is a type in its package and carries the responsibility, state, or contract for `Manifest Facade`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 * Validates immutable build manifests and coordinates activation propagation.
 */
public final class ManifestFacade {

    /**
     * 字段 `SUPPORTED_SCHEMA_VERSION` 表示 `ManifestFacade` 中与 `SUPPORTED SCHEMA VERSION` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `SUPPORTED_SCHEMA_VERSION` stores the `SUPPORTED SCHEMA VERSION`-related state, dependency, configuration, or result of `ManifestFacade` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `SUPPORTED_SCHEMA_VERSION` 时应保持 `ManifestFacade` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `SUPPORTED_SCHEMA_VERSION`, preserve `ManifestFacade`'s lifecycle, immutability, and thread-safety constraints.
     */
    private static final String SUPPORTED_SCHEMA_VERSION = "1";

    /**
     * 字段 `manifestStore` 表示 `ManifestFacade` 中与 `manifest Store` 相关的状态、依赖、配置或结果（声明类型 `ManifestStore`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `manifestStore` stores the `manifest Store`-related state, dependency, configuration, or result of `ManifestFacade` (declared type `ManifestStore`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `manifestStore` 时应保持 `ManifestFacade` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `manifestStore`, preserve `ManifestFacade`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final ManifestStore manifestStore;
    /**
     * 字段 `componentKeyRegistry` 表示 `ManifestFacade` 中与 `component Key Registry` 相关的状态、依赖、配置或结果（声明类型 `ComponentKeyRegistry`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `componentKeyRegistry` stores the `component Key Registry`-related state, dependency, configuration, or result of `ManifestFacade` (declared type `ComponentKeyRegistry`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `componentKeyRegistry` 时应保持 `ManifestFacade` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `componentKeyRegistry`, preserve `ManifestFacade`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final ComponentKeyRegistry componentKeyRegistry;

    /**
     * 构造器 `ManifestFacade` 用于创建并初始化 `ManifestFacade` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `ManifestFacade` creates and initializes `ManifestFacade`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `ManifestFacade` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `ManifestFacade`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param manifestStore 输入参数 `manifestStore`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param componentKeyRegistry 输入参数 `componentKeyRegistry`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public ManifestFacade(
            ManifestStore manifestStore,
            ComponentKeyRegistry componentKeyRegistry) {
        this.manifestStore = Objects.requireNonNull(manifestStore, "manifestStore");
        this.componentKeyRegistry = Objects.requireNonNull(
                componentKeyRegistry, "componentKeyRegistry");
    }

    /**
     * 方法 `submit` 按照 `ManifestFacade` 的职责处理输入，完成 `submit` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `submit` processes its inputs according to `ManifestFacade`'s responsibility, performs the `submit` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `submit` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `submit`, then continue the business flow using its result, exception, or side effect.
     *
     * @param command 输入参数 `command`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public SubmissionResult submit(SubmitCommand command) {
        Objects.requireNonNull(command, "command");
        validate(command);
        ResourceManifest manifest = command.manifest();
        Optional<StoredManifest> existing = manifestStore.findByBuild(
                command.tenantId(),
                command.applicationId(),
                manifest.artifactVersion(),
                manifest.buildId());
        if (existing.isPresent()) {
            if (existing.get().checksum().equals(manifest.checksum())) {
                return new SubmissionResult(
                        SubmissionOutcome.IDEMPOTENT, existing.get().manifestId());
            }
            throw new Rbac3RuleViolation("RESOURCE_MANIFEST_CONFLICT");
        }
        StoredManifest stored = new StoredManifest(
                command.tenantId(),
                command.applicationId(),
                command.manifestId(),
                command.definitionSetId(),
                manifest.artifactVersion(),
                manifest.buildId(),
                manifest.manifestVersion(),
                manifest.checksum(),
                manifest);
        manifestStore.insert(stored);
        return new SubmissionResult(SubmissionOutcome.ACCEPTED, stored.manifestId());
    }

    /**
     * 方法 `activate` 按照 `ManifestFacade` 的职责处理输入，完成 `activate` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `activate` processes its inputs according to `ManifestFacade`'s responsibility, performs the `activate` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `activate` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `activate`, then continue the business flow using its result, exception, or side effect.
     *
     * @param command 输入参数 `command`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public ActivationResult activate(ActivateCommand command, Instant now) {
        Objects.requireNonNull(command, "command");
        ManifestStore.ActivationMutation mutation = manifestStore.activate(
                command.tenantId(),
                command.applicationId(),
                command.manifestId(),
                command.expectedApplicationVersion(),
                command.expectedCurrentManifestVersion(),
                command.expectedDefinitionSetId(),
                command.actorId(),
                command.idempotencyKey(),
                command.reason(),
                now);
        return new ActivationResult(
                command.manifestId(),
                mutation.policyVersion(),
                mutation.propagationId(),
                mutation.propagationPending());
    }

    /**
     * 方法 `validate` 按照 `ManifestFacade` 的职责处理输入，完成 `validate` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `validate` processes its inputs according to `ManifestFacade`'s responsibility, performs the `validate` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `validate` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `validate`, then continue the business flow using its result, exception, or side effect.
     *
     * @param command 输入参数 `command`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    private void validate(SubmitCommand command) {
        ResourceManifest manifest = command.manifest();
        if (!SUPPORTED_SCHEMA_VERSION.equals(manifest.schemaVersion())) {
            throw new Rbac3RuleViolation("RESOURCE_MANIFEST_INVALID");
        }
        validateComponentKeys(manifest.routes());
        validateComponentKeys(manifest.actions());
        Set<String> resourceCodes = new HashSet<>();
        validateUniqueCodes(resourceCodes, manifest.apps());
        validateUniqueCodes(resourceCodes, manifest.menus());
        validateUniqueCodes(resourceCodes, manifest.routes());
        validateUniqueCodes(resourceCodes, manifest.actions());
        validateUniqueCodes(resourceCodes, manifest.apis());
        Map<String, ResourceKind> kinds = resourceKinds(manifest);
        validateHierarchy(manifest, kinds);
        validateFields(manifest, kinds.keySet());
        Set<String> operationIds = new HashSet<>();
        for (ManifestResource api : manifest.apis()) {
            if (api.gatewayOperationId() == null
                    || api.gatewayOperationId().length() > 64
                    || api.httpMethod() == null
                    || api.pathPattern() == null
                    || !operationIds.add(api.gatewayOperationId())
                    || api.requiredPermissionCode() == null
                    && !Boolean.TRUE.equals(api.externalAccessible())) {
                throw new Rbac3RuleViolation("RESOURCE_MANIFEST_INVALID");
            }
        }
    }

    /**
     * 方法 `validateComponentKeys` 按照 `ManifestFacade` 的职责处理输入，完成 `validate Component Keys` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `validateComponentKeys` processes its inputs according to `ManifestFacade`'s responsibility, performs the `validate Component Keys` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `validateComponentKeys` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `validateComponentKeys`, then continue the business flow using its result, exception, or side effect.
     *
     * @param resources 输入参数 `resources`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    private void validateComponentKeys(List<ManifestResource> resources) {
        for (ManifestResource resource : resources) {
            if (resource.componentKey() != null
                    && !componentKeyRegistry.known(resource.componentKey())) {
                throw new Rbac3RuleViolation("RESOURCE_MANIFEST_INVALID");
            }
        }
    }

    /**
     * 方法 `validateUniqueCodes` 按照 `ManifestFacade` 的职责处理输入，完成 `validate Unique Codes` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `validateUniqueCodes` processes its inputs according to `ManifestFacade`'s responsibility, performs the `validate Unique Codes` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `validateUniqueCodes` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `validateUniqueCodes`, then continue the business flow using its result, exception, or side effect.
     *
     * @param resourceCodes 输入参数 `resourceCodes`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param resources 输入参数 `resources`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    private void validateUniqueCodes(
            Set<String> resourceCodes,
            List<ManifestResource> resources) {
        for (ManifestResource resource : resources) {
            if (!resource.code().matches("^[a-z][a-z0-9-]{1,127}$")
                    || !resourceCodes.add(resource.code())) {
                throw new Rbac3RuleViolation("RESOURCE_MANIFEST_INVALID");
            }
        }
    }

    /**
     * 方法 `resourceKinds` 按照 `ManifestFacade` 的职责处理输入，完成 `resource Kinds` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `resourceKinds` processes its inputs according to `ManifestFacade`'s responsibility, performs the `resource Kinds` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `resourceKinds` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `resourceKinds`, then continue the business flow using its result, exception, or side effect.
     *
     * @param manifest 输入参数 `manifest`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private Map<String, ResourceKind> resourceKinds(ResourceManifest manifest) {
        Map<String, ResourceKind> result = new LinkedHashMap<>();
        put(result, ResourceKind.APP, manifest.apps());
        put(result, ResourceKind.MENU, manifest.menus());
        put(result, ResourceKind.ROUTE, manifest.routes());
        put(result, ResourceKind.ACTION, manifest.actions());
        put(result, ResourceKind.API, manifest.apis());
        return result;
    }

    /**
     * 方法 `validateHierarchy` 按照 `ManifestFacade` 的职责处理输入，完成 `validate Hierarchy` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `validateHierarchy` processes its inputs according to `ManifestFacade`'s responsibility, performs the `validate Hierarchy` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `validateHierarchy` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `validateHierarchy`, then continue the business flow using its result, exception, or side effect.
     *
     * @param manifest 输入参数 `manifest`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param kinds 输入参数 `kinds`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    private void validateHierarchy(
            ResourceManifest manifest,
            Map<String, ResourceKind> kinds) {
        validateParents(manifest.apps(), kinds);
        validateParents(manifest.menus(), kinds, ResourceKind.APP, ResourceKind.MENU);
        validateParents(manifest.routes(), kinds, ResourceKind.APP, ResourceKind.MENU);
        validateParents(manifest.actions(), kinds, ResourceKind.ROUTE);
        validateParents(manifest.apis(), kinds);
        for (ManifestResource route : manifest.routes()) {
            if (route.path() == null || route.componentKey() == null) {
                throw new Rbac3RuleViolation("RESOURCE_MANIFEST_INVALID");
            }
        }
    }

    /**
     * 方法 `validateParents` 按照 `ManifestFacade` 的职责处理输入，完成 `validate Parents` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `validateParents` processes its inputs according to `ManifestFacade`'s responsibility, performs the `validate Parents` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `validateParents` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `validateParents`, then continue the business flow using its result, exception, or side effect.
     *
     * @param resources 输入参数 `resources`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param kinds 输入参数 `kinds`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param allowedParents 输入参数 `allowedParents`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    private void validateParents(
            List<ManifestResource> resources,
            Map<String, ResourceKind> kinds,
            ResourceKind... allowedParents) {
        Set<ResourceKind> allowed = Set.of(allowedParents);
        for (ManifestResource resource : resources) {
            if (resource.parentCode() == null) {
                if (!allowed.isEmpty()) {
                    throw new Rbac3RuleViolation("RESOURCE_MANIFEST_INVALID");
                }
                continue;
            }
            ResourceKind parent = kinds.get(resource.parentCode());
            if (parent == null || !allowed.contains(parent)) {
                throw new Rbac3RuleViolation("RESOURCE_MANIFEST_INVALID");
            }
        }
    }

    /**
     * 方法 `validateFields` 按照 `ManifestFacade` 的职责处理输入，完成 `validate Fields` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `validateFields` processes its inputs according to `ManifestFacade`'s responsibility, performs the `validate Fields` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `validateFields` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `validateFields`, then continue the business flow using its result, exception, or side effect.
     *
     * @param manifest 输入参数 `manifest`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param resourceCodes 输入参数 `resourceCodes`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    private void validateFields(ResourceManifest manifest, Set<String> resourceCodes) {
        Set<String> fieldIdentities = new HashSet<>();
        for (ResourceManifest.FieldDefinition field : manifest.fieldDefinitions()) {
            if (!resourceCodes.contains(field.resourceCode())
                    || !fieldIdentities.add(field.resourceCode() + ':' + field.fieldCode())) {
                throw new Rbac3RuleViolation("RESOURCE_MANIFEST_INVALID");
            }
        }
    }

    /**
     * 方法 `put` 按照 `ManifestFacade` 的职责处理输入，完成 `put` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `put` processes its inputs according to `ManifestFacade`'s responsibility, performs the `put` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `put` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `put`, then continue the business flow using its result, exception, or side effect.
     *
     * @param target 输入参数 `target`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param kind 输入参数 `kind`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param resources 输入参数 `resources`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    private static void put(
            Map<String, ResourceKind> target,
            ResourceKind kind,
            List<ManifestResource> resources) {
        resources.forEach(resource -> target.put(resource.code(), kind));
    }

    /**
     * 类型 `ResourceKind` 位于 `ManifestFacade` 内，是枚举，用于承载 `Resource Kind` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `ResourceKind` is an enum inside `ManifestFacade` and carries the responsibility, state, or contract for `Resource Kind`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `ResourceKind` 作为 `ManifestFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `ResourceKind` as the responsibility boundary of `ManifestFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    private enum ResourceKind {
        /**
         * 字段 `APP` 表示 `ResourceKind` 中与 `APP` 相关的状态、依赖、配置或结果（声明类型 `ResourceKind`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `APP` stores the `APP`-related state, dependency, configuration, or result of `ResourceKind` (declared type `ResourceKind`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `APP` 时应保持 `ResourceKind` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `APP`, preserve `ResourceKind`'s lifecycle, immutability, and thread-safety constraints.
         */
        APP,
        /**
         * 字段 `MENU` 表示 `ResourceKind` 中与 `MENU` 相关的状态、依赖、配置或结果（声明类型 `ResourceKind`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `MENU` stores the `MENU`-related state, dependency, configuration, or result of `ResourceKind` (declared type `ResourceKind`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `MENU` 时应保持 `ResourceKind` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `MENU`, preserve `ResourceKind`'s lifecycle, immutability, and thread-safety constraints.
         */
        MENU,
        /**
         * 字段 `ROUTE` 表示 `ResourceKind` 中与 `ROUTE` 相关的状态、依赖、配置或结果（声明类型 `ResourceKind`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `ROUTE` stores the `ROUTE`-related state, dependency, configuration, or result of `ResourceKind` (declared type `ResourceKind`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `ROUTE` 时应保持 `ResourceKind` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `ROUTE`, preserve `ResourceKind`'s lifecycle, immutability, and thread-safety constraints.
         */
        ROUTE,
        /**
         * 字段 `ACTION` 表示 `ResourceKind` 中与 `ACTION` 相关的状态、依赖、配置或结果（声明类型 `ResourceKind`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `ACTION` stores the `ACTION`-related state, dependency, configuration, or result of `ResourceKind` (declared type `ResourceKind`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `ACTION` 时应保持 `ResourceKind` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `ACTION`, preserve `ResourceKind`'s lifecycle, immutability, and thread-safety constraints.
         */
        ACTION,
        /**
         * 字段 `API` 表示 `ResourceKind` 中与 `API` 相关的状态、依赖、配置或结果（声明类型 `ResourceKind`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `API` stores the `API`-related state, dependency, configuration, or result of `ResourceKind` (declared type `ResourceKind`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `API` 时应保持 `ResourceKind` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `API`, preserve `ResourceKind`'s lifecycle, immutability, and thread-safety constraints.
         */
        API
    }

    /**
     * 类型 `ManifestStore` 位于 `ManifestFacade` 内，是接口，用于承载 `Manifest Store` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `ManifestStore` is an interface inside `ManifestFacade` and carries the responsibility, state, or contract for `Manifest Store`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `ManifestStore` 作为 `ManifestFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `ManifestStore` as the responsibility boundary of `ManifestFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public interface ManifestStore {

        /**
         * 方法 `findByBuild` 按照 `ManifestStore` 的职责处理输入，完成 `find By Build` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `findByBuild` processes its inputs according to `ManifestStore`'s responsibility, performs the `find By Build` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `findByBuild` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `findByBuild`, then continue the business flow using its result, exception, or side effect.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param applicationId 输入参数 `applicationId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param artifactVersion 输入参数 `artifactVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param buildId 输入参数 `buildId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        Optional<StoredManifest> findByBuild(
                String tenantId,
                String applicationId,
                String artifactVersion,
                String buildId);

        /**
         * 方法 `insert` 按照 `ManifestStore` 的职责处理输入，完成 `insert` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `insert` processes its inputs according to `ManifestStore`'s responsibility, performs the `insert` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `insert` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `insert`, then continue the business flow using its result, exception, or side effect.
         *
         * @param manifest 输入参数 `manifest`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        void insert(StoredManifest manifest);

        /**
         * 方法 `activate` 按照 `ManifestStore` 的职责处理输入，完成 `activate` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `activate` processes its inputs according to `ManifestStore`'s responsibility, performs the `activate` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `activate` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `activate`, then continue the business flow using its result, exception, or side effect.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param applicationId 输入参数 `applicationId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param manifestId 输入参数 `manifestId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param expectedApplicationVersion 输入参数 `expectedApplicationVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param expectedCurrentManifestVersion 输入参数 `expectedCurrentManifestVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param expectedDefinitionSetId 输入参数 `expectedDefinitionSetId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param idempotencyKey 输入参数 `idempotencyKey`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param reason 输入参数 `reason`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        default ActivationMutation activate(
                String tenantId,
                String applicationId,
                String manifestId,
                long expectedApplicationVersion,
                long expectedCurrentManifestVersion,
                String expectedDefinitionSetId,
                String actorId,
                String idempotencyKey,
                String reason,
                Instant now) {
            throw new UnsupportedOperationException("manifest activation is not configured");
        }

        /**
         * 类型 `ActivationMutation` 位于 `ManifestStore` 内，是记录类型，用于承载 `Activation Mutation` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
         * Type `ActivationMutation` is a record inside `ManifestStore` and carries the responsibility, state, or contract for `Activation Mutation`; callers normally use it through its public API, Spring assembly, or implementation relationship.
         *
         * 语义与用法：将 `ActivationMutation` 作为 `ManifestStore` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
         * Semantics and usage: use `ActivationMutation` as the responsibility boundary of `ManifestStore`, following its existing construction, interface, or Spring-assembly mechanism.
         *
         * @param policyVersion 记录组件 `policyVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `policyVersion` carries constructor data whose meaning is defined by the record contract.
         * @param propagationId 记录组件 `propagationId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `propagationId` carries constructor data whose meaning is defined by the record contract.
         * @param propagationPending 记录组件 `propagationPending` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `propagationPending` carries constructor data whose meaning is defined by the record contract.
         */
        record ActivationMutation(
                /**
                 * 字段 `policyVersion` 表示 `ActivationMutation` 中与 `policy Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
                 * Field `policyVersion` stores the `policy Version`-related state, dependency, configuration, or result of `ActivationMutation` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
                 *
                 * 含义与用法：读取、传递或更新 `policyVersion` 时应保持 `ActivationMutation` 的生命周期、不可变性和线程安全约束。
                 * Meaning and usage: when reading, passing, or updating `policyVersion`, preserve `ActivationMutation`'s lifecycle, immutability, and thread-safety constraints.
                 */
                long policyVersion,
                /**
                 * 字段 `propagationId` 表示 `ActivationMutation` 中与 `propagation Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
                 * Field `propagationId` stores the `propagation Id`-related state, dependency, configuration, or result of `ActivationMutation` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
                 *
                 * 含义与用法：读取、传递或更新 `propagationId` 时应保持 `ActivationMutation` 的生命周期、不可变性和线程安全约束。
                 * Meaning and usage: when reading, passing, or updating `propagationId`, preserve `ActivationMutation`'s lifecycle, immutability, and thread-safety constraints.
                 */
                String propagationId,
                /**
                 * 字段 `propagationPending` 表示 `ActivationMutation` 中与 `propagation Pending` 相关的状态、依赖、配置或结果（声明类型 `boolean`）；其生命周期和取值含义由声明类型及所属对象共同确定。
                 * Field `propagationPending` stores the `propagation Pending`-related state, dependency, configuration, or result of `ActivationMutation` (declared type `boolean`); its lifecycle and value semantics are defined by its declared type and owning object.
                 *
                 * 含义与用法：读取、传递或更新 `propagationPending` 时应保持 `ActivationMutation` 的生命周期、不可变性和线程安全约束。
                 * Meaning and usage: when reading, passing, or updating `propagationPending`, preserve `ActivationMutation`'s lifecycle, immutability, and thread-safety constraints.
                 */
                boolean propagationPending
        ) {
        }
    }

    /**
     * 类型 `ComponentKeyRegistry` 位于 `ManifestFacade` 内，是接口，用于承载 `Component Key Registry` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `ComponentKeyRegistry` is an interface inside `ManifestFacade` and carries the responsibility, state, or contract for `Component Key Registry`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `ComponentKeyRegistry` 作为 `ManifestFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `ComponentKeyRegistry` as the responsibility boundary of `ManifestFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    @FunctionalInterface
    public interface ComponentKeyRegistry {

        /**
         * 方法 `known` 按照 `ComponentKeyRegistry` 的职责处理输入，完成 `known` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `known` processes its inputs according to `ComponentKeyRegistry`'s responsibility, performs the `known` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `known` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `known`, then continue the business flow using its result, exception, or side effect.
         *
         * @param componentKey 输入参数 `componentKey`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        boolean known(String componentKey);
    }

    /**
     * 类型 `SubmitCommand` 位于 `ManifestFacade` 内，是记录类型，用于承载 `Submit Command` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `SubmitCommand` is a record inside `ManifestFacade` and carries the responsibility, state, or contract for `Submit Command`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `SubmitCommand` 作为 `ManifestFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `SubmitCommand` as the responsibility boundary of `ManifestFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param tenantId 记录组件 `tenantId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantId` carries constructor data whose meaning is defined by the record contract.
     * @param applicationId 记录组件 `applicationId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `applicationId` carries constructor data whose meaning is defined by the record contract.
     * @param manifestId 记录组件 `manifestId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `manifestId` carries constructor data whose meaning is defined by the record contract.
     * @param definitionSetId 记录组件 `definitionSetId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `definitionSetId` carries constructor data whose meaning is defined by the record contract.
     * @param manifest 记录组件 `manifest` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `manifest` carries constructor data whose meaning is defined by the record contract.
     */
    public record SubmitCommand(
            /**
             * 字段 `tenantId` 表示 `SubmitCommand` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `SubmitCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `SubmitCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `SubmitCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String tenantId,
            /**
             * 字段 `applicationId` 表示 `SubmitCommand` 中与 `application Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `applicationId` stores the `application Id`-related state, dependency, configuration, or result of `SubmitCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `applicationId` 时应保持 `SubmitCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `applicationId`, preserve `SubmitCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String applicationId,
            /**
             * 字段 `manifestId` 表示 `SubmitCommand` 中与 `manifest Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `manifestId` stores the `manifest Id`-related state, dependency, configuration, or result of `SubmitCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `manifestId` 时应保持 `SubmitCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `manifestId`, preserve `SubmitCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String manifestId,
            /**
             * 字段 `definitionSetId` 表示 `SubmitCommand` 中与 `definition Set Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `definitionSetId` stores the `definition Set Id`-related state, dependency, configuration, or result of `SubmitCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `definitionSetId` 时应保持 `SubmitCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `definitionSetId`, preserve `SubmitCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String definitionSetId,
            /**
             * 字段 `manifest` 表示 `SubmitCommand` 中与 `manifest` 相关的状态、依赖、配置或结果（声明类型 `ResourceManifest`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `manifest` stores the `manifest`-related state, dependency, configuration, or result of `SubmitCommand` (declared type `ResourceManifest`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `manifest` 时应保持 `SubmitCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `manifest`, preserve `SubmitCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            ResourceManifest manifest
    ) {

        /**
         * 构造器 `SubmitCommand` 用于创建并初始化 `SubmitCommand` 实例，建立该类型后续方法所依赖的状态和不变量。
         * Constructor `SubmitCommand` creates and initializes `SubmitCommand`, establishing the state and invariants required by subsequent operations.
         *
         * 用法：通过 `SubmitCommand` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `SubmitCommand`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param applicationId 输入参数 `applicationId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param manifestId 输入参数 `manifestId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param definitionSetId 输入参数 `definitionSetId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param manifest 输入参数 `manifest`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public SubmitCommand {
            tenantId = required(tenantId, "tenantId");
            applicationId = required(applicationId, "applicationId");
            manifestId = required(manifestId, "manifestId");
            definitionSetId = required(definitionSetId, "definitionSetId");
            manifest = Objects.requireNonNull(manifest, "manifest");
        }
    }

    /**
     * 类型 `StoredManifest` 位于 `ManifestFacade` 内，是记录类型，用于承载 `Stored Manifest` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `StoredManifest` is a record inside `ManifestFacade` and carries the responsibility, state, or contract for `Stored Manifest`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `StoredManifest` 作为 `ManifestFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `StoredManifest` as the responsibility boundary of `ManifestFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param tenantId 记录组件 `tenantId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantId` carries constructor data whose meaning is defined by the record contract.
     * @param applicationId 记录组件 `applicationId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `applicationId` carries constructor data whose meaning is defined by the record contract.
     * @param manifestId 记录组件 `manifestId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `manifestId` carries constructor data whose meaning is defined by the record contract.
     * @param definitionSetId 记录组件 `definitionSetId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `definitionSetId` carries constructor data whose meaning is defined by the record contract.
     * @param artifactVersion 记录组件 `artifactVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `artifactVersion` carries constructor data whose meaning is defined by the record contract.
     * @param buildId 记录组件 `buildId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `buildId` carries constructor data whose meaning is defined by the record contract.
     * @param manifestVersion 记录组件 `manifestVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `manifestVersion` carries constructor data whose meaning is defined by the record contract.
     * @param checksum 记录组件 `checksum` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `checksum` carries constructor data whose meaning is defined by the record contract.
     * @param manifest 记录组件 `manifest` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `manifest` carries constructor data whose meaning is defined by the record contract.
     */
    public record StoredManifest(
            /**
             * 字段 `tenantId` 表示 `StoredManifest` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `StoredManifest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `StoredManifest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `StoredManifest`'s lifecycle, immutability, and thread-safety constraints.
             */
            String tenantId,
            /**
             * 字段 `applicationId` 表示 `StoredManifest` 中与 `application Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `applicationId` stores the `application Id`-related state, dependency, configuration, or result of `StoredManifest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `applicationId` 时应保持 `StoredManifest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `applicationId`, preserve `StoredManifest`'s lifecycle, immutability, and thread-safety constraints.
             */
            String applicationId,
            /**
             * 字段 `manifestId` 表示 `StoredManifest` 中与 `manifest Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `manifestId` stores the `manifest Id`-related state, dependency, configuration, or result of `StoredManifest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `manifestId` 时应保持 `StoredManifest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `manifestId`, preserve `StoredManifest`'s lifecycle, immutability, and thread-safety constraints.
             */
            String manifestId,
            /**
             * 字段 `definitionSetId` 表示 `StoredManifest` 中与 `definition Set Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `definitionSetId` stores the `definition Set Id`-related state, dependency, configuration, or result of `StoredManifest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `definitionSetId` 时应保持 `StoredManifest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `definitionSetId`, preserve `StoredManifest`'s lifecycle, immutability, and thread-safety constraints.
             */
            String definitionSetId,
            /**
             * 字段 `artifactVersion` 表示 `StoredManifest` 中与 `artifact Version` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `artifactVersion` stores the `artifact Version`-related state, dependency, configuration, or result of `StoredManifest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `artifactVersion` 时应保持 `StoredManifest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `artifactVersion`, preserve `StoredManifest`'s lifecycle, immutability, and thread-safety constraints.
             */
            String artifactVersion,
            /**
             * 字段 `buildId` 表示 `StoredManifest` 中与 `build Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `buildId` stores the `build Id`-related state, dependency, configuration, or result of `StoredManifest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `buildId` 时应保持 `StoredManifest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `buildId`, preserve `StoredManifest`'s lifecycle, immutability, and thread-safety constraints.
             */
            String buildId,
            /**
             * 字段 `manifestVersion` 表示 `StoredManifest` 中与 `manifest Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `manifestVersion` stores the `manifest Version`-related state, dependency, configuration, or result of `StoredManifest` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `manifestVersion` 时应保持 `StoredManifest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `manifestVersion`, preserve `StoredManifest`'s lifecycle, immutability, and thread-safety constraints.
             */
            long manifestVersion,
            /**
             * 字段 `checksum` 表示 `StoredManifest` 中与 `checksum` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `checksum` stores the `checksum`-related state, dependency, configuration, or result of `StoredManifest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `checksum` 时应保持 `StoredManifest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `checksum`, preserve `StoredManifest`'s lifecycle, immutability, and thread-safety constraints.
             */
            String checksum,
            /**
             * 字段 `manifest` 表示 `StoredManifest` 中与 `manifest` 相关的状态、依赖、配置或结果（声明类型 `ResourceManifest`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `manifest` stores the `manifest`-related state, dependency, configuration, or result of `StoredManifest` (declared type `ResourceManifest`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `manifest` 时应保持 `StoredManifest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `manifest`, preserve `StoredManifest`'s lifecycle, immutability, and thread-safety constraints.
             */
            ResourceManifest manifest
    ) {
    }

    /**
     * 类型 `ActivateCommand` 位于 `ManifestFacade` 内，是记录类型，用于承载 `Activate Command` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `ActivateCommand` is a record inside `ManifestFacade` and carries the responsibility, state, or contract for `Activate Command`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `ActivateCommand` 作为 `ManifestFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `ActivateCommand` as the responsibility boundary of `ManifestFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param tenantId 记录组件 `tenantId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantId` carries constructor data whose meaning is defined by the record contract.
     * @param applicationId 记录组件 `applicationId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `applicationId` carries constructor data whose meaning is defined by the record contract.
     * @param manifestId 记录组件 `manifestId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `manifestId` carries constructor data whose meaning is defined by the record contract.
     * @param expectedApplicationVersion 记录组件 `expectedApplicationVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `expectedApplicationVersion` carries constructor data whose meaning is defined by the record contract.
     * @param expectedCurrentManifestVersion 记录组件 `expectedCurrentManifestVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `expectedCurrentManifestVersion` carries constructor data whose meaning is defined by the record contract.
     * @param expectedDefinitionSetId 记录组件 `expectedDefinitionSetId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `expectedDefinitionSetId` carries constructor data whose meaning is defined by the record contract.
     * @param actorId 记录组件 `actorId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `actorId` carries constructor data whose meaning is defined by the record contract.
     * @param idempotencyKey 记录组件 `idempotencyKey` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `idempotencyKey` carries constructor data whose meaning is defined by the record contract.
     * @param reason 记录组件 `reason` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `reason` carries constructor data whose meaning is defined by the record contract.
     */
    public record ActivateCommand(
            /**
             * 字段 `tenantId` 表示 `ActivateCommand` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `ActivateCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `ActivateCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `ActivateCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String tenantId,
            /**
             * 字段 `applicationId` 表示 `ActivateCommand` 中与 `application Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `applicationId` stores the `application Id`-related state, dependency, configuration, or result of `ActivateCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `applicationId` 时应保持 `ActivateCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `applicationId`, preserve `ActivateCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String applicationId,
            /**
             * 字段 `manifestId` 表示 `ActivateCommand` 中与 `manifest Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `manifestId` stores the `manifest Id`-related state, dependency, configuration, or result of `ActivateCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `manifestId` 时应保持 `ActivateCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `manifestId`, preserve `ActivateCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String manifestId,
            /**
             * 字段 `expectedApplicationVersion` 表示 `ActivateCommand` 中与 `expected Application Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `expectedApplicationVersion` stores the `expected Application Version`-related state, dependency, configuration, or result of `ActivateCommand` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `expectedApplicationVersion` 时应保持 `ActivateCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `expectedApplicationVersion`, preserve `ActivateCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            long expectedApplicationVersion,
            /**
             * 字段 `expectedCurrentManifestVersion` 表示 `ActivateCommand` 中与 `expected Current Manifest Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `expectedCurrentManifestVersion` stores the `expected Current Manifest Version`-related state, dependency, configuration, or result of `ActivateCommand` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `expectedCurrentManifestVersion` 时应保持 `ActivateCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `expectedCurrentManifestVersion`, preserve `ActivateCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            long expectedCurrentManifestVersion,
            /**
             * 字段 `expectedDefinitionSetId` 表示 `ActivateCommand` 中与 `expected Definition Set Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `expectedDefinitionSetId` stores the `expected Definition Set Id`-related state, dependency, configuration, or result of `ActivateCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `expectedDefinitionSetId` 时应保持 `ActivateCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `expectedDefinitionSetId`, preserve `ActivateCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String expectedDefinitionSetId,
            /**
             * 字段 `actorId` 表示 `ActivateCommand` 中与 `actor Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `actorId` stores the `actor Id`-related state, dependency, configuration, or result of `ActivateCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `actorId` 时应保持 `ActivateCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `actorId`, preserve `ActivateCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String actorId,
            /**
             * 字段 `idempotencyKey` 表示 `ActivateCommand` 中与 `idempotency Key` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `idempotencyKey` stores the `idempotency Key`-related state, dependency, configuration, or result of `ActivateCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `idempotencyKey` 时应保持 `ActivateCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `idempotencyKey`, preserve `ActivateCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String idempotencyKey,
            /**
             * 字段 `reason` 表示 `ActivateCommand` 中与 `reason` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `reason` stores the `reason`-related state, dependency, configuration, or result of `ActivateCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `reason` 时应保持 `ActivateCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `reason`, preserve `ActivateCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String reason
    ) {

        /**
         * 构造器 `ActivateCommand` 用于创建并初始化 `ActivateCommand` 实例，建立该类型后续方法所依赖的状态和不变量。
         * Constructor `ActivateCommand` creates and initializes `ActivateCommand`, establishing the state and invariants required by subsequent operations.
         *
         * 用法：通过 `ActivateCommand` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `ActivateCommand`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param applicationId 输入参数 `applicationId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param manifestId 输入参数 `manifestId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param expectedApplicationVersion 输入参数 `expectedApplicationVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param expectedCurrentManifestVersion 输入参数 `expectedCurrentManifestVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param expectedDefinitionSetId 输入参数 `expectedDefinitionSetId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param idempotencyKey 输入参数 `idempotencyKey`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param reason 输入参数 `reason`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public ActivateCommand {
            tenantId = required(tenantId, "tenantId");
            applicationId = required(applicationId, "applicationId");
            manifestId = required(manifestId, "manifestId");
            expectedDefinitionSetId = required(
                    expectedDefinitionSetId, "expectedDefinitionSetId");
            actorId = required(actorId, "actorId");
            idempotencyKey = required(idempotencyKey, "idempotencyKey");
            reason = required(reason, "reason");
            if (expectedApplicationVersion < 0L || expectedCurrentManifestVersion < 0L) {
                throw new IllegalArgumentException("manifest versions must not be negative");
            }
        }
    }

    /**
     * 类型 `SubmissionResult` 位于 `ManifestFacade` 内，是记录类型，用于承载 `Submission Result` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `SubmissionResult` is a record inside `ManifestFacade` and carries the responsibility, state, or contract for `Submission Result`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `SubmissionResult` 作为 `ManifestFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `SubmissionResult` as the responsibility boundary of `ManifestFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param outcome 记录组件 `outcome` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `outcome` carries constructor data whose meaning is defined by the record contract.
     * @param manifestId 记录组件 `manifestId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `manifestId` carries constructor data whose meaning is defined by the record contract.
     */
    public record SubmissionResult(/**
 * 字段 `outcome` 表示 `SubmissionResult` 中与 `outcome` 相关的状态、依赖、配置或结果（声明类型 `SubmissionOutcome`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `outcome` stores the `outcome`-related state, dependency, configuration, or result of `SubmissionResult` (declared type `SubmissionOutcome`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `outcome` 时应保持 `SubmissionResult` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `outcome`, preserve `SubmissionResult`'s lifecycle, immutability, and thread-safety constraints.
 */ SubmissionOutcome outcome, /**
 * 字段 `manifestId` 表示 `SubmissionResult` 中与 `manifest Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `manifestId` stores the `manifest Id`-related state, dependency, configuration, or result of `SubmissionResult` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `manifestId` 时应保持 `SubmissionResult` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `manifestId`, preserve `SubmissionResult`'s lifecycle, immutability, and thread-safety constraints.
 */ String manifestId) {
    }

    /**
     * 类型 `ActivationResult` 位于 `ManifestFacade` 内，是记录类型，用于承载 `Activation Result` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `ActivationResult` is a record inside `ManifestFacade` and carries the responsibility, state, or contract for `Activation Result`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `ActivationResult` 作为 `ManifestFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `ActivationResult` as the responsibility boundary of `ManifestFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param manifestId 记录组件 `manifestId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `manifestId` carries constructor data whose meaning is defined by the record contract.
     * @param policyVersion 记录组件 `policyVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `policyVersion` carries constructor data whose meaning is defined by the record contract.
     * @param propagationId 记录组件 `propagationId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `propagationId` carries constructor data whose meaning is defined by the record contract.
     * @param propagationPending 记录组件 `propagationPending` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `propagationPending` carries constructor data whose meaning is defined by the record contract.
     */
    public record ActivationResult(
            /**
             * 字段 `manifestId` 表示 `ActivationResult` 中与 `manifest Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `manifestId` stores the `manifest Id`-related state, dependency, configuration, or result of `ActivationResult` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `manifestId` 时应保持 `ActivationResult` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `manifestId`, preserve `ActivationResult`'s lifecycle, immutability, and thread-safety constraints.
             */
            String manifestId,
            /**
             * 字段 `policyVersion` 表示 `ActivationResult` 中与 `policy Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `policyVersion` stores the `policy Version`-related state, dependency, configuration, or result of `ActivationResult` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `policyVersion` 时应保持 `ActivationResult` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `policyVersion`, preserve `ActivationResult`'s lifecycle, immutability, and thread-safety constraints.
             */
            long policyVersion,
            /**
             * 字段 `propagationId` 表示 `ActivationResult` 中与 `propagation Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `propagationId` stores the `propagation Id`-related state, dependency, configuration, or result of `ActivationResult` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `propagationId` 时应保持 `ActivationResult` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `propagationId`, preserve `ActivationResult`'s lifecycle, immutability, and thread-safety constraints.
             */
            String propagationId,
            /**
             * 字段 `propagationPending` 表示 `ActivationResult` 中与 `propagation Pending` 相关的状态、依赖、配置或结果（声明类型 `boolean`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `propagationPending` stores the `propagation Pending`-related state, dependency, configuration, or result of `ActivationResult` (declared type `boolean`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `propagationPending` 时应保持 `ActivationResult` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `propagationPending`, preserve `ActivationResult`'s lifecycle, immutability, and thread-safety constraints.
             */
            boolean propagationPending
    ) {
    }

    /**
     * 类型 `SubmissionOutcome` 位于 `ManifestFacade` 内，是枚举，用于承载 `Submission Outcome` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `SubmissionOutcome` is an enum inside `ManifestFacade` and carries the responsibility, state, or contract for `Submission Outcome`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `SubmissionOutcome` 作为 `ManifestFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `SubmissionOutcome` as the responsibility boundary of `ManifestFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public enum SubmissionOutcome {
        /**
         * 字段 `ACCEPTED` 表示 `SubmissionOutcome` 中与 `ACCEPTED` 相关的状态、依赖、配置或结果（声明类型 `SubmissionOutcome`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `ACCEPTED` stores the `ACCEPTED`-related state, dependency, configuration, or result of `SubmissionOutcome` (declared type `SubmissionOutcome`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `ACCEPTED` 时应保持 `SubmissionOutcome` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `ACCEPTED`, preserve `SubmissionOutcome`'s lifecycle, immutability, and thread-safety constraints.
         */
        ACCEPTED,
        /**
         * 字段 `IDEMPOTENT` 表示 `SubmissionOutcome` 中与 `IDEMPOTENT` 相关的状态、依赖、配置或结果（声明类型 `SubmissionOutcome`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `IDEMPOTENT` stores the `IDEMPOTENT`-related state, dependency, configuration, or result of `SubmissionOutcome` (declared type `SubmissionOutcome`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `IDEMPOTENT` 时应保持 `SubmissionOutcome` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `IDEMPOTENT`, preserve `SubmissionOutcome`'s lifecycle, immutability, and thread-safety constraints.
         */
        IDEMPOTENT
    }

    /**
     * 方法 `required` 按照 `ManifestFacade` 的职责处理输入，完成 `required` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `required` processes its inputs according to `ManifestFacade`'s responsibility, performs the `required` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
