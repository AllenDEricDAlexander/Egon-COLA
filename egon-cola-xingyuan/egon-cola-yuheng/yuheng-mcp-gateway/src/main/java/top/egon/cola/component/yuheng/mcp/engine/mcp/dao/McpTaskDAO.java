package top.egon.cola.component.yuheng.mcp.engine.mcp.dao;

import top.egon.cola.component.common.mybatis.extension.EgonColaMapper;
import top.egon.cola.component.yuheng.mcp.engine.mcp.domain.po.McpTaskRecordPO;

/**
 * 中文说明：{@code McpTaskDAO} 是数据面进程自有的 {@code gateway_mcp_task_instance} 映射器，只继承
 * {@link EgonColaMapper} 的受守卫语句（{@code selectActiveById}/{@code selectActiveByIds}/
 * {@code deleteVersionedById}），不声明任何按状态批量写入的自定义语句；批量过期一律先具名查询活跃行的
 * {@code id}/{@code version}，再逐行走受保护 CAS 或逻辑删除。
 * English summary: {@code McpTaskDAO} is the data-plane process' own mapper for {@code gateway_mcp_task_instance}. It
 * only inherits the guarded statements of {@link EgonColaMapper} ({@code selectActiveById}, {@code selectActiveByIds},
 * {@code deleteVersionedById}) and declares no custom statement that writes by state alone: expiry first reads the
 * active rows' {@code id}/{@code version} by a named query and then applies protected CAS or soft-delete per row.
 *
 * 用法 / Usage: 由 {@code @MapperScan} 注册，仅经 {@code McpTaskPersistenceRepository} 的受守卫边界访问，
 * 业务代码不得直接注入本接口。/ Registered through {@code @MapperScan} and reached only through the guarded
 * {@code McpTaskPersistenceRepository}; business code must not inject this interface directly.
 */
public interface McpTaskDAO extends EgonColaMapper<McpTaskRecordPO> {
}
