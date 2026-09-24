package top.egon.cola.component.yuheng.mcp.engine.mcp.repository;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;
import top.egon.cola.component.common.mybatis.extension.EgonColaRepository;
import top.egon.cola.component.yuheng.mcp.engine.mcp.dao.McpApprovalDAO;
import top.egon.cola.component.yuheng.mcp.engine.mcp.domain.po.McpApprovalRecordPO;

import java.util.Objects;
import java.util.Optional;

/**
 * 中文说明：{@code McpApprovalPersistenceRepository} 是数据面进程访问共享表 {@code gateway_mcp_approval} 的唯一受守卫
 * 边界；数据面只读取活跃行并对 PENDING 行做一次性 CAS 消费，签发与撤销仍归控制面。
 * English summary: {@code McpApprovalPersistenceRepository} is the only guarded boundary through which the data-plane
 * process touches the shared table {@code gateway_mcp_approval}; the data plane reads active rows and consumes a PENDING
 * row exactly once via CAS, while issuing and revoking stay with the control plane.
 *
 * 用法 / Usage: 仅经继承的受守卫 API（同租户过滤、{@code deleted_at IS NULL}、技术 {@code version} 乐观锁）访问，
 * 影响 0 行一律视为失败，绝不伪装成功。/ Reach it only through the inherited guarded API (same-tenant filtering,
 * {@code deleted_at IS NULL} and the technical {@code version} optimistic lock); a zero-row effect is a failure and is
 * never presented as success.
 */
@Slf4j
@Repository("mcpApprovalPersistenceRepository")
@RequiredArgsConstructor
public class McpApprovalPersistenceRepository extends EgonColaRepository<McpApprovalDAO, McpApprovalRecordPO> {

    /** 中文说明：{@code gateway_mcp_approval} 的映射器，语句集合见 {@code mybatis/mapper/mcp/McpApprovalDAO.xml}。 English summary: the mapper for {@code gateway_mcp_approval}; its statements live in {@code mybatis/mapper/mcp/McpApprovalDAO.xml}. */
    @Qualifier("mcpApprovalDAO")
    private final McpApprovalDAO mapper;

    /** 中文说明：组件配置，提供批量上限、分页与写守卫参数。 English summary: the component configuration supplying the batch ceiling, pagination and write-guard settings. */
    @Qualifier("egon.cola.component.mybatis-plus-top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties")
    private final EgonColaMybatisPlusProperties properties;

    @Override
    public McpApprovalDAO getBaseMapper() {
        return mapper;
    }

    @Override
    protected EgonColaMybatisPlusProperties getProperties() {
        return properties;
    }

    /**
     * 中文说明：具名读取审批令牌对应的活跃行，等价于旧 {@code SELECT ... FROM gateway_mcp_approval WHERE
     * token_digest = ?}；跨租户与已逻辑删除的行由守卫过滤自动排除，不做任何摘要重算。
     * English summary: The named active-row read for an approval token, the equivalent of the legacy
     * {@code SELECT ... FROM gateway_mcp_approval WHERE token_digest = ?}; rows of another tenant or already logically
     * deleted are excluded by the guards themselves and no digest is recomputed.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code mcpApprovalPersistenceRepository.findActiveByTokenDigest(tokenDigest)}。
     * @param tokenDigest 参数 审批令牌摘要；parameter the approval token digest.
     * @return 返回 活跃行；returns the active row when present.
     */
    public Optional<McpApprovalRecordPO> findActiveByTokenDigest(String tokenDigest) {
        Objects.requireNonNull(tokenDigest, "tokenDigest");
        return list(Wrappers.<McpApprovalRecordPO>lambdaQuery()
                        .eq(McpApprovalRecordPO::getTokenDigest, tokenDigest))
                .stream()
                .findFirst();
    }
}
