package top.egon.cola.component.yuheng.mcp.engine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 中文说明：MCP 专属可执行入口，固定扫描本进程的装配包。
 * English summary: MCP-only entry point; role-local configuration owns bean aliases and lifecycle.
 * 用法 / Usage: Run independently beside the API_RPC engine against the same release stream.
 */
@SpringBootApplication(scanBasePackages = "top.egon.cola.component.yuheng.mcp.engine.bootstrap")
public class McpGatewayEngineApplication {

    public static void main(String[] args) {
        SpringApplication.run(McpGatewayEngineApplication.class, args);
    }
}
