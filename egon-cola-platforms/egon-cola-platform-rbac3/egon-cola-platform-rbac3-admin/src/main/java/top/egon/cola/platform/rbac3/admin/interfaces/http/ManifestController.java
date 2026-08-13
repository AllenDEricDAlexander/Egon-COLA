package top.egon.cola.platform.rbac3.admin.interfaces.http;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.component.gateway.starter.annotation.EgonHttpService;
import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;
import top.egon.cola.platform.rbac3.admin.shared.repository.DatabaseClock;
import top.egon.cola.platform.rbac3.admin.resource.application.ApplicationResourceFacade;
import top.egon.cola.platform.rbac3.admin.resource.application.ManifestFacade;
import top.egon.cola.platform.rbac3.admin.config.security.CurrentRbac3Principal;
import top.egon.cola.platform.rbac3.admin.config.security.RequiresRbac3Permission;
import top.egon.cola.platform.rbac3.admin.tenant.TenantContext;
import top.egon.cola.platform.rbac3.contract.manifest.ResourceManifest;
import top.egon.cola.platform.rbac3.admin.shared.domain.vo.ApiEnvelopeVO;

/**
 * 类型 `ManifestController` 位于当前包内，是类型，用于承载 `Manifest Controller` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `ManifestController` is a type in its package and carries the responsibility, state, or contract for `Manifest Controller`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * 语义与用法：将 `ManifestController` 作为 `当前包` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
 * Semantics and usage: use `ManifestController` as the responsibility boundary of `the current package`, following its existing construction, interface, or Spring-assembly mechanism.
 */
@RestController
@RequestMapping("/api/rbac3/v1")
@GatewayInterfaceGroup(
        businessDomainCode = "platform",
        businessDomainName = "平台治理域",
        entityDomainCode = "rbac3",
        entityDomainName = "RBAC3权限实体域",
        code = "resource-manifest",
        name = "资源清单接口组")
@EgonHttpService(
        serviceName = "rbac3-admin",
        group = "default",
        version = "1.0.0",
        basePath = "/api/rbac3/v1")
public class ManifestController {

    /**
     * 字段 `manifestFacade` 表示 `ManifestController` 中与 `manifest Facade` 相关的状态、依赖、配置或结果（声明类型 `ManifestFacade`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `manifestFacade` stores the `manifest Facade`-related state, dependency, configuration, or result of `ManifestController` (declared type `ManifestFacade`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `manifestFacade` 时应保持 `ManifestController` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `manifestFacade`, preserve `ManifestController`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final ManifestFacade manifestFacade;
    /**
     * 字段 `resourceFacade` 表示 `ManifestController` 中与 `resource Facade` 相关的状态、依赖、配置或结果（声明类型 `ApplicationResourceFacade`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `resourceFacade` stores the `resource Facade`-related state, dependency, configuration, or result of `ManifestController` (declared type `ApplicationResourceFacade`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `resourceFacade` 时应保持 `ManifestController` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `resourceFacade`, preserve `ManifestController`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final ApplicationResourceFacade resourceFacade;
    /**
     * 字段 `idGenerator` 表示 `ManifestController` 中与 `id Generator` 相关的状态、依赖、配置或结果（声明类型 `LongIdGenerator`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `idGenerator` stores the `id Generator`-related state, dependency, configuration, or result of `ManifestController` (declared type `LongIdGenerator`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `idGenerator` 时应保持 `ManifestController` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `idGenerator`, preserve `ManifestController`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final LongIdGenerator idGenerator;
    /**
     * 字段 `databaseClock` 表示 `ManifestController` 中与 `database Clock` 相关的状态、依赖、配置或结果（声明类型 `DatabaseClock`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `databaseClock` stores the `database Clock`-related state, dependency, configuration, or result of `ManifestController` (declared type `DatabaseClock`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `databaseClock` 时应保持 `ManifestController` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `databaseClock`, preserve `ManifestController`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final DatabaseClock databaseClock;

    /**
     * 构造器 `ManifestController` 用于创建并初始化 `ManifestController` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `ManifestController` creates and initializes `ManifestController`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `ManifestController` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `ManifestController`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param manifestFacade 输入参数 `manifestFacade`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param resourceFacade 输入参数 `resourceFacade`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param idGenerator 输入参数 `idGenerator`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param databaseClock 输入参数 `databaseClock`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public ManifestController(
            ManifestFacade manifestFacade,
            ApplicationResourceFacade resourceFacade,
            LongIdGenerator idGenerator,
            DatabaseClock databaseClock) {
        this.manifestFacade = manifestFacade;
        this.resourceFacade = resourceFacade;
        this.idGenerator = idGenerator;
        this.databaseClock = databaseClock;
    }

    /**
     * 方法 `submit` 按照 `ManifestController` 的职责处理输入，完成 `submit` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `submit` processes its inputs according to `ManifestController`'s responsibility, performs the `submit` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `submit` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `submit`, then continue the business flow using its result, exception, or side effect.
     *
     * @param request 输入参数 `request`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @PostMapping("/internal/resource-manifests")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @RequiresRbac3Permission(permission = "system:resource-manifest:submit")
    @GatewayOperation(
            name = "rbac3-resource-manifest-submit-v1",
            summary = "提交不可变资源清单",
            externalAccessible = false,
            tags = {"rbac3", "resource-manifest"})
    public ApiEnvelopeVO<ManifestFacade.SubmissionResult> submit(
            @Valid @RequestBody SubmitManifestRequest request) {
        return ApiEnvelopeVO.success(manifestFacade.submit(new ManifestFacade.SubmitCommand(
                tenantId(),
                request.applicationId(),
                Long.toString(idGenerator.nextLongId()),
                request.definitionSetId(),
                request.manifest())));
    }

    /**
     * 方法 `manifest` 按照 `ManifestController` 的职责处理输入，完成 `manifest` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `manifest` processes its inputs according to `ManifestController`'s responsibility, performs the `manifest` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `manifest` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `manifest`, then continue the business flow using its result, exception, or side effect.
     *
     * @param manifestId 输入参数 `manifestId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @GetMapping("/resource-manifests/{manifestId}")
    @RequiresRbac3Permission(permission = "system:resource-manifest:read")
    @GatewayOperation(
            name = "rbac3-resource-manifest-get-v1",
            summary = "查询资源清单",
            externalAccessible = true,
            tags = {"rbac3", "resource-manifest"})
    public ApiEnvelopeVO<ApplicationResourceFacade.ManifestView> manifest(
            @PathVariable String manifestId) {
        return ApiEnvelopeVO.success(resourceFacade.manifest(tenantId(), manifestId));
    }

    /**
     * 方法 `validation` 按照 `ManifestController` 的职责处理输入，完成 `validation` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `validation` processes its inputs according to `ManifestController`'s responsibility, performs the `validation` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `validation` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `validation`, then continue the business flow using its result, exception, or side effect.
     *
     * @param manifestId 输入参数 `manifestId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @GetMapping("/resource-manifests/{manifestId}/validation")
    @RequiresRbac3Permission(permission = "system:resource-manifest:read")
    @GatewayOperation(
            name = "rbac3-resource-manifest-validation-v1",
            summary = "查询资源清单验证结果",
            externalAccessible = true,
            tags = {"rbac3", "resource-manifest"})
    public ApiEnvelopeVO<ApplicationResourceFacade.ManifestValidationView> validation(
            @PathVariable String manifestId) {
        return ApiEnvelopeVO.success(resourceFacade.validation(tenantId(), manifestId));
    }

    /**
     * 方法 `impact` 按照 `ManifestController` 的职责处理输入，完成 `impact` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `impact` processes its inputs according to `ManifestController`'s responsibility, performs the `impact` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `impact` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `impact`, then continue the business flow using its result, exception, or side effect.
     *
     * @param manifestId 输入参数 `manifestId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @PostMapping("/resource-manifests/{manifestId}/impact-analysis")
    @RequiresRbac3Permission(permission = "system:resource-manifest:read")
    @GatewayOperation(
            name = "rbac3-resource-manifest-impact-v1",
            summary = "分析资源清单激活影响",
            externalAccessible = true,
            tags = {"rbac3", "resource-manifest"})
    public ApiEnvelopeVO<ApplicationResourceFacade.ManifestImpactView> impact(
            @PathVariable String manifestId) {
        return ApiEnvelopeVO.success(resourceFacade.impact(tenantId(), manifestId));
    }

    /**
     * 方法 `activate` 按照 `ManifestController` 的职责处理输入，完成 `activate` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `activate` processes its inputs according to `ManifestController`'s responsibility, performs the `activate` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `activate` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `activate`, then continue the business flow using its result, exception, or side effect.
     *
     * @param manifestId 输入参数 `manifestId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param expectedApplicationVersion 输入参数 `expectedApplicationVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param idempotencyKey 输入参数 `idempotencyKey`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param request 输入参数 `request`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param principal 输入参数 `principal`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @PostMapping("/resource-manifests/{manifestId}/activate")
    @RequiresRbac3Permission(permission = "system:resource-manifest:activate")
    @GatewayOperation(
            name = "rbac3-resource-manifest-activate-v1",
            summary = "原子激活资源清单",
            externalAccessible = true,
            tags = {"rbac3", "resource-manifest"})
    public ApiEnvelopeVO<ManifestFacade.ActivationResult> activate(
            @PathVariable String manifestId,
            @RequestHeader("If-Match") long expectedApplicationVersion,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody ActivateManifestRequest request,
            @AuthenticationPrincipal CurrentRbac3Principal principal) {
        return ApiEnvelopeVO.success(manifestFacade.activate(new ManifestFacade.ActivateCommand(
                tenantId(),
                request.applicationId(),
                manifestId,
                expectedApplicationVersion,
                request.expectedCurrentManifestVersion(),
                request.expectedDefinitionSetId(),
                principal.userId(),
                idempotencyKey,
                request.reason()),
                databaseClock.transactionNow()));
    }

    /**
     * 方法 `tenantId` 按照 `ManifestController` 的职责处理输入，完成 `tenant Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `tenantId` processes its inputs according to `ManifestController`'s responsibility, performs the `tenant Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `tenantId` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `tenantId`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static String tenantId() {
        return TenantContext.requireCurrent().effectiveTenantId();
    }

    /**
     * 类型 `SubmitManifestRequest` 位于 `ManifestController` 内，是记录类型，用于承载 `Submit Manifest Request` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `SubmitManifestRequest` is a record inside `ManifestController` and carries the responsibility, state, or contract for `Submit Manifest Request`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `SubmitManifestRequest` 作为 `ManifestController` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `SubmitManifestRequest` as the responsibility boundary of `ManifestController`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param applicationId 记录组件 `applicationId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `applicationId` carries constructor data whose meaning is defined by the record contract.
     * @param definitionSetId 记录组件 `definitionSetId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `definitionSetId` carries constructor data whose meaning is defined by the record contract.
     * @param manifest 记录组件 `manifest` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `manifest` carries constructor data whose meaning is defined by the record contract.
     */
    public record SubmitManifestRequest(
            /**
             * 字段 `applicationId` 表示 `SubmitManifestRequest` 中与 `application Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `applicationId` stores the `application Id`-related state, dependency, configuration, or result of `SubmitManifestRequest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `applicationId` 时应保持 `SubmitManifestRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `applicationId`, preserve `SubmitManifestRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotBlank String applicationId,
            /**
             * 字段 `definitionSetId` 表示 `SubmitManifestRequest` 中与 `definition Set Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `definitionSetId` stores the `definition Set Id`-related state, dependency, configuration, or result of `SubmitManifestRequest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `definitionSetId` 时应保持 `SubmitManifestRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `definitionSetId`, preserve `SubmitManifestRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotBlank String definitionSetId,
            /**
             * 字段 `manifest` 表示 `SubmitManifestRequest` 中与 `manifest` 相关的状态、依赖、配置或结果（声明类型 `ResourceManifest`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `manifest` stores the `manifest`-related state, dependency, configuration, or result of `SubmitManifestRequest` (declared type `ResourceManifest`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `manifest` 时应保持 `SubmitManifestRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `manifest`, preserve `SubmitManifestRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotNull ResourceManifest manifest) {
    }

    /**
     * 类型 `ActivateManifestRequest` 位于 `ManifestController` 内，是记录类型，用于承载 `Activate Manifest Request` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `ActivateManifestRequest` is a record inside `ManifestController` and carries the responsibility, state, or contract for `Activate Manifest Request`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `ActivateManifestRequest` 作为 `ManifestController` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `ActivateManifestRequest` as the responsibility boundary of `ManifestController`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param applicationId 记录组件 `applicationId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `applicationId` carries constructor data whose meaning is defined by the record contract.
     * @param expectedCurrentManifestVersion 记录组件 `expectedCurrentManifestVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `expectedCurrentManifestVersion` carries constructor data whose meaning is defined by the record contract.
     * @param expectedDefinitionSetId 记录组件 `expectedDefinitionSetId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `expectedDefinitionSetId` carries constructor data whose meaning is defined by the record contract.
     * @param reason 记录组件 `reason` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `reason` carries constructor data whose meaning is defined by the record contract.
     */
    public record ActivateManifestRequest(
            /**
             * 字段 `applicationId` 表示 `ActivateManifestRequest` 中与 `application Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `applicationId` stores the `application Id`-related state, dependency, configuration, or result of `ActivateManifestRequest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `applicationId` 时应保持 `ActivateManifestRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `applicationId`, preserve `ActivateManifestRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotBlank String applicationId,
            /**
             * 字段 `expectedCurrentManifestVersion` 表示 `ActivateManifestRequest` 中与 `expected Current Manifest Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `expectedCurrentManifestVersion` stores the `expected Current Manifest Version`-related state, dependency, configuration, or result of `ActivateManifestRequest` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `expectedCurrentManifestVersion` 时应保持 `ActivateManifestRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `expectedCurrentManifestVersion`, preserve `ActivateManifestRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            @PositiveOrZero long expectedCurrentManifestVersion,
            /**
             * 字段 `expectedDefinitionSetId` 表示 `ActivateManifestRequest` 中与 `expected Definition Set Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `expectedDefinitionSetId` stores the `expected Definition Set Id`-related state, dependency, configuration, or result of `ActivateManifestRequest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `expectedDefinitionSetId` 时应保持 `ActivateManifestRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `expectedDefinitionSetId`, preserve `ActivateManifestRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotBlank String expectedDefinitionSetId,
            /**
             * 字段 `reason` 表示 `ActivateManifestRequest` 中与 `reason` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `reason` stores the `reason`-related state, dependency, configuration, or result of `ActivateManifestRequest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `reason` 时应保持 `ActivateManifestRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `reason`, preserve `ActivateManifestRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotBlank String reason) {
    }
}
