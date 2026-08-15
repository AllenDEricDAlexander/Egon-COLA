package top.egon.cola.platform.rbac3.admin.iam.resource.manifest.service;

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
import top.egon.cola.platform.rbac3.admin.iam.resource.manifest.domain.enums.ManifestResourceKindEnum;
import top.egon.cola.platform.rbac3.admin.iam.resource.manifest.repository.ResourceManifestRepository;
import top.egon.cola.platform.rbac3.admin.iam.resource.manifest.domain.vo.ActivationMutation;
import top.egon.cola.platform.rbac3.admin.iam.resource.repository.ComponentKeyRegistry;
import top.egon.cola.platform.rbac3.admin.iam.resource.manifest.domain.dto.SubmitCommandDTO;
import top.egon.cola.platform.rbac3.admin.iam.resource.manifest.domain.vo.StoredManifestVO;
import top.egon.cola.platform.rbac3.admin.iam.resource.manifest.domain.dto.ActivateCommandDTO;
import top.egon.cola.platform.rbac3.admin.iam.resource.manifest.domain.vo.SubmissionResultVO;
import top.egon.cola.platform.rbac3.admin.iam.resource.domain.vo.ActivationResultVO;
import top.egon.cola.platform.rbac3.admin.iam.resource.manifest.domain.enums.ManifestSubmissionOutcomeEnum;

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
     * 字段 `manifestStore` 表示 `ManifestFacade` 中与 `manifest Store` 相关的状态、依赖、配置或结果（声明类型 `ResourceManifestRepository`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `manifestStore` stores the `manifest Store`-related state, dependency, configuration, or result of `ManifestFacade` (declared type `ResourceManifestRepository`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `manifestStore` 时应保持 `ManifestFacade` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `manifestStore`, preserve `ManifestFacade`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final ResourceManifestRepository manifestStore;
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
            ResourceManifestRepository manifestStore,
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
    public SubmissionResultVO submit(SubmitCommandDTO command) {
        Objects.requireNonNull(command, "command");
        validate(command);
        ResourceManifest manifest = command.manifest();
        Optional<StoredManifestVO> existing = manifestStore.findByBuild(
                command.tenantId(),
                command.applicationId(),
                manifest.artifactVersion(),
                manifest.buildId());
        if (existing.isPresent()) {
            if (existing.get().checksum().equals(manifest.checksum())) {
                return new SubmissionResultVO(
                        ManifestSubmissionOutcomeEnum.IDEMPOTENT, existing.get().manifestId());
            }
            throw new Rbac3RuleViolation("RESOURCE_MANIFEST_CONFLICT");
        }
        StoredManifestVO stored = new StoredManifestVO(
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
        return new SubmissionResultVO(ManifestSubmissionOutcomeEnum.ACCEPTED, stored.manifestId());
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
    public ActivationResultVO activate(ActivateCommandDTO command, Instant now) {
        Objects.requireNonNull(command, "command");
        ActivationMutation mutation = manifestStore.activate(
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
        return new ActivationResultVO(
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
    private void validate(SubmitCommandDTO command) {
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
        Map<String, ManifestResourceKindEnum> kinds = resourceKinds(manifest);
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
    private Map<String, ManifestResourceKindEnum> resourceKinds(ResourceManifest manifest) {
        Map<String, ManifestResourceKindEnum> result = new LinkedHashMap<>();
        put(result, ManifestResourceKindEnum.APP, manifest.apps());
        put(result, ManifestResourceKindEnum.MENU, manifest.menus());
        put(result, ManifestResourceKindEnum.ROUTE, manifest.routes());
        put(result, ManifestResourceKindEnum.ACTION, manifest.actions());
        put(result, ManifestResourceKindEnum.API, manifest.apis());
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
            Map<String, ManifestResourceKindEnum> kinds) {
        validateParents(manifest.apps(), kinds);
        validateParents(manifest.menus(), kinds, ManifestResourceKindEnum.APP, ManifestResourceKindEnum.MENU);
        validateParents(manifest.routes(), kinds, ManifestResourceKindEnum.APP, ManifestResourceKindEnum.MENU);
        validateParents(manifest.actions(), kinds, ManifestResourceKindEnum.ROUTE);
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
            Map<String, ManifestResourceKindEnum> kinds,
            ManifestResourceKindEnum... allowedParents) {
        Set<ManifestResourceKindEnum> allowed = Set.of(allowedParents);
        for (ManifestResource resource : resources) {
            if (resource.parentCode() == null) {
                if (!allowed.isEmpty()) {
                    throw new Rbac3RuleViolation("RESOURCE_MANIFEST_INVALID");
                }
                continue;
            }
            ManifestResourceKindEnum parent = kinds.get(resource.parentCode());
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
            Map<String, ManifestResourceKindEnum> target,
            ManifestResourceKindEnum kind,
            List<ManifestResource> resources) {
        resources.forEach(resource -> target.put(resource.code(), kind));
    }

}
