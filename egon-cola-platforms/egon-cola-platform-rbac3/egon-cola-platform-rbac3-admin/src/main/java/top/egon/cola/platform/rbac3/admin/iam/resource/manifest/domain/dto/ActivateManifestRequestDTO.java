package top.egon.cola.platform.rbac3.admin.iam.resource.manifest.domain.dto;

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
import top.egon.cola.platform.rbac3.contract.manifest.ResourceManifest;

/**
     * 类型 `ActivateManifestRequestDTO` 位于 `ManifestController` 内，是记录类型，用于承载 `Activate Manifest Request` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `ActivateManifestRequestDTO` is a record inside `ManifestController` and carries the responsibility, state, or contract for `Activate Manifest Request`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `ActivateManifestRequestDTO` 作为 `ManifestController` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `ActivateManifestRequestDTO` as the responsibility boundary of `ManifestController`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param applicationId 记录组件 `applicationId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `applicationId` carries constructor data whose meaning is defined by the record contract.
     * @param expectedCurrentManifestVersion 记录组件 `expectedCurrentManifestVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `expectedCurrentManifestVersion` carries constructor data whose meaning is defined by the record contract.
     * @param expectedDefinitionSetId 记录组件 `expectedDefinitionSetId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `expectedDefinitionSetId` carries constructor data whose meaning is defined by the record contract.
     * @param reason 记录组件 `reason` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `reason` carries constructor data whose meaning is defined by the record contract.
     */
    public record ActivateManifestRequestDTO(
            /**
             * 字段 `applicationId` 表示 `ActivateManifestRequestDTO` 中与 `application Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `applicationId` stores the `application Id`-related state, dependency, configuration, or result of `ActivateManifestRequestDTO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `applicationId` 时应保持 `ActivateManifestRequestDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `applicationId`, preserve `ActivateManifestRequestDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotBlank String applicationId,
            /**
             * 字段 `expectedCurrentManifestVersion` 表示 `ActivateManifestRequestDTO` 中与 `expected Current Manifest Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `expectedCurrentManifestVersion` stores the `expected Current Manifest Version`-related state, dependency, configuration, or result of `ActivateManifestRequestDTO` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `expectedCurrentManifestVersion` 时应保持 `ActivateManifestRequestDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `expectedCurrentManifestVersion`, preserve `ActivateManifestRequestDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            @PositiveOrZero long expectedCurrentManifestVersion,
            /**
             * 字段 `expectedDefinitionSetId` 表示 `ActivateManifestRequestDTO` 中与 `expected Definition Set Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `expectedDefinitionSetId` stores the `expected Definition Set Id`-related state, dependency, configuration, or result of `ActivateManifestRequestDTO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `expectedDefinitionSetId` 时应保持 `ActivateManifestRequestDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `expectedDefinitionSetId`, preserve `ActivateManifestRequestDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotBlank String expectedDefinitionSetId,
            /**
             * 字段 `reason` 表示 `ActivateManifestRequestDTO` 中与 `reason` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `reason` stores the `reason`-related state, dependency, configuration, or result of `ActivateManifestRequestDTO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `reason` 时应保持 `ActivateManifestRequestDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `reason`, preserve `ActivateManifestRequestDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotBlank String reason) {
    }
