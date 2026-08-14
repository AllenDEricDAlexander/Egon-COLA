package top.egon.cola.component.gateway.admin.release.domain.vo;


import top.egon.cola.component.ddc.model.management.DdcManagementPublishResult;

import static top.egon.cola.component.gateway.admin.release.domain.enums.GatewayPublicationStatusEnum.SUCCESS;

/**
 * 中文说明：{@code GatewayPublicationOutcomeVO} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责PublicationOutcome相关的职责与边界。
 * English summary: {@code GatewayPublicationOutcomeVO} is an immutable data carrier in the current Gateway module; it owns the publication outcome-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param status 参数 status；parameter status。
 * @param changeId 参数 changeId；parameter change id。
 * @param result 参数 result；parameter result。
 * @param partialApplied 参数 partialApplied；parameter partial applied。
 */
public record GatewayPublicationOutcomeVO(
        /**
         * 中文说明：保存 status 对应的状态、依赖或配置值；字段类型为 {@code top.egon.cola.component.gateway.admin.release.domain.enums.GatewayPublicationStatusEnum}，由 {@code top.egon.cola.component.gateway.admin.release.domain.vo.GatewayPublicationOutcomeVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by status; its type is {@code top.egon.cola.component.gateway.admin.release.domain.enums.GatewayPublicationStatusEnum}, and {@code top.egon.cola.component.gateway.admin.release.domain.vo.GatewayPublicationOutcomeVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.release.domain.vo.GatewayPublicationOutcomeVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.release.domain.vo.GatewayPublicationOutcomeVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        top.egon.cola.component.gateway.admin.release.domain.enums.GatewayPublicationStatusEnum status,
        /**
         * 中文说明：保存 changeId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.release.domain.vo.GatewayPublicationOutcomeVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by change id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.release.domain.vo.GatewayPublicationOutcomeVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.release.domain.vo.GatewayPublicationOutcomeVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.release.domain.vo.GatewayPublicationOutcomeVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String changeId,
        /**
         * 中文说明：保存 result 对应的状态、依赖或配置值；字段类型为 {@code DdcManagementPublishResult}，由 {@code top.egon.cola.component.gateway.admin.release.domain.vo.GatewayPublicationOutcomeVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by result; its type is {@code DdcManagementPublishResult}, and {@code top.egon.cola.component.gateway.admin.release.domain.vo.GatewayPublicationOutcomeVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.release.domain.vo.GatewayPublicationOutcomeVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.release.domain.vo.GatewayPublicationOutcomeVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        DdcManagementPublishResult result,
        /**
         * 中文说明：保存 partialApplied 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code top.egon.cola.component.gateway.admin.release.domain.vo.GatewayPublicationOutcomeVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by partial applied; its type is {@code boolean}, and {@code top.egon.cola.component.gateway.admin.release.domain.vo.GatewayPublicationOutcomeVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.release.domain.vo.GatewayPublicationOutcomeVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.release.domain.vo.GatewayPublicationOutcomeVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        boolean partialApplied
) {

    /**
     * 中文说明：执行 successful 操作；该方法是 {@code top.egon.cola.component.gateway.admin.release.domain.vo.GatewayPublicationOutcomeVO} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the successful operation; this method is the invocation entry point on {@code top.egon.cola.component.gateway.admin.release.domain.vo.GatewayPublicationOutcomeVO} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code top.egon.cola.component.gateway.admin.release.domain.vo.GatewayPublicationOutcomeVO.successful(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 successful 的处理结果；returns the result of the operation.
     */
    public boolean successful() {
        return status == SUCCESS;
    }
}
