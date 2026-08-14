package top.egon.cola.component.gateway.admin.catalog.repository.jdbc;


import top.egon.cola.component.gateway.admin.catalog.domain.vo.GatewayBusinessNodeVO;

import java.util.LinkedHashMap;
import java.util.Map;


/**
 * 中文说明：{@code GatewayCatalogMutableBusiness} 是类型，位于当前 Gateway 模块的相关包中，负责MutableBusiness相关的职责与边界。
 * English summary: {@code GatewayCatalogMutableBusiness} is a type in the current Gateway module; it owns the mutable business-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class GatewayCatalogMutableBusiness {

    /**
     * 中文说明：保存 id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.catalog.repository.jdbc.GatewayCatalogMutableBusiness} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.catalog.repository.jdbc.GatewayCatalogMutableBusiness} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.catalog.repository.jdbc.GatewayCatalogMutableBusiness} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.catalog.repository.jdbc.GatewayCatalogMutableBusiness}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final String id;

    /**
     * 中文说明：保存 code 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.catalog.repository.jdbc.GatewayCatalogMutableBusiness} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by code; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.catalog.repository.jdbc.GatewayCatalogMutableBusiness} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.catalog.repository.jdbc.GatewayCatalogMutableBusiness} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.catalog.repository.jdbc.GatewayCatalogMutableBusiness}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final String code;

    /**
     * 中文说明：保存 displayName 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.catalog.repository.jdbc.GatewayCatalogMutableBusiness} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by display name; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.catalog.repository.jdbc.GatewayCatalogMutableBusiness} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.catalog.repository.jdbc.GatewayCatalogMutableBusiness} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.catalog.repository.jdbc.GatewayCatalogMutableBusiness}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final String displayName;

    /**
     * 中文说明：保存 entities 对应的状态、依赖或配置值；字段类型为 {@code Map<String, GatewayCatalogMutableEntity>}，由 {@code top.egon.cola.component.gateway.admin.catalog.repository.jdbc.GatewayCatalogMutableBusiness} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by entities; its type is {@code Map<String, GatewayCatalogMutableEntity>}, and {@code top.egon.cola.component.gateway.admin.catalog.repository.jdbc.GatewayCatalogMutableBusiness} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.catalog.repository.jdbc.GatewayCatalogMutableBusiness} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.catalog.repository.jdbc.GatewayCatalogMutableBusiness}; do not couple callers to its representation when the owning type exposes an API.
     */
    final Map<String, GatewayCatalogMutableEntity> entities =
            new LinkedHashMap<>();

    /**
     * 中文说明：创建 {@code top.egon.cola.component.gateway.admin.catalog.repository.jdbc.GatewayCatalogMutableBusiness} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code top.egon.cola.component.gateway.admin.catalog.repository.jdbc.GatewayCatalogMutableBusiness} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param id 参数 id；parameter id。
     * @param code 参数 code；parameter code。
     * @param displayName 参数 displayName；parameter display name。
     */
    public GatewayCatalogMutableBusiness(
            String id,
            String code,
            String displayName) {
        this.id = id;
        this.code = code;
        this.displayName = displayName;
    }

    /**
     * 中文说明：执行 freeze 操作；该方法是 {@code top.egon.cola.component.gateway.admin.catalog.repository.jdbc.GatewayCatalogMutableBusiness} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the freeze operation; this method is the invocation entry point on {@code top.egon.cola.component.gateway.admin.catalog.repository.jdbc.GatewayCatalogMutableBusiness} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code top.egon.cola.component.gateway.admin.catalog.repository.jdbc.GatewayCatalogMutableBusiness.freeze(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 freeze 的处理结果；returns the result of the operation.
     */
    public GatewayBusinessNodeVO freeze() {
        return new GatewayBusinessNodeVO(
                id,
                code,
                displayName,
                entities.values().stream()
                        .map(GatewayCatalogMutableEntity::freeze)
                        .toList()
        );
    }
}
