package top.egon.cola.component.gateway.admin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 中文说明：{@code GatewayAdminProperties} 是配置属性模型，位于当前 Gateway 模块的相关包中，负责网关管理端Properties相关的职责与边界。
 * English summary: {@code GatewayAdminProperties} is a gateway admin properties properties in the current Gateway module; it owns the gateway admin properties-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
@ConfigurationProperties(prefix = "gateway.admin")
public class GatewayAdminProperties {

    /**
     * 中文说明：保存 规则Chunk 对应的状态、依赖或配置值；字段类型为 {@code RuleChunk}，由 {@code GatewayAdminProperties} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by rule chunk; its type is {@code RuleChunk}, and {@code GatewayAdminProperties} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayAdminProperties} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayAdminProperties}; do not couple callers to its representation when the owning type exposes an API.
     */
    private RuleChunk ruleChunk = new RuleChunk();

    /**
     * 中文说明：保存 ddc 对应的状态、依赖或配置值；字段类型为 {@code Ddc}，由 {@code GatewayAdminProperties} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by ddc; its type is {@code Ddc}, and {@code GatewayAdminProperties} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayAdminProperties} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayAdminProperties}; do not couple callers to its representation when the owning type exposes an API.
     */
    private Ddc ddc = new Ddc();

    /**
     * 中文说明：执行 get规则Chunk 操作；该方法是 {@code GatewayAdminProperties} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the get rule chunk operation; this method is the invocation entry point on {@code GatewayAdminProperties} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayAdminProperties.getRuleChunk(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 get规则Chunk 的处理结果；returns the result of the operation.
     */
    public RuleChunk getRuleChunk() {
        return ruleChunk;
    }

    /**
     * 中文说明：执行 set规则Chunk 操作；该方法是 {@code GatewayAdminProperties} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the set rule chunk operation; this method is the invocation entry point on {@code GatewayAdminProperties} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayAdminProperties.setRuleChunk(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param ruleChunk 参数 规则Chunk；parameter rule chunk。
     */
    public void setRuleChunk(RuleChunk ruleChunk) {
        this.ruleChunk = ruleChunk;
    }

    /**
     * 中文说明：执行 getDdc 操作；该方法是 {@code GatewayAdminProperties} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the get ddc operation; this method is the invocation entry point on {@code GatewayAdminProperties} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayAdminProperties.getDdc(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 getDdc 的处理结果；returns the result of the operation.
     */
    public Ddc getDdc() {
        return ddc;
    }

    /**
     * 中文说明：执行 setDdc 操作；该方法是 {@code GatewayAdminProperties} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the set ddc operation; this method is the invocation entry point on {@code GatewayAdminProperties} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayAdminProperties.setDdc(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param ddc 参数 ddc；parameter ddc。
     */
    public void setDdc(Ddc ddc) {
        this.ddc = ddc;
    }

    /**
     * 中文说明：{@code Ddc} 是类型，位于当前 Gateway 模块的相关包中，负责Ddc相关的职责与边界。
     * English summary: {@code Ddc} is a type in the current Gateway module; it owns the ddc-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     */
    public static class Ddc {

        /**
         * 中文说明：保存 targetBizCode 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayAdminProperties.Ddc} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by target biz code; its type is {@code String}, and {@code GatewayAdminProperties.Ddc} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayAdminProperties.Ddc} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayAdminProperties.Ddc}; do not couple callers to its representation when the owning type exposes an API.
         */
        private String targetBizCode = "infra";

        /**
         * 中文说明：保存 targetAppCode 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayAdminProperties.Ddc} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by target app code; its type is {@code String}, and {@code GatewayAdminProperties.Ddc} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayAdminProperties.Ddc} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayAdminProperties.Ddc}; do not couple callers to its representation when the owning type exposes an API.
         */
        private String targetAppCode = "ge";

        /**
         * 中文说明：执行 getTargetBizCode 操作；该方法是 {@code GatewayAdminProperties.Ddc} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the get target biz code operation; this method is the invocation entry point on {@code GatewayAdminProperties.Ddc} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayAdminProperties.Ddc.getTargetBizCode(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 getTargetBizCode 的处理结果；returns the result of the operation.
         */
        public String getTargetBizCode() {
            return targetBizCode;
        }

        /**
         * 中文说明：执行 setTargetBizCode 操作；该方法是 {@code GatewayAdminProperties.Ddc} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the set target biz code operation; this method is the invocation entry point on {@code GatewayAdminProperties.Ddc} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayAdminProperties.Ddc.setTargetBizCode(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param targetBizCode 参数 targetBizCode；parameter target biz code。
         */
        public void setTargetBizCode(String targetBizCode) {
            this.targetBizCode = targetBizCode;
        }

        /**
         * 中文说明：执行 getTargetAppCode 操作；该方法是 {@code GatewayAdminProperties.Ddc} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the get target app code operation; this method is the invocation entry point on {@code GatewayAdminProperties.Ddc} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayAdminProperties.Ddc.getTargetAppCode(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 getTargetAppCode 的处理结果；returns the result of the operation.
         */
        public String getTargetAppCode() {
            return targetAppCode;
        }

        /**
         * 中文说明：执行 setTargetAppCode 操作；该方法是 {@code GatewayAdminProperties.Ddc} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the set target app code operation; this method is the invocation entry point on {@code GatewayAdminProperties.Ddc} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayAdminProperties.Ddc.setTargetAppCode(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param targetAppCode 参数 targetAppCode；parameter target app code。
         */
        public void setTargetAppCode(String targetAppCode) {
            this.targetAppCode = targetAppCode;
        }
    }

    /**
     * 中文说明：{@code RuleChunk} 是类型，位于当前 Gateway 模块的相关包中，负责规则Chunk相关的职责与边界。
     * English summary: {@code RuleChunk} is a type in the current Gateway module; it owns the rule chunk-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     */
    public static class RuleChunk {

        /**
         * 中文说明：保存 retention 对应的状态、依赖或配置值；字段类型为 {@code Duration}，由 {@code GatewayAdminProperties.RuleChunk} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by retention; its type is {@code Duration}, and {@code GatewayAdminProperties.RuleChunk} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayAdminProperties.RuleChunk} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayAdminProperties.RuleChunk}; do not couple callers to its representation when the owning type exposes an API.
         */
        private Duration retention = Duration.ofHours(24);

        /**
         * 中文说明：执行 getRetention 操作；该方法是 {@code GatewayAdminProperties.RuleChunk} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the get retention operation; this method is the invocation entry point on {@code GatewayAdminProperties.RuleChunk} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayAdminProperties.RuleChunk.getRetention(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 getRetention 的处理结果；returns the result of the operation.
         */
        public Duration getRetention() {
            return retention;
        }

        /**
         * 中文说明：执行 setRetention 操作；该方法是 {@code GatewayAdminProperties.RuleChunk} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the set retention operation; this method is the invocation entry point on {@code GatewayAdminProperties.RuleChunk} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayAdminProperties.RuleChunk.setRetention(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param retention 参数 retention；parameter retention。
         */
        public void setRetention(Duration retention) {
            this.retention = retention;
        }
    }
}
