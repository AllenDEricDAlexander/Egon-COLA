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
     * 类型 `DirectoryPageVO` 位于 `TenantUserDirectoryController` 内，是记录类型，用于承载 `Page View` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `DirectoryPageVO` is a record inside `TenantUserDirectoryController` and carries the responsibility, state, or contract for `Page View`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `DirectoryPageVO` 作为 `TenantUserDirectoryController` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `DirectoryPageVO` as the responsibility boundary of `TenantUserDirectoryController`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param <T> 类型参数表示分页元素的具体类型；type parameter representing the page element type.
     * @param items 记录组件 `items` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `items` carries constructor data whose meaning is defined by the record contract.
     * @param page 记录组件 `page` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `page` carries constructor data whose meaning is defined by the record contract.
     * @param size 记录组件 `size` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `size` carries constructor data whose meaning is defined by the record contract.
     * @param total 记录组件 `total` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `total` carries constructor data whose meaning is defined by the record contract.
     */
    public record DirectoryPageVO<T>(
            /**
             * 字段 `items` 表示 `DirectoryPageVO` 中与 `items` 相关的状态、依赖、配置或结果（声明类型 `List&lt;T&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `items` stores the `items`-related state, dependency, configuration, or result of `DirectoryPageVO` (declared type `List&lt;T&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `items` 时应保持 `DirectoryPageVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `items`, preserve `DirectoryPageVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            List<T> items,
            /**
             * 字段 `page` 表示 `DirectoryPageVO` 中与 `page` 相关的状态、依赖、配置或结果（声明类型 `int`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `page` stores the `page`-related state, dependency, configuration, or result of `DirectoryPageVO` (declared type `int`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `page` 时应保持 `DirectoryPageVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `page`, preserve `DirectoryPageVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            int page,
            /**
             * 字段 `size` 表示 `DirectoryPageVO` 中与 `size` 相关的状态、依赖、配置或结果（声明类型 `int`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `size` stores the `size`-related state, dependency, configuration, or result of `DirectoryPageVO` (declared type `int`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `size` 时应保持 `DirectoryPageVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `size`, preserve `DirectoryPageVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            int size,
            /**
             * 字段 `total` 表示 `DirectoryPageVO` 中与 `total` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `total` stores the `total`-related state, dependency, configuration, or result of `DirectoryPageVO` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `total` 时应保持 `DirectoryPageVO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `total`, preserve `DirectoryPageVO`'s lifecycle, immutability, and thread-safety constraints.
             */
            long total
    ) {
    }
