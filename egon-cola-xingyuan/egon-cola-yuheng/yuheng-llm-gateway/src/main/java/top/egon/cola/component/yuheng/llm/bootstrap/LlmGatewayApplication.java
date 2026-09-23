package top.egon.cola.component.yuheng.llm.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 中文说明：{@code LlmGatewayApplication} 是 LLM 引擎角色的唯一可执行入口，组件扫描范围固定为 {@code llm} 包，
 * 不承载 admin 控制面或 MCP 数据面的装配，也不引入旧 JPA/Flyway 运行链路。
 * English summary: {@code LlmGatewayApplication} is the single executable entry point of the LLM engine role.
 * It component-scans only the {@code llm} package, so it never assembles the admin control plane or the MCP data
 * plane and carries no legacy JPA/Flyway runtime.
 *
 * 用法 / Usage: 与 admin、MCP 角色并行独立启动 / Booted independently beside the admin and MCP roles.
 */
@SpringBootApplication(scanBasePackages = LlmGatewayApplication.LLM_PACKAGE)
public class LlmGatewayApplication {

    public static final String LLM_PACKAGE = "top.egon.cola.component.yuheng.llm";

    /**
     * 中文说明：启动 LLM 引擎角色进程。
     * English summary: Boots the LLM engine role process.
     *
     * 用法 / Usage: {@code LlmGatewayApplication.main(...)}。
     * @param args 参数 args；parameter args。
     */
    public static void main(String[] args) {
        SpringApplication.run(LlmGatewayApplication.class, args);
    }
}
