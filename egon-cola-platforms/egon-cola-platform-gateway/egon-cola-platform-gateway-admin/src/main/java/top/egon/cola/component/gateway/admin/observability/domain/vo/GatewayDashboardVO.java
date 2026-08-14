package top.egon.cola.component.gateway.admin.observability.domain.vo;


import top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayProtocolCallDTO;
import top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayRequestPointDTO;

import java.util.List;

/**
 * 中文说明：{@code GatewayDashboardVO} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责GatewayDashboardVO相关的职责与边界。
 * English summary: {@code GatewayDashboardVO} is an immutable data carrier in the current Gateway module; it owns the dashboard summary-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param gatewayGroups 参数 网关Groups；parameter gateway groups。
 * @param readyEngines 参数 readyEngines；parameter ready engines。
 * @param totalEngines 参数 totalEngines；parameter total engines。
 * @param inconsistentGroups 参数 inconsistentGroups；parameter inconsistent groups。
 * @param activeProviders 参数 activeProviders；parameter active providers。
 * @param abnormalProviders 参数 abnormalProviders；parameter abnormal providers。
 * @param releaseSuccessRate 参数 发布SuccessRate；parameter release success rate。
 * @param requestSeries 参数 请求Series；parameter request series。
 * @param protocolCalls 参数 protocolCalls；parameter protocol calls。
 * @param observabilityState 参数 可观测性State；parameter observability state。
 */
public record GatewayDashboardVO(
        /**
         * 中文说明：保存 网关Groups 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayDashboardVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by gateway groups; its type is {@code long}, and {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayDashboardVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayDashboardVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayDashboardVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        long gatewayGroups,
        /**
         * 中文说明：保存 readyEngines 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayDashboardVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by ready engines; its type is {@code long}, and {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayDashboardVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayDashboardVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayDashboardVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        long readyEngines,
        /**
         * 中文说明：保存 totalEngines 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayDashboardVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by total engines; its type is {@code long}, and {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayDashboardVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayDashboardVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayDashboardVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        long totalEngines,
        /**
         * 中文说明：保存 inconsistentGroups 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayDashboardVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by inconsistent groups; its type is {@code long}, and {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayDashboardVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayDashboardVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayDashboardVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        long inconsistentGroups,
        /**
         * 中文说明：保存 activeProviders 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayDashboardVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by active providers; its type is {@code long}, and {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayDashboardVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayDashboardVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayDashboardVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        long activeProviders,
        /**
         * 中文说明：保存 abnormalProviders 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayDashboardVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by abnormal providers; its type is {@code long}, and {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayDashboardVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayDashboardVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayDashboardVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        long abnormalProviders,
        /**
         * 中文说明：保存 发布SuccessRate 对应的状态、依赖或配置值；字段类型为 {@code double}，由 {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayDashboardVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by release success rate; its type is {@code double}, and {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayDashboardVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayDashboardVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayDashboardVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        double releaseSuccessRate,
        /**
         * 中文说明：保存 请求Series 对应的状态、依赖或配置值；字段类型为 {@code List<GatewayRequestPointDTO>}，由 {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayDashboardVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by request series; its type is {@code List<GatewayRequestPointDTO>}, and {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayDashboardVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayDashboardVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayDashboardVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        List<GatewayRequestPointDTO> requestSeries,
        /**
         * 中文说明：保存 protocolCalls 对应的状态、依赖或配置值；字段类型为 {@code List<GatewayProtocolCallDTO>}，由 {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayDashboardVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by protocol calls; its type is {@code List<GatewayProtocolCallDTO>}, and {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayDashboardVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayDashboardVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayDashboardVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        List<GatewayProtocolCallDTO> protocolCalls,
        /**
         * 中文说明：保存 可观测性State 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayDashboardVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by observability state; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayDashboardVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayDashboardVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayDashboardVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String observabilityState
) {

    /**
     * 中文说明：创建 {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayDashboardVO} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayDashboardVO} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param gatewayGroups 参数 网关Groups；parameter gateway groups。
     * @param readyEngines 参数 readyEngines；parameter ready engines。
     * @param totalEngines 参数 totalEngines；parameter total engines。
     * @param inconsistentGroups 参数 inconsistentGroups；parameter inconsistent groups。
     * @param activeProviders 参数 activeProviders；parameter active providers。
     * @param abnormalProviders 参数 abnormalProviders；parameter abnormal providers。
     * @param releaseSuccessRate 参数 发布SuccessRate；parameter release success rate。
     * @param requestSeries 参数 请求Series；parameter request series。
     * @param protocolCalls 参数 protocolCalls；parameter protocol calls。
     * @param observabilityState 参数 可观测性State；parameter observability state。
     */
    public GatewayDashboardVO {
        requestSeries = List.copyOf(requestSeries);
        protocolCalls = List.copyOf(protocolCalls);
    }
}
