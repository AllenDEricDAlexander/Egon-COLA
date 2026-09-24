package top.egon.cola.component.yuheng.admin.mcp.repository.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.yuheng.admin.mcp.converter.McpServerPersistenceConverter;
import top.egon.cola.component.yuheng.admin.mcp.domain.bo.McpServerBO;
import top.egon.cola.component.yuheng.admin.mcp.domain.po.McpServerRecordPO;
import top.egon.cola.component.yuheng.admin.mcp.repository.McpServerRepository;
import top.egon.cola.component.yuheng.admin.mcp.repository.mp.McpServerPersistenceRepository;
import top.egon.cola.component.yuheng.admin.shared.domain.exception.GatewayAdminRevisionConflictException;

/**
 * 中文说明：{@code MpMcpServerRepository} 是 gateway_mcp_server 的业务仓储门面，以受守卫的 MyBatis-Plus 边界取代旧的
 * Spring Data/JPA 托管实体：读取一律按组键或主键命中活跃行后交给转换器投影业务载体；写入把旧实现的
 * {@code saveAndFlush}/{@code flush} 脏检查改为显式的业务 {@code revision} CAS——新建落 revision 0、更新按传入 revision
 * 校验后写 storedRevision + 1、载体标记删除时改写软删时间并同步关闭 enabled，0 行写入一律按冲突如实抛出而不是静默成功；
 * {@code description}/{@code instructions} 这类可空列以显式 {@code set(col, null)} 落库，避免可空列的清空被当成未变。
 * English summary: {@code MpMcpServerRepository} is the business repository facade for gateway_mcp_server, replacing the former
 * Spring Data/JPA managed entity with the guarded MyBatis-Plus boundary: reads hit an active row by group key or identifier and let
 * the converter project the business carrier; writes turn the former {@code saveAndFlush}/{@code flush} dirty checking into an
 * explicit compare-and-set on the business {@code revision} - inserts store revision 0, updates verify the supplied revision and
 * write storedRevision + 1, and a carrier marked deleted rewrites the soft-delete instant while keeping enabled closed - with every
 * zero-row write surfacing truthfully as a conflict instead of silent success. Nullable columns such as
 * {@code description}/{@code instructions} are cleared through an explicit {@code set(col, null)} so nulling them is never mistaken
 * for leaving them unchanged.
 *
 * 用法 / Usage: 仅由 {@code McpControlPlaneService} 等管理面服务经 {@link McpServerRepository} 端口注入使用；行模型只在本类与
 * 转换器之间出现，租户、审计、软删与 MP 版本列由受守卫边界补齐。写后必须采用 {@code save} 的返回值。
 * Injected only into management-plane services through the {@link McpServerRepository} port; a row model only meets this class and the
 * converter, while the tenant, audit, soft-delete and MP version columns are filled by the guarded boundary. The value returned by
 * {@code save} must be adopted by the caller.
 */
@Slf4j
@Validated
@Repository("mcpServerRepository")
@RequiredArgsConstructor
public class MpMcpServerRepository implements McpServerRepository {

    /** {@code gateway_mcp_server} 的受守卫持久化边界。/ Guarded store for gateway_mcp_server rows. */
    @Qualifier("mcpServerPersistenceRepository")
    private final McpServerPersistenceRepository serverPersistenceRepository;

    /** gateway_mcp_server 行模型与业务载体之间的唯一转换器。/ The only converter between server rows and business carriers. */
    @Qualifier("mcpServerPersistenceConverter")
    private final McpServerPersistenceConverter mcpServerPersistenceConverter;

    /**
     * 中文说明：执行 按组列服务器 操作，复刻旧派生查询 {@code findAllByGatewayGroupIdAndDeletedFalseOrderByServerCode}：
     * 只读本组活跃行并按 serverCode 升序返回。
     * English summary: Executes the list servers by group operation, reproducing the legacy derived query
     * {@code findAllByGatewayGroupIdAndDeletedFalseOrderByServerCode}: only the group's active rows are read, ordered ascending by
     * serverCode.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code mcpServerRepository.findAllByGatewayGroupIdAndDeletedFalseOrderByServerCode(groupId)}。
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @return 返回 组内未删除的服务器载体；returns the group's non-deleted server carriers.
     */
    @Override
    public List<McpServerBO> findAllByGatewayGroupIdAndDeletedFalseOrderByServerCode(
            String gatewayGroupId) {
        Long groupColumn = requiredColumn(gatewayGroupId, "gatewayGroupId");
        return serverPersistenceRepository.list(boundPredicate(
                        Wrappers.<McpServerRecordPO>lambdaQuery()
                                .eq(McpServerRecordPO::getGatewayGroupId, groupColumn)
                                .orderByAsc(McpServerRecordPO::getServerCode)))
                .stream()
                .map(mcpServerPersistenceConverter::toBusiness)
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * 中文说明：执行 按主键读服务器 操作，复刻旧派生查询 {@code findByIdAndDeletedFalse}：非十进制或不存在的主键在旧模型下
     * 永远不可能命中活跃行，因此如实返回空。
     * English summary: Executes the read server by identifier operation, reproducing the legacy derived query
     * {@code findByIdAndDeletedFalse}: a non-decimal or absent identifier could never match an active row under the legacy model, so
     * it truthfully yields empty.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code mcpServerRepository.findByIdAndDeletedFalse(id)}。
     * @param id 参数 id；parameter id。
     * @return 返回 命中的服务器载体；returns the matched server carrier.
     */
    @Override
    public Optional<McpServerBO> findByIdAndDeletedFalse(String id) {
        Long rowId = columnValue(id);
        if (rowId == null) {
            return Optional.empty();
        }
        return serverPersistenceRepository.getOptById(rowId)
                .map(mcpServerPersistenceConverter::toBusiness);
    }

    /**
     * 中文说明：执行 保存服务器 操作，取代旧的 {@code saveAndFlush} 与托管实体脏检查：行不存在时受守卫插入并落 revision 0，
     * 行存在时按业务 revision 做 CAS 并写 storedRevision + 1，载体标记删除则同一写内落软删时间；revision 不一致或 0 行写入
     * 一律抛 {@link GatewayAdminRevisionConflictException}，权威 revision 与审计投影回写进返回载体。
     * English summary: Executes the save server operation, replacing the former {@code saveAndFlush} and managed-entity dirty
     * checking: an absent row takes the guarded insert and stores revision 0, a present row compares-and-sets on the business
     * revision and writes storedRevision + 1, and a carrier marked deleted lands the soft-delete instant inside the same write; a
     * revision mismatch or a zero-row write always raises {@link GatewayAdminRevisionConflictException}, and the authoritative
     * revision plus audit projections are written back into the returned carrier.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code server = mcpServerRepository.save(server)}；忽略返回值即为丢写。
     * Ignoring the return value is a lost write.
     * @param server 参数 服务器载体；parameter server carrier。
     * @return 返回 权威服务器载体；returns the authoritative server carrier.
     */
    @Override
    public McpServerBO save(McpServerBO server) {
        Objects.requireNonNull(server, "server");
        McpServerRecordPO candidate = mcpServerPersistenceConverter.newRow(server);
        Long rowId = candidate.getId();
        if (rowId == null) {
            throw new IllegalArgumentException("id is required");
        }
        Optional<McpServerRecordPO> current = serverPersistenceRepository.getOptById(rowId);
        if (current.isEmpty()) {
            candidate.setRevision(0L);
            if (!serverPersistenceRepository.save(candidate)) {
                log.debug("gateway_mcp_server {} was not inserted; treating as concurrent write", rowId);
                throw new GatewayAdminRevisionConflictException(server.getRevision());
            }
            return authoritative(server, candidate, null);
        }
        McpServerRecordPO persisted = current.get();
        long storedRevision = value(persisted.getRevision());
        if (server.getRevision() != storedRevision) {
            throw new GatewayAdminRevisionConflictException(storedRevision);
        }
        candidate.setId(persisted.getId());
        candidate.setVersion(persisted.getVersion());
        candidate.setRevision(storedRevision + 1);
        // create_* 列在受守卫边界上永不被更新写覆盖，因此可以安全回填到候选行，只为返回载体补齐审计投影。/
        // The create_* columns are never overwritten by an update on the guarded boundary, so copying them onto the candidate
        // only feeds the audit projection of the returned carrier.
        candidate.setCreateTime(persisted.getCreateTime());
        candidate.setCreateUserId(persisted.getCreateUserId());
        LocalDateTime deletedAt = server.isDeleted()
                ? softDeleteInstant(server.getUpdatedAt())
                : null;
        LambdaUpdateWrapper<McpServerRecordPO> predicate =
                Wrappers.<McpServerRecordPO>lambdaUpdate()
                        .eq(McpServerRecordPO::getId, persisted.getId())
                        .eq(McpServerRecordPO::getRevision, storedRevision);
        if (deletedAt != null) {
            predicate.set(McpServerRecordPO::getDeletedAt, deletedAt);
        } else {
            if (server.getDescription() == null) {
                predicate.set(McpServerRecordPO::getDescription, null);
            }
            if (server.getInstructions() == null) {
                predicate.set(McpServerRecordPO::getInstructions, null);
            }
        }
        if (!serverPersistenceRepository.update(candidate, boundPredicate(predicate))) {
            throw new GatewayAdminRevisionConflictException(storedRevision);
        }
        return authoritative(server, candidate, deletedAt);
    }

    /**
     * 中文说明：把行模型投影回业务载体并补上仓储侧权威值：业务 revision、审计投影与由软删时间推导的 deleted 标志。
     * English summary: Projects the row back onto the business carrier and patches in the repository-authoritative values: the
     * business revision, the audit projections and the deleted flag derived from the soft-delete instant.
     *
     * 用法 / Usage: 仅在本类保存成功后调用。/ Invoked only after a successful save in this class.
     * @param carrier 参数 业务载体；parameter business carrier。
     * @param row 参数 已写入的行模型；parameter the written row model。
     * @param deletedAt 参数 本次写入落下的软删时间，未删除时为 {@code null}；parameter the soft-delete instant written by this
     *                  save, {@code null} when the row stays active。
     * @return 返回 权威业务载体；returns the authoritative business carrier.
     */
    private McpServerBO authoritative(
            McpServerBO carrier,
            McpServerRecordPO row,
            LocalDateTime deletedAt) {
        carrier.setRevision(value(row.getRevision()));
        carrier.setCreatedAt(row.getCreateTime());
        carrier.setCreatedBy(row.getCreateUserId());
        carrier.setUpdatedAt(row.getUpdateTime());
        carrier.setUpdatedBy(row.getUpdateUserId());
        if (deletedAt != null) {
            carrier.setDeleted(true);
            carrier.setUpdatedAt(deletedAt.toInstant(ZoneOffset.UTC));
        }
        log.debug(
                "gateway_mcp_server {} resolved to authoritative revision {}",
                row.getId(),
                row.getRevision()
        );
        return carrier;
    }

    /**
     * 中文说明：把调用方携带的删除时刻换算为软删列值；缺失时如实退化为当前 UTC 时刻，与旧实现「删除必留时间戳」一致。
     * English summary: Converts the caller-carried deletion instant into the soft-delete column value; an absent instant degrades
     * truthfully to the current UTC instant, matching the legacy rule that a deletion always records a timestamp.
     *
     * 用法 / Usage: 仅由 {@link #save(McpServerBO)} 在软删分支调用。
     * @param updatedAt 参数 载体上的更新时刻；parameter the carrier's update instant。
     * @return 返回 软删列值；returns the soft-delete column value.
     */
    private static LocalDateTime softDeleteInstant(Instant updatedAt) {
        return LocalDateTime.ofInstant(
                updatedAt == null ? Instant.now() : updatedAt,
                ZoneOffset.UTC
        );
    }

    /**
     * 中文说明：把可空的业务 revision 列投影为基础类型；列缺失即 0，与旧默认值一致。
     * English summary: Projects the nullable business revision column onto the primitive; an absent column reads as zero, matching the
     * legacy default.
     *
     * 用法 / Usage: 仅在本类内比较与回写 revision 时使用。
     * @param value 参数 列值；parameter column value。
     * @return 返回 基础类型值；returns the primitive value.
     */
    private static long value(Long value) {
        return value == null ? 0L : value;
    }

    /**
     * 中文说明：读取必填的外键列值：空白沿用旧实现的 {@code gatewayGroupId is required} 文案，非十进制在旧 VARCHAR 外键模型下
     * 不可能命中任何行，因此如实拒绝。
     * English summary: Reads a mandatory foreign-key column value: blank keeps the legacy {@code gatewayGroupId is required} message,
     * and a non-decimal value could never match a row under the legacy VARCHAR foreign-key model, so it is rejected truthfully.
     *
     * 用法 / Usage: 仅由按组键读取的方法调用。
     * @param value 参数 业务标识；parameter business identifier。
     * @param field 参数 字段名；parameter field name。
     * @return 返回 列值；returns the column value.
     */
    private static Long requiredColumn(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        Long column = columnValue(value);
        if (column == null) {
            throw new IllegalArgumentException(field + " must be decimal: " + value);
        }
        return column;
    }

    /**
     * 中文说明：把端口携带的十进制不透明标识转换为新 {@code bigint} 列的取值；null、空白或非十进制一律返回 {@code null}，
     * 由调用方映射为「无匹配行」的空读取。
     * English summary: Converts the decimal opaque identifier carried by the port into a value of the new {@code bigint} column; null,
     * blank and non-decimal inputs all yield {@code null}, which callers map onto an empty "no matching row" read.
     *
     * 用法 / Usage: 仅在本类内为守卫查询准备主键时调用。
     * @param opaqueId 参数 不透明标识；parameter opaque identifier。
     * @return 返回 列值；returns the column value.
     */
    private static Long columnValue(String opaqueId) {
        if (opaqueId == null || opaqueId.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(opaqueId.trim());
        } catch (NumberFormatException invalidOpaqueId) {
            return null;
        }
    }

    /**
     * 中文说明：先求值一次条件片段，把 wrapper 的参数绑定物化后再交给受守卫边界；等价于旧实现把占位符与参数一并交给 JdbcTemplate。
     * English summary: Evaluates the condition segment once so the wrapper's parameter bindings are materialized before the guarded
     * boundary receives it, the equivalent of handing placeholders and arguments to JdbcTemplate together in the legacy code.
     *
     * 用法 / Usage: 所有交给受守卫边界的 wrapper 都必须经过本方法。
     * @param predicate 参数 条件包装器；parameter the condition wrapper。
     * @return 返回 已绑定参数的条件包装器；returns the parameter-bound condition wrapper.
     */
    private static <C extends com.baomidou.mybatisplus.core.conditions.Wrapper<?>> C boundPredicate(
            C predicate) {
        predicate.getSqlSegment();
        return predicate;
    }
}
