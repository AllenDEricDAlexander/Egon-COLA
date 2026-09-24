package top.egon.cola.component.yuheng.mcp.engine.mcp.dao;

import top.egon.cola.component.common.mybatis.extension.EgonColaMapper;
import top.egon.cola.component.yuheng.mcp.engine.mcp.domain.po.McpApprovalRecordPO;

/**
 * 中文说明：{@code McpApprovalDAO} 是数据面进程自有的 {@code gateway_mcp_approval} 映射器，只继承
 * {@link EgonColaMapper} 的受守卫语句（{@code selectActiveById}/{@code selectActiveByIds}/
 * {@code deleteVersionedById}），签发与撤销语句仍归控制面。
 * English summary: {@code McpApprovalDAO} is the data-plane process' own mapper for {@code gateway_mcp_approval}; it only
 * inherits the guarded statements of {@link EgonColaMapper} ({@code selectActiveById}, {@code selectActiveByIds},
 * {@code deleteVersionedById}) while the issuing and revoking statements stay owned by the control plane.
 *
 * 用法 / Usage: 由 {@code @MapperScan} 注册，仅经 {@code McpApprovalPersistenceRepository} 的受守卫边界访问，
 * 一次性消费走受保护 CAS 而非本接口的自定义语句。/ Registered through {@code @MapperScan} and reached only through the
 * guarded {@code McpApprovalPersistenceRepository}; one-shot consumption goes through a protected CAS instead of a custom
 * statement here.
 */
public interface McpApprovalDAO extends EgonColaMapper<McpApprovalRecordPO> {
}
