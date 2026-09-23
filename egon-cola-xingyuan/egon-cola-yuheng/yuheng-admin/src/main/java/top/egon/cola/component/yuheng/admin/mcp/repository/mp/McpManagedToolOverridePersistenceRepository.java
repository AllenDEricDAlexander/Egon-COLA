package top.egon.cola.component.yuheng.admin.mcp.repository.mp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;
import top.egon.cola.component.common.mybatis.extension.EgonColaRepository;
import top.egon.cola.component.yuheng.admin.mcp.dao.McpManagedToolOverrideDAO;
import top.egon.cola.component.yuheng.admin.mcp.domain.po.McpManagedToolOverrideRecordPO;

/**
 * gateway_mcp_managed_tool_override 的受守卫持久化边界，复用 EgonColaRepository 的租户过滤、活跃读取与乐观锁 CAS 语义。
 * Guarded persistence boundary for gateway_mcp_managed_tool_override; reuses EgonColaRepository tenant filtering, active reads and CAS writes.
 * 用法 / Usage: 业务代码仅经继承的 save/updateById/removeById 及具名查询访问，禁止裸 Wrapper 绕过守卫。
 */
@Slf4j
@Repository("mcpManagedToolOverridePersistenceRepository")
@RequiredArgsConstructor
public class McpManagedToolOverridePersistenceRepository extends EgonColaRepository<McpManagedToolOverrideDAO, McpManagedToolOverrideRecordPO> {

    @Qualifier("mcpManagedToolOverrideDAO")
    private final McpManagedToolOverrideDAO mapper;

    @Qualifier("egon.cola.component.mybatis-plus-top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties")
    private final EgonColaMybatisPlusProperties properties;

    @Override
    public McpManagedToolOverrideDAO getBaseMapper() {
        return mapper;
    }

    @Override
    protected EgonColaMybatisPlusProperties getProperties() {
        return properties;
    }
}
