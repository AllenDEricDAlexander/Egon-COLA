package top.egon.cola.component.yuheng.admin.mcp.repository.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.yuheng.admin.mcp.converter.McpArtifactMetadataPersistenceConverter;
import top.egon.cola.component.yuheng.admin.mcp.domain.bo.McpArtifactMetadataBO;
import top.egon.cola.component.yuheng.admin.mcp.domain.enums.McpArtifactStatusEnum;
import top.egon.cola.component.yuheng.admin.mcp.domain.po.McpArtifactMetadataRecordPO;
import top.egon.cola.component.yuheng.admin.mcp.repository.McpArtifactMetadataRepository;
import top.egon.cola.component.yuheng.admin.mcp.repository.mp.McpArtifactMetadataPersistenceRepository;

/**
 * 中文说明：{@code MpMcpArtifactMetadataRepository} 是应用制品元数据的 MyBatis-Plus 门面存储，逐方法取代被退役的遗留持久化载体：
 * {@code gateway_mcp_app_artifact} 的每次读写都走受守卫的 {@code EgonColaRepository} 边界（同租户过滤、仅活跃行
 * {@code deleted_at IS NULL}、技术 {@code version} 乐观锁），而 {@code gateway_group_id} 已是 {@code bigint} 外键；
 * {@code save} 保留旧 {@code INSERT} 的列集合与 {@code 'ACTIVE'} 字面量（状态由
 * {@code mcpArtifactMetadataPersistenceConverter.newRow(...)} 落位），受守卫边界以布尔值上报插入是否命中唯一约束，
 * 故未成功时如实抛 {@code YUHENG_ADMIN_MCP_ARTIFACT_INSERT_CONFLICT}；{@code find} 与 {@code list} 保留
 * {@code status = 'ACTIVE'} 谓词与 {@code ORDER BY app_code, app_version}，只把不可读的状态行留给 {@code revoke}；
 * {@code revoke} 保留「仅 {@code id + status = 'ACTIVE'} 命中一行才算成功」的 {@code == 1} 语义，
 * 且 {@code SET} 只带 {@code status} 一列，与旧语句一致；两个 jsonb 集合列的编解码只发生在转换器内，
 * 读写两侧的载体一律重新经 {@code McpArtifactMetadataBO.normalized(...)} 复核旧构造器不变量，
 * 公开端口不泄漏行模型或 DAO。
 *
 * English summary: {@code MpMcpArtifactMetadataRepository} is the MyBatis-Plus facade store of application artifact metadata, replacing
 * the retired legacy persistence carrier method by method: every read and write of {@code gateway_mcp_app_artifact} goes through the
 * guarded {@code EgonColaRepository} boundary (same-tenant filtering, active rows only under {@code deleted_at IS NULL}, the technical
 * {@code version} optimistic lock) while {@code gateway_group_id} is already a {@code bigint} foreign key; {@code save} keeps the legacy
 * column set and the {@code 'ACTIVE'} status literal (applied by {@code mcpArtifactMetadataPersistenceConverter.newRow(...)}), and
 * because the guarded boundary reports an insert as a boolean a missed insert surfaces truthfully as
 * {@code YUHENG_ADMIN_MCP_ARTIFACT_INSERT_CONFLICT}; {@code find} and {@code list} keep the {@code status = 'ACTIVE'} predicate and the
 * {@code ORDER BY app_code, app_version}, leaving only the unreadable revoked rows to {@code revoke}; {@code revoke} keeps the
 * "exactly one row hit by {@code id + status = 'ACTIVE'} means success" semantics with a {@code SET} carrying {@code status} alone,
 * like the legacy statement; the two jsonb collection columns are encoded and decoded only inside the converter, both boundaries
 * re-check carriers through {@code McpArtifactMetadataBO.normalized(...)}, and the public port leaks neither the row model nor a DAO.
 *
 * 用法 / Usage: 通过业务端口 {@code McpArtifactMetadataRepository} 由 Spring 容器注入；遗留实现没有声明事务边界，本门面也不声明
 * {@code @Transactional}；读不到行、行不可表示或状态谓词不成立时按旧契约返回空值或 {@code false}，不抛异常。
 * Inject it through the business port {@code McpArtifactMetadataRepository}; the legacy carrier declared no transaction boundary and
 * neither does this facade; a missing row, an unrepresentable identifier or an unmet status predicate surfaces as the legacy empty
 * value or {@code false} instead of a failure.
 */
@Slf4j
@Repository("mpMcpArtifactMetadataRepository")
@RequiredArgsConstructor
@Validated
public class MpMcpArtifactMetadataRepository
        implements McpArtifactMetadataRepository {

    /**
     * 中文说明：制品行的受守卫持久化仓储（租户过滤、活跃读取与乐观锁 CAS 的唯一入口）。
     * English summary: The guarded persistence store for artifact rows, the only entry point for tenant filtering, active reads and the
     * optimistic-lock CAS.
     */
    @Qualifier("mcpArtifactMetadataPersistenceRepository")
    private final McpArtifactMetadataPersistenceRepository artifactMetadataPersistenceRepository;

    /**
     * 中文说明：{@code McpArtifactMetadataBO} 与 {@code McpArtifactMetadataRecordPO} 的双向转换器。
     * English summary: The bidirectional converter between McpArtifactMetadataBO and McpArtifactMetadataRecordPO.
     */
    @Qualifier("mcpArtifactMetadataPersistenceConverter")
    private final McpArtifactMetadataPersistenceConverter artifactMetadataPersistenceConverter;

    /**
     * 中文说明：执行 save 操作；等价遗留 {@code INSERT INTO gateway_mcp_app_artifact(...)}，列集合与
     * {@code status = 'ACTIVE'} 字面量逐列沿用（新行由转换器落位），制品编号是调用方生成的十进制雪花标识，
     * 两个 jsonb 集合列经转换器编码；{@code artifact} 必填守护沿用旧文案，受守卫插入未成功时抛
     * {@code IllegalStateException("YUHENG_ADMIN_MCP_ARTIFACT_INSERT_CONFLICT")}。
     * English summary: Executes the save operation; equivalent to the legacy {@code INSERT INTO gateway_mcp_app_artifact(...)}: the
     * column set and the {@code status = 'ACTIVE'} literal carry over column by column (a fresh row is stamped by the converter), the
     * identifier is the caller-generated decimal snowflake key, and the two jsonb collection columns are encoded by the converter; the
     * required {@code artifact} guard keeps the legacy wording and a guarded insert that did not land raises
     * {@code IllegalStateException("YUHENG_ADMIN_MCP_ARTIFACT_INSERT_CONFLICT")}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code mpMcpArtifactMetadataRepository.save(artifactBO)}。
     * @param artifact 参数 制品；parameter artifact.
     */
    @Override
    public void save(McpArtifactMetadataBO artifact) {
        McpArtifactMetadataBO carrier = renormalize(
                Objects.requireNonNull(artifact, "artifact"));
        if (!artifactMetadataPersistenceRepository.save(
                artifactMetadataPersistenceConverter.newRow(carrier))) {
            throw new IllegalStateException(
                    "YUHENG_ADMIN_MCP_ARTIFACT_INSERT_CONFLICT"
            );
        }
    }

    /**
     * 中文说明：执行 find 操作；等价遗留 {@code SELECT ... WHERE id = ? AND status = 'ACTIVE'} 的
     * {@code stream().findFirst()}：迁移后主键是 {@code bigint}，无法表示的文本按旧「等值比较不成立」的结果如实读作空；
     * 命中的行经转换器投影后立即复核 {@code McpArtifactMetadataBO.normalized(...)}。
     * English summary: Executes the find operation; equivalent to the legacy {@code SELECT ... WHERE id = ? AND status = 'ACTIVE'}
     * combined with {@code stream().findFirst()}: the primary key is a {@code bigint} after migration, so a text that cannot be
     * represented truthfully reads as empty like the legacy comparison that never matched, and a hit is projected by the converter and
     * immediately re-checked by {@code McpArtifactMetadataBO.normalized(...)}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code mpMcpArtifactMetadataRepository.find(id)}。
     * @param id 参数 制品Id；parameter artifact id.
     * @return 返回 find 的处理结果；returns the active artifact when one is visible.
     */
    @Override
    public Optional<McpArtifactMetadataBO> find(String id) {
        Long key = columnValue(id);
        if (key == null) {
            return Optional.empty();
        }
        return artifactMetadataPersistenceRepository.list(
                boundPredicate(Wrappers.<McpArtifactMetadataRecordPO>lambdaQuery()
                        .eq(McpArtifactMetadataRecordPO::getId, key)
                        .eq(
                                McpArtifactMetadataRecordPO::getStatus,
                                McpArtifactStatusEnum.ACTIVE.wireValue()
                        ))
        ).stream().findFirst().map(this::carrier);
    }

    /**
     * 中文说明：执行 list 操作；等价遗留 {@code SELECT ... WHERE gateway_group_id = ? AND status = 'ACTIVE'
     * ORDER BY app_code, app_version}：分组标识是 {@code bigint} 外键，无法表示的文本如实读作空集，
     * 追加技术 id 升序只在同应用同版本的并列行上给出稳定次序，每行都复核 {@code normalized(...)}。
     * English summary: Executes the list operation; equivalent to the legacy
     * {@code SELECT ... WHERE gateway_group_id = ? AND status = 'ACTIVE' ORDER BY app_code, app_version}: the group identifier is a
     * {@code bigint} foreign key so an unrepresentable text truthfully reads as the empty set, the appended ascending technical id only
     * stabilizes the order among rows sharing app and version, and every row is re-checked through {@code normalized(...)}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code mpMcpArtifactMetadataRepository.list(gatewayGroupId)}。
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id.
     * @return 返回 list 的处理结果；returns the active artifacts of one Gateway Group in application order.
     */
    @Override
    public List<McpArtifactMetadataBO> list(String gatewayGroupId) {
        Long group = columnValue(gatewayGroupId);
        if (group == null) {
            return List.of();
        }
        return artifactMetadataPersistenceRepository.list(
                boundPredicate(Wrappers.<McpArtifactMetadataRecordPO>lambdaQuery()
                        .eq(McpArtifactMetadataRecordPO::getGatewayGroupId, group)
                        .eq(
                                McpArtifactMetadataRecordPO::getStatus,
                                McpArtifactStatusEnum.ACTIVE.wireValue()
                        )
                        .orderByAsc(McpArtifactMetadataRecordPO::getAppCode)
                        .orderByAsc(McpArtifactMetadataRecordPO::getAppVersion)
                        .orderByAsc(McpArtifactMetadataRecordPO::getId))
        ).stream().map(this::carrier).toList();
    }

    /**
     * 中文说明：执行 revoke 操作；等价遗留 {@code UPDATE ... SET status = 'REVOKED'
     * WHERE id = ? AND status = 'ACTIVE'} 的 {@code == 1} 判定：先按同一组谓词读定位（含技术 {@code version}），
     * 再以「{@code id + status = 'ACTIVE'}」做 CAS 且 {@code SET} 只带 {@code status} 一列，
     * 行不可见、已被撤销或 {@code version} 竞争失败都如实返回 {@code false}，交由调用方按旧文案上报
     * {@code YUHENG_MCP_ARTIFACT_ALREADY_REVOKED}。
     * English summary: Executes the revoke operation; equivalent to the legacy
     * {@code UPDATE ... SET status = 'REVOKED' WHERE id = ? AND status = 'ACTIVE'} judged by {@code == 1}: the row is first located by
     * the very same predicates (carrying its technical {@code version}), then compare-and-set with a {@code SET} carrying
     * {@code status} alone, so an invisible row, an already revoked row and a lost {@code version} race all return {@code false}
     * truthfully and the caller keeps reporting {@code YUHENG_MCP_ARTIFACT_ALREADY_REVOKED} with the legacy wording.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code mpMcpArtifactMetadataRepository.revoke(id)}。
     * @param id 参数 制品Id；parameter artifact id.
     * @return 返回 revoke 的处理结果；returns whether exactly one active row moved to revoked.
     */
    @Override
    public boolean revoke(String id) {
        Long key = columnValue(id);
        if (key == null) {
            return false;
        }
        Optional<McpArtifactMetadataRecordPO> located =
                artifactMetadataPersistenceRepository.list(
                        boundPredicate(revocationPredicate(
                                Wrappers.<McpArtifactMetadataRecordPO>lambdaQuery(), key))
                ).stream().findFirst();
        if (located.isEmpty()) {
            return false;
        }
        McpArtifactMetadataRecordPO row = located.get();
        McpArtifactMetadataRecordPO changed = new McpArtifactMetadataRecordPO();
        changed.setId(row.getId());
        changed.setVersion(row.getVersion());
        changed.setStatus(McpArtifactStatusEnum.REVOKED.wireValue());
        return artifactMetadataPersistenceRepository.update(
                changed,
                boundPredicate(revocationPredicate(
                        Wrappers.<McpArtifactMetadataRecordPO>lambdaUpdate(), key)));
    }

    /**
     * 中文说明：把旧语句的 {@code (id, status = 'ACTIVE')} 撤销谓词挂到任意 lambda 条件上，定位读取与 CAS 写入
     * 共用同一份谓词定义，避免两条路径漂移。
     * English summary: Attaches the legacy {@code (id, status = 'ACTIVE')} revocation predicates to any lambda condition, so the locating
     * read and the compare-and-set write share one predicate definition and cannot drift apart.
     * @param predicate 参数 待挂载的条件；parameter the condition to enrich.
     * @param key 参数 制品主键；parameter the artifact key.
     * @return 返回 同一份挂好业务谓词的条件；returns the very same condition carrying the business predicates.
     */
    private static <W extends com.baomidou.mybatisplus.core.conditions.AbstractLambdaWrapper<
            McpArtifactMetadataRecordPO, W>> W revocationPredicate(
            W predicate,
            Long key) {
        return predicate
                .eq(McpArtifactMetadataRecordPO::getId, key)
                .eq(
                        McpArtifactMetadataRecordPO::getStatus,
                        McpArtifactStatusEnum.ACTIVE.wireValue()
                );
    }

    /**
     * 中文说明：把行模型经转换器投影为业务载体后立即复核旧构造器不变量（读边界）。
     * English summary: Projects a row onto the business carrier through the converter and immediately re-checks the legacy constructor
     * invariants, which is the read boundary.
     * @param row 参数 行模型；parameter the row model.
     * @return 返回 业务载体；returns the business carrier.
     */
    private McpArtifactMetadataBO carrier(McpArtifactMetadataRecordPO row) {
        return renormalize(artifactMetadataPersistenceConverter.toBusiness(row));
    }

    /**
     * 中文说明：执行 renormalize 操作：把已完成列映射的载体逐字段送回
     * {@code McpArtifactMetadataBO.normalized(...)}，因此无论载入还是保存都不存在「未经校验构造
     * {@code McpArtifactMetadataBO}」的路径，必填、摘要长度与制品大小区间守护一律按旧文案生效。
     * English summary: Executes the renormalize operation, feeding an already column-mapped carrier field by field back into
     * {@code McpArtifactMetadataBO.normalized(...)}, so neither loading nor saving can hold an unvalidated
     * {@code McpArtifactMetadataBO} and the required fields, the digest length and the artifact size band guards all apply with the
     * legacy messages.
     * @param carrier 参数 已映射的载体；parameter the mapped carrier.
     * @return 返回 复核后的载体；returns the re-validated carrier.
     */
    private static McpArtifactMetadataBO renormalize(McpArtifactMetadataBO carrier) {
        return McpArtifactMetadataBO.normalized(
                carrier.getId(),
                carrier.getGatewayGroupId(),
                carrier.getAppCode(),
                carrier.getVersion(),
                carrier.getDisplayName(),
                carrier.getResourceUri(),
                carrier.getArtifactReference(),
                carrier.getSha256(),
                carrier.getSizeBytes(),
                carrier.getMimeType(),
                carrier.getContentSecurityPolicy(),
                carrier.getPermissions(),
                carrier.getAllowedOrigins(),
                carrier.getCreatedBy(),
                carrier.getCreatedAt()
        );
    }

    /**
     * 中文说明：把端口上的十进制标识换算为 {@code bigint} 列值；空白、非十进制与非正值都返回 {@code null}，
     * 由调用方按旧「等值比较不成立、读不到行」的结果处理。
     * English summary: Converts the decimal identifier the port carries into the {@code bigint} column value; blank, non-decimal and
     * non-positive values all yield {@code null} so the caller falls back to the legacy outcome of a comparison that never matched.
     * @param opaqueId 参数 标识文本；parameter the identifier text.
     * @return 返回 列值或 {@code null}；returns the column value or {@code null}.
     */
    private static Long columnValue(String opaqueId) {
        if (opaqueId == null || opaqueId.isBlank()) {
            return null;
        }
        try {
            long parsed = Long.parseLong(opaqueId.trim());
            return parsed > 0L ? parsed : null;
        } catch (NumberFormatException invalidIdentifier) {
            return null;
        }
    }

    /**
     * 中文说明：把条件交付受守卫边界之前先成形一次：MyBatis-Plus 的 {@code eq/orderByAsc} 只在 SQL 真正成形时才把取值写进
     * {@code paramNameValuePairs}，故此处先成形一次，让谓词在离开门面时参数已完全绑定；之后（包括 MyBatis 自己下发时）
     * 命中同一份片段缓存，参数与 SQL 都不再改变，列解析失败（lambda 缓存缺失）也如实在门面这一层暴露，
     * 而不是留到语句下发时。
     * English summary: Forms a condition once before it is handed to the guarded boundary: MyBatis-Plus only moves the values of
     * {@code eq/orderByAsc} into {@code paramNameValuePairs} while the SQL is being formed, so forming it here first means the
     * parameters are fully bound when the predicate leaves the facade; later renders (including the one MyBatis performs) hit the same
     * segment cache and change neither the parameters nor the SQL, while a column-resolution failure (a missing lambda cache) surfaces
     * truthfully at the facade instead of at statement time.
     * @param predicate 参数 已构造完成的业务条件；parameter the completed business condition.
     * @return 返回 同一份参数已绑定的条件；returns the very same condition with its parameters bound.
     */
    private static <C extends Wrapper<?>> C boundPredicate(C predicate) {
        predicate.getSqlSegment();
        return predicate;
    }
}
