package top.egon.cola.component.gateway.admin.shared.repository.jdbc;


import java.sql.Timestamp;
import java.time.Instant;

/**
 * 中文说明：{@code GatewayJdbcParameters} 是类型，位于当前 Gateway 模块的相关包中，负责Jdbc网关Parameters相关的职责与边界。
 * English summary: {@code GatewayJdbcParameters} is a type in the current Gateway module; it owns the jdbc gateway parameters-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class GatewayJdbcParameters {

    /**
     * 中文说明：创建 {@code GatewayJdbcParameters} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayJdbcParameters} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     */
    private GatewayJdbcParameters() {
    }

    /**
     * 中文说明：执行 timestamp 操作；该方法是 {@code GatewayJdbcParameters} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the timestamp operation; this method is the invocation entry point on {@code GatewayJdbcParameters} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayJdbcParameters.timestamp(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 timestamp 的处理结果；returns the result of the operation.
     */
    public static Timestamp timestamp(Instant value) {
        return value == null ? null : Timestamp.from(value);
    }
}
