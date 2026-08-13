package top.egon.cola.platform.rbac3.admin.directory.domain.vo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.gateway.starter.annotation.EgonHttpService;
import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;
import top.egon.cola.component.gateway.starter.annotation.GatewaySchemaField;
import top.egon.cola.platform.rbac3.admin.config.security.CurrentRbac3Principal;
import top.egon.cola.platform.rbac3.admin.config.security.RequiresRbac3Permission;
import top.egon.cola.platform.rbac3.admin.tenant.domain.TenantContext;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import top.egon.cola.platform.rbac3.admin.shared.domain.vo.ApiEnvelopeVO;

/**
     * 类型 `DirectorySyncVO` 位于 `TenantUserDirectoryController` 内，是记录类型，用于承载 `Directory Sync View` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `DirectorySyncVO` is a record inside `TenantUserDirectoryController` and carries the responsibility, state, or contract for `Directory Sync View`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `DirectorySyncVO` 作为 `TenantUserDirectoryController` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `DirectorySyncVO` as the responsibility boundary of `TenantUserDirectoryController`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param snapshotId 记录组件 `snapshotId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `snapshotId` carries constructor data whose meaning is defined by the record contract.
     * @param outcome 记录组件 `outcome` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `outcome` carries constructor data whose meaning is defined by the record contract.
     * @param counts 记录组件 `counts` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `counts` carries constructor data whose meaning is defined by the record contract.
     * @param affectedUserCount 记录组件 `affectedUserCount` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `affectedUserCount` carries constructor data whose meaning is defined by the record contract.
     */
    public record DirectorySyncVO(
            /**
             * 字段 `snapshotId` 表示 `DirectorySyncVO` 中与 `snapshot Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `snapshotId` stores the `snapshot Id`-related state, dependency, configuration, or result of `DirectorySyncVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `snapshotId` 时应保持 `DirectorySyncVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `snapshotId`, preserve `DirectorySyncVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String snapshotId,
            /**
             * 字段 `outcome` 表示 `DirectorySyncVO` 中与 `outcome` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `outcome` stores the `outcome`-related state, dependency, configuration, or result of `DirectorySyncVO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `outcome` 时应保持 `DirectorySyncVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `outcome`, preserve `DirectorySyncVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String outcome,
            /**
             * 字段 `counts` 表示 `DirectorySyncVO` 中与 `counts` 相关的状态、依赖、配置或结果（声明类型 `Map&lt;String, Long&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `counts` stores the `counts`-related state, dependency, configuration, or result of `DirectorySyncVO` (declared type `Map&lt;String, Long&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `counts` 时应保持 `DirectorySyncVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `counts`, preserve `DirectorySyncVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            Map<String, Long> counts,
            /**
             * 字段 `affectedUserCount` 表示 `DirectorySyncVO` 中与 `affected User Count` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `affectedUserCount` stores the `affected User Count`-related state, dependency, configuration, or result of `DirectorySyncVO` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `affectedUserCount` 时应保持 `DirectorySyncVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `affectedUserCount`, preserve `DirectorySyncVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            long affectedUserCount
    ) {
    }
