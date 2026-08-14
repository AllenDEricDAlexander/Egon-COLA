package top.egon.cola.component.gateway.admin.config.properties;


/**
 * 中文说明：{@code GatewayAdminDdcProperties} 是类型，位于当前 Gateway 模块的相关包中，负责Ddc相关的职责与边界。
 * English summary: {@code GatewayAdminDdcProperties} is a type in the current Gateway module; it owns the ddc-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public class GatewayAdminDdcProperties {

    /**
     * 中文说明：保存 targetBizCode 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code config.properties.GatewayAdminDdcProperties} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by target biz code; its type is {@code String}, and {@code config.properties.GatewayAdminDdcProperties} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code config.properties.GatewayAdminDdcProperties} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code config.properties.GatewayAdminDdcProperties}; do not couple callers to its representation when the owning type exposes an API.
     */
    private String targetBizCode = "infra";

    /**
     * 中文说明：保存 targetAppCode 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code config.properties.GatewayAdminDdcProperties} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by target app code; its type is {@code String}, and {@code config.properties.GatewayAdminDdcProperties} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code config.properties.GatewayAdminDdcProperties} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code config.properties.GatewayAdminDdcProperties}; do not couple callers to its representation when the owning type exposes an API.
     */
    private String targetAppCode = "ge";

    /**
     * 中文说明：执行 getTargetBizCode 操作；该方法是 {@code config.properties.GatewayAdminDdcProperties} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the get target biz code operation; this method is the invocation entry point on {@code config.properties.GatewayAdminDdcProperties} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code config.properties.GatewayAdminDdcProperties.getTargetBizCode(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 getTargetBizCode 的处理结果；returns the result of the operation.
     */
    public String getTargetBizCode() {
        return targetBizCode;
    }

    /**
     * 中文说明：执行 setTargetBizCode 操作；该方法是 {@code config.properties.GatewayAdminDdcProperties} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the set target biz code operation; this method is the invocation entry point on {@code config.properties.GatewayAdminDdcProperties} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code config.properties.GatewayAdminDdcProperties.setTargetBizCode(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param targetBizCode 参数 targetBizCode；parameter target biz code。
     */
    public void setTargetBizCode(String targetBizCode) {
        this.targetBizCode = targetBizCode;
    }

    /**
     * 中文说明：执行 getTargetAppCode 操作；该方法是 {@code config.properties.GatewayAdminDdcProperties} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the get target app code operation; this method is the invocation entry point on {@code config.properties.GatewayAdminDdcProperties} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code config.properties.GatewayAdminDdcProperties.getTargetAppCode(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 getTargetAppCode 的处理结果；returns the result of the operation.
     */
    public String getTargetAppCode() {
        return targetAppCode;
    }

    /**
     * 中文说明：执行 setTargetAppCode 操作；该方法是 {@code config.properties.GatewayAdminDdcProperties} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the set target app code operation; this method is the invocation entry point on {@code config.properties.GatewayAdminDdcProperties} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code config.properties.GatewayAdminDdcProperties.setTargetAppCode(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param targetAppCode 参数 targetAppCode；parameter target app code。
     */
    public void setTargetAppCode(String targetAppCode) {
        this.targetAppCode = targetAppCode;
    }
}
