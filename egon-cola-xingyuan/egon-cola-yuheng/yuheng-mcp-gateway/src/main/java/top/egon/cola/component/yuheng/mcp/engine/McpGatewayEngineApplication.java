package top.egon.cola.component.yuheng.mcp.engine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.mybatis.spring.annotation.MapperScan;
import top.egon.cola.component.yuheng.mcp.engine.mcp.dao.McpApprovalDAO;
import top.egon.cola.component.yuheng.mcp.engine.mcp.dao.McpTaskDAO;

/**
 * 中文说明：MCP 专属可执行入口，显式扫描 bootstrap/config/MCP feature packages，并注册 task/approval Mapper；受管身份配置与持久化仓储因此进入真实服务上下文。
 * English summary: MCP-only executable entry that explicitly scans the bootstrap/config/MCP feature packages and registers
 * task/approval mappers, so managed persistence configuration and repositories are part of the real service context.
 * 用法 / Usage: Run independently beside the API_RPC engine against the same release stream.
 */
@SpringBootApplication(scanBasePackages = {
        "top.egon.cola.component.yuheng.mcp.engine.bootstrap",
        "top.egon.cola.component.yuheng.mcp.engine.config",
        "top.egon.cola.component.yuheng.mcp.engine.mcp"
})
@MapperScan(basePackageClasses = {McpTaskDAO.class, McpApprovalDAO.class})
public class McpGatewayEngineApplication {

    public static void main(String[] args) {
        SpringApplication.run(McpGatewayEngineApplication.class, args);
    }
}
