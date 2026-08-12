package top.egon.cola.component.gateway.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 中文说明：{@code GatewayAdminApplication} 是类型，位于当前 Gateway 模块的相关包中，负责网关管理端Application相关的职责与边界。
 * English summary: {@code GatewayAdminApplication} is a type in the current Gateway module; it owns the gateway admin application-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
@SpringBootApplication
public class GatewayAdminApplication {

    /**
     * 中文说明：执行 main 操作；该方法是 {@code GatewayAdminApplication} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the main operation; this method is the invocation entry point on {@code GatewayAdminApplication} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayAdminApplication.main(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param args 参数 args；parameter args。
     */
    public static void main(String[] args) {
        SpringApplication.run(GatewayAdminApplication.class, args);
    }
}
