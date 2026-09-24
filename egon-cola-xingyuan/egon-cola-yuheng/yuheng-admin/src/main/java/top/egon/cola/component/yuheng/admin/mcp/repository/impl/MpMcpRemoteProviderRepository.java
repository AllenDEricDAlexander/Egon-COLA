package top.egon.cola.component.yuheng.admin.mcp.repository.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.mybatis.extension.EgonColaRepository;
import top.egon.cola.component.common.mybatis.model.EgonModel;
import top.egon.cola.component.yuheng.admin.mcp.converter.McpRemoteCapabilityPersistenceConverter;
import top.egon.cola.component.yuheng.admin.mcp.converter.McpRemoteMountDraftPersistenceConverter;
import top.egon.cola.component.yuheng.admin.mcp.converter.McpRemoteProviderDraftPersistenceConverter;
import top.egon.cola.component.yuheng.admin.mcp.domain.bo.McpRemoteCapabilityBO;
import top.egon.cola.component.yuheng.admin.mcp.domain.bo.McpRemoteMountDraftBO;
import top.egon.cola.component.yuheng.admin.mcp.domain.bo.McpRemoteProviderDraftBO;
import top.egon.cola.component.yuheng.admin.mcp.domain.dto.McpRemoteProviderDraftMutationDTO;
import top.egon.cola.component.yuheng.admin.mcp.domain.po.McpRemoteCapabilityRecordPO;
import top.egon.cola.component.yuheng.admin.mcp.domain.po.McpRemoteMountDraftRecordPO;
import top.egon.cola.component.yuheng.admin.mcp.domain.po.McpRemoteProviderDraftRecordPO;
import top.egon.cola.component.yuheng.admin.mcp.repository.McpRemoteProviderRepository;
import top.egon.cola.component.yuheng.admin.mcp.repository.mp.McpRemoteCapabilityPersistenceRepository;
import top.egon.cola.component.yuheng.admin.mcp.repository.mp.McpRemoteMountDraftPersistenceRepository;
import top.egon.cola.component.yuheng.admin.mcp.repository.mp.McpRemoteProviderDraftPersistenceRepository;
import top.egon.cola.component.yuheng.admin.shared.domain.AdminActor;
import top.egon.cola.component.yuheng.admin.shared.domain.exception.GatewayAdminRevisionConflictException;

/**
 * 中文说明：{@code MpMcpRemoteProviderRepository} 是远端 Provider 草稿、远端能力快照与远端挂载草稿的
 * MyBatis-Plus 门面存储，逐方法取代被退役的遗留持久化载体：三张表
 * {@code gateway_mcp_remote_provider}、{@code gateway_mcp_remote_capability}、
 * {@code gateway_mcp_remote_mount_draft} 的每次读写都走各自的受守卫 {@code EgonColaRepository} 边界
 * （同租户过滤、仅活跃行 {@code deleted_at IS NULL}、技术 {@code version} 乐观锁），
 * {@code gateway_group_id}/{@code server_id}/{@code provider_id} 已是 {@code bigint} 外键；
 * {@code providers} 与 {@code mounts} 保留旧投影与 {@code ORDER BY provider_code}、
 * {@code ORDER BY server_id, namespace}，Provider 的 {@code content} 由转换器按旧键序
 * （displayName、dialect、transportType、endpointReference，随后按需追加可空三项，最后 status）重建，
 * 缺席键保持缺席而不是写 {@code null}；{@code saveProvider} 与 {@code saveMount} 保留旧实现
 * 「先按 {@code (id, revision)} 做 UPDATE，未命中再读当前修订决定抛冲突还是以 {@code revision = 0} 插入」
 * 的 upsert 语义与 {@code expectedRevision must not be negative} 守护，UPDATE 绝不回写
 * {@code gateway_group_id}（变更实体上如实留空），Provider 的三个可空引用列缺席时经更新条件显式下推 SQL NULL；
 * {@code capabilities} 保留 {@code WHERE provider_id = ? ORDER BY primitive_type, remote_name}；
 * {@code replaceCapabilities} 沿用旧 {@code @Transactional} 边界，把旧的硬 {@code DELETE} 落为受守卫边界的
 * 版本化软删、逐条受守卫插入，再按旧语句把 {@code capability_fingerprint}、{@code status = 'SYNCED'}
 * 与自增修订推回 Provider 行，且与旧实现一样不判定该语句的影响行数；{@code softDeleteProvider}
 * 与 {@code softDeleteMount} 共用旧实现的同一条按表模板，定位谓词与 {@code expectedRevision + 1} 回报完全一致；
 * 列与类型映射只经三个既有转换器完成，读写两侧的载体一律重新经各端口的 {@code normalized(...)} 复核，
 * 公开端口不泄漏行模型或 DAO。
 *
 * English summary: {@code MpMcpRemoteProviderRepository} is the MyBatis-Plus facade store of remote Provider drafts, remote capability
 * snapshots and remote mount drafts, replacing the retired legacy persistence carrier method by method: every read and write of the three
 * tables {@code gateway_mcp_remote_provider}, {@code gateway_mcp_remote_capability} and {@code gateway_mcp_remote_mount_draft} goes
 * through its own guarded {@code EgonColaRepository} boundary (same-tenant filtering, active rows only under
 * {@code deleted_at IS NULL}, the technical {@code version} optimistic lock) while {@code gateway_group_id}/{@code server_id}/
 * {@code provider_id} are already {@code bigint} foreign keys; {@code providers} and {@code mounts} keep the legacy projections plus
 * {@code ORDER BY provider_code} and {@code ORDER BY server_id, namespace}, and the Provider {@code content} is rebuilt by the converter
 * in the legacy key order (displayName, dialect, transportType, endpointReference, then the three nullable references only when present,
 * then status) so an absent key stays absent instead of being written as {@code null}; {@code saveProvider} and {@code saveMount} keep
 * the legacy upsert order of first UPDATE-ing by {@code (id, revision)} and then either raising a conflict or inserting at
 * {@code revision = 0} depending on the revision read back, together with the {@code expectedRevision must not be negative} guard, the
 * UPDATE never rewrites {@code gateway_group_id} (it stays unset on the change entity) and the three nullable Provider reference columns
 * push an explicit SQL NULL through the update condition when absent; {@code capabilities} keeps
 * {@code WHERE provider_id = ? ORDER BY primitive_type, remote_name}; {@code replaceCapabilities} keeps the legacy
 * {@code @Transactional} boundary, turns the legacy hard {@code DELETE} into the versioned soft delete of the guarded boundary, inserts
 * every capability through that same boundary and then pushes {@code capability_fingerprint}, {@code status = 'SYNCED'} and the
 * incremented revision back onto the Provider row exactly like the legacy statement, including the legacy decision to ignore how many
 * rows that statement affected; {@code softDeleteProvider} and {@code softDeleteMount} share the single per-table template of the legacy
 * implementation with identical locating predicates and the same {@code expectedRevision + 1} report; column and type mapping happens only
 * in the three existing converters, both boundaries re-check carriers through the {@code normalized(...)} factory of their own port, and
 * the public port leaks neither a row model nor a DAO.
 *
 * 用法 / Usage: 通过业务端口 {@code McpRemoteProviderRepository} 由 Spring 容器注入；除 {@code replaceCapabilities}
 * 沿用旧 {@code @Transactional} 之外，其余方法都没有事务边界，「读当前修订—判定冲突—写入」的竞态由调用方事务与
 * 技术 {@code version} 乐观锁共同收敛；冲突一律按旧契约抛 {@code GatewayAdminRevisionConflictException}，
 * 读不到行时携带 {@code -1}。
 * Inject it through the business port {@code McpRemoteProviderRepository}; apart from {@code replaceCapabilities}, which keeps the legacy
 * {@code @Transactional} boundary, no method declares a transaction and the read-current-revision, decide-conflict, write race is收敛 by
 * the caller's transaction together with the technical {@code version} optimistic lock; conflicts always surface as the legacy
 * {@code GatewayAdminRevisionConflictException}, carrying {@code -1} when no row is visible.
 */
@Slf4j
@Repository("mpMcpRemoteProviderRepository")
@RequiredArgsConstructor
@Validated
public class MpMcpRemoteProviderRepository
        implements McpRemoteProviderRepository {

    /**
     * 中文说明：表示 SYNCED 这一固定值，是旧 {@code replaceCapabilities} 语句里
     * {@code status = 'SYNCED'} 的字面量，迁移后仍按 wire 文本下发。
     * English summary: Represents the fixed synced value, the {@code status = 'SYNCED'} literal of the replaced
     * {@code replaceCapabilities} statement, still written as wire text after migration.
     */
    private static final String SYNCED_STATUS = "SYNCED";

    /**
     * 中文说明：Provider 草稿行的受守卫持久化仓储。
     * English summary: The guarded persistence store for remote Provider draft rows.
     */
    @Qualifier("mcpRemoteProviderDraftPersistenceRepository")
    private final McpRemoteProviderDraftPersistenceRepository remoteProviderDraftPersistenceRepository;

    /**
     * 中文说明：远端挂载草稿行的受守卫持久化仓储。
     * English summary: The guarded persistence store for remote mount draft rows.
     */
    @Qualifier("mcpRemoteMountDraftPersistenceRepository")
    private final McpRemoteMountDraftPersistenceRepository remoteMountDraftPersistenceRepository;

    /**
     * 中文说明：远端能力快照行的受守卫持久化仓储。
     * English summary: The guarded persistence store for remote capability snapshot rows.
     */
    @Qualifier("mcpRemoteCapabilityPersistenceRepository")
    private final McpRemoteCapabilityPersistenceRepository remoteCapabilityPersistenceRepository;

    /**
     * 中文说明：{@code McpRemoteProviderDraftBO} 与 {@code McpRemoteProviderDraftRecordPO} 的双向转换器。
     * English summary: The bidirectional converter between McpRemoteProviderDraftBO and McpRemoteProviderDraftRecordPO.
     */
    @Qualifier("mcpRemoteProviderDraftPersistenceConverter")
    private final McpRemoteProviderDraftPersistenceConverter remoteProviderDraftPersistenceConverter;

    /**
     * 中文说明：{@code McpRemoteMountDraftBO} 与 {@code McpRemoteMountDraftRecordPO} 的双向转换器。
     * English summary: The bidirectional converter between McpRemoteMountDraftBO and McpRemoteMountDraftRecordPO.
     */
    @Qualifier("mcpRemoteMountDraftPersistenceConverter")
    private final McpRemoteMountDraftPersistenceConverter remoteMountDraftPersistenceConverter;

    /**
     * 中文说明：{@code McpRemoteCapabilityBO} 与 {@code McpRemoteCapabilityRecordPO} 的双向转换器。
     * English summary: The bidirectional converter between McpRemoteCapabilityBO and McpRemoteCapabilityRecordPO.
     */
    @Qualifier("mcpRemoteCapabilityPersistenceConverter")
    private final McpRemoteCapabilityPersistenceConverter remoteCapabilityPersistenceConverter;

    /**
     * 中文说明：执行 providers 操作；等价遗留 {@code SELECT ... WHERE gateway_group_id = ? AND deleted = FALSE
     * ORDER BY provider_code}：旧实现未对该标识做必填守护，迁移后该列是 {@code bigint} 外键，
     * 无法表示的文本按旧「等值比较不成立」的结果如实读为空集，追加技术 id 升序保证同编码行的稳定次序；
     * 每行的 {@code content} 由转换器按旧键序重建后再复核 {@code McpRemoteProviderDraftBO.normalized(...)}。
     * English summary: Executes the providers operation; equivalent to the legacy
     * {@code SELECT ... WHERE gateway_group_id = ? AND deleted = FALSE ORDER BY provider_code}: the legacy implementation applied no
     * required guard to that identifier, the column became a {@code bigint} foreign key after migration so a text that cannot be
     * represented truthfully reads as the empty set - the legacy comparison never matched - and an appended ascending technical id keeps a
     * stable order among rows sharing a code; every row rebuilds its {@code content} through the converter in the legacy key order and is
     * then re-checked by {@code McpRemoteProviderDraftBO.normalized(...)}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code mpMcpRemoteProviderRepository.providers(gatewayGroupId)}。
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id.
     * @return 返回 providers 的处理结果；returns the Provider drafts of one Gateway Group in code order.
     */
    @Override
    public List<McpRemoteProviderDraftBO> providers(String gatewayGroupId) {
        Long group = columnValue(gatewayGroupId);
        if (group == null) {
            return List.of();
        }
        return remoteProviderDraftPersistenceRepository.list(
                boundPredicate(Wrappers.<McpRemoteProviderDraftRecordPO>lambdaQuery()
                        .eq(
                                McpRemoteProviderDraftRecordPO::getGatewayGroupId,
                                group
                        )
                        .orderByAsc(McpRemoteProviderDraftRecordPO::getProviderCode)
                        .orderByAsc(McpRemoteProviderDraftRecordPO::getId))
        ).stream().map(this::provider).toList();
    }

    /**
     * 中文说明：执行 saveProvider 操作；等价遗留「UPDATE ... SET provider_code, display_name, dialect,
     * transport_type, endpoint_reference, auth_profile_reference, tls_profile_reference,
     * capability_fingerprint, status, enabled, revision = revision + 1, updated_at, updated_by
     * WHERE id = ? AND revision = ? AND deleted = FALSE」，命中一行即按 {@code expectedRevision + 1} 返回；
     * 否则读回当前修订，存在修订或期望修订非 0 时抛 {@code GatewayAdminRevisionConflictException}，
     * 只有「无行且期望修订为 0」才以 {@code revision = 0} 受守卫插入并返回 0；
     * 三个可空引用列缺席时经更新条件显式下推 SQL NULL，{@code gateway_group_id} 与旧语句一致绝不被回写。
     * English summary: Executes the saveProvider operation; equivalent to the legacy
     * {@code UPDATE ... SET provider_code, display_name, dialect, transport_type, endpoint_reference, auth_profile_reference,
     * tls_profile_reference, capability_fingerprint, status, enabled, revision = revision + 1, updated_at, updated_by WHERE id = ? AND
     * revision = ? AND deleted = FALSE}: one affected row returns {@code expectedRevision + 1}; otherwise the current revision is read
     * back, a stored revision or a non-zero expected revision raises the legacy {@code GatewayAdminRevisionConflictException}, and only
     * "no row plus expected revision 0" inserts through the guarded boundary at {@code revision = 0} and returns 0; the three nullable
     * reference columns push an explicit SQL NULL through the update condition when absent and {@code gateway_group_id} is never
     * rewritten, exactly like the legacy statement.
     *
     * 用法 / Usage: 调用方式 / Usage:
     * {@code mpMcpRemoteProviderRepository.saveProvider(providerBO, expectedRevision, actor, now)}。
     * @param provider 参数 provider；parameter provider.
     * @param expectedRevision 参数 expectedRevision；parameter expected revision.
     * @param actor 参数 actor；parameter actor.
     * @param now 参数 now；parameter now.
     * @return 返回 saveProvider 的处理结果；returns the mutated Provider identifier and its new revision.
     */
    @Override
    public McpRemoteProviderDraftMutationDTO saveProvider(
            McpRemoteProviderDraftBO provider,
            long expectedRevision,
            AdminActor actor,
            Instant now) {
        Objects.requireNonNull(provider, "provider");
        validateExpectedRevision(expectedRevision);
        McpRemoteProviderDraftBO carrier = renormalize(provider);
        Objects.requireNonNull(now, "now");
        Objects.requireNonNull(actor, "actor");
        Long key = columnValue(carrier.getId());
        Optional<McpRemoteProviderDraftRecordPO> located = key == null
                ? Optional.empty()
                : remoteProviderDraftPersistenceRepository.list(
                        boundPredicate(keyPredicate(
                                Wrappers.<McpRemoteProviderDraftRecordPO>lambdaQuery(),
                                key,
                                expectedRevision))
                ).stream().findFirst();
        if (located.isPresent()) {
            McpRemoteProviderDraftRecordPO row = located.get();
            McpRemoteProviderDraftRecordPO changed =
                    remoteProviderDraftPersistenceConverter.toPersistence(carrier);
            changed.setId(row.getId());
            changed.setVersion(row.getVersion());
            changed.setRevision(expectedRevision + 1L);
            changed.setGatewayGroupId(null);
            if (remoteProviderDraftPersistenceRepository.update(
                    changed,
                    boundPredicate(keyPredicate(
                            Wrappers.<McpRemoteProviderDraftRecordPO>lambdaUpdate(),
                            row.getId(),
                            expectedRevision)
                            .set(
                                    changed.getAuthProfileReference() == null,
                                    McpRemoteProviderDraftRecordPO::getAuthProfileReference,
                                    null
                            )
                            .set(
                                    changed.getTlsProfileReference() == null,
                                    McpRemoteProviderDraftRecordPO::getTlsProfileReference,
                                    null
                            )
                            .set(
                                    changed.getCapabilityFingerprint() == null,
                                    McpRemoteProviderDraftRecordPO::getCapabilityFingerprint,
                                    null
                            ))
            )) {
                return new McpRemoteProviderDraftMutationDTO(
                        carrier.getId(),
                        expectedRevision + 1
                );
            }
        }
        Long current = currentRevision(
                remoteProviderDraftPersistenceRepository,
                McpRemoteProviderDraftRecordPO::getId,
                McpRemoteProviderDraftRecordPO::getRevision,
                key
        );
        if (current != null || expectedRevision != 0) {
            throw revisionConflict(current);
        }
        McpRemoteProviderDraftRecordPO fresh =
                remoteProviderDraftPersistenceConverter.newRow(carrier);
        fresh.setRevision(0L);
        if (!remoteProviderDraftPersistenceRepository.save(fresh)) {
            throw new IllegalStateException(
                    "YUHENG_ADMIN_MCP_REMOTE_PROVIDER_INSERT_CONFLICT"
            );
        }
        return new McpRemoteProviderDraftMutationDTO(carrier.getId(), 0);
    }

    /**
     * 中文说明：执行 capabilities 操作；等价遗留 {@code SELECT ... WHERE provider_id = ?
     * ORDER BY primitive_type, remote_name}：该语句从未按 {@code deleted} 过滤，受守卫边界只为活跃行开口，
     * 故快照替换后的历史行不再可见；无法表示的 Provider 标识按旧「等值比较不成立」如实读为空集，
     * 投影由既有转换器完成（该载体没有 {@code normalized(...)} 工厂）。
     * English summary: Executes the capabilities operation; equivalent to the legacy
     * {@code SELECT ... WHERE provider_id = ? ORDER BY primitive_type, remote_name}: that statement never filtered on
     * {@code deleted} while the guarded boundary only speaks for active rows, so a superseded snapshot row is no longer visible, an
     * unrepresentable Provider identifier truthfully reads as the empty set like the comparison that never matched, and the projection
     * runs in the existing converter because this carrier declares no {@code normalized(...)} factory.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code mpMcpRemoteProviderRepository.capabilities(providerId)}。
     * @param providerId 参数 providerId；parameter provider id.
     * @return 返回 capabilities 的处理结果；returns the capability snapshot of one Provider in primitive and remote name order.
     */
    @Override
    public List<McpRemoteCapabilityBO> capabilities(String providerId) {
        Long provider = columnValue(providerId);
        if (provider == null) {
            return List.of();
        }
        return remoteCapabilityPersistenceRepository.list(
                boundPredicate(Wrappers.<McpRemoteCapabilityRecordPO>lambdaQuery()
                        .eq(McpRemoteCapabilityRecordPO::getProviderId, provider)
                        .orderByAsc(McpRemoteCapabilityRecordPO::getPrimitiveType)
                        .orderByAsc(McpRemoteCapabilityRecordPO::getRemoteName)
                        .orderByAsc(McpRemoteCapabilityRecordPO::getId))
        ).stream()
                .map(remoteCapabilityPersistenceConverter::toBusiness)
                .toList();
    }

    /**
     * 中文说明：执行 replaceCapabilities 操作；沿用旧实现的 {@code @Transactional} 边界，三步与旧语句一一对应：
     * 先按 {@code provider_id} 读定位该 Provider 的既有快照并逐行走受守卫边界的版本化软删（旧语句是硬
     * {@code DELETE}，软删行对所有活跃读取不可见，效果一致），再按入参顺序逐条受守卫插入——
     * {@code provider_id}、{@code capability_fingerprint} 与 {@code synced_at} 一律取方法参数而非载体值，
     * 与被替换语句一致，插入未成功时按所属表上报 {@code GATEWAY_MCP_REMOTE_CAPABILITY_INSERT_CONFLICT}；
     * 最后把指纹、{@code SYNCED} 状态与自增修订推回 Provider 行，且与旧实现一样完全不判定该语句的影响行数。
     * English summary: Executes the replaceCapabilities operation, keeping the {@code @Transactional} boundary of the legacy
     * implementation with the same three steps: the snapshot rows of the Provider are located by {@code provider_id} and each one is
     * handed to the versioned soft delete of the guarded boundary - the replaced statement was a hard {@code DELETE} and a soft-deleted
     * row is invisible to every active read, so the outcome matches - then every capability is inserted through that same boundary in
     * argument order with {@code provider_id}, {@code capability_fingerprint} and {@code synced_at} taken from the method arguments
     * rather than the carrier, exactly like the replaced statement, and a missed insert reports
     * {@code GATEWAY_MCP_REMOTE_CAPABILITY_INSERT_CONFLICT} for its own table; finally the fingerprint, the {@code SYNCED} status and the
     * incremented revision are pushed back onto the Provider row while the number of rows that statement affected stays as unexamined as
     * it was in the legacy code.
     *
     * 用法 / Usage: 调用方式 / Usage:
     * {@code mpMcpRemoteProviderRepository.replaceCapabilities(providerId, fingerprint, capabilityBOList, syncedAt)}。
     * @param providerId 参数 providerId；parameter provider id.
     * @param fingerprint 参数 fingerprint；parameter fingerprint.
     * @param capabilities 参数 capabilities；parameter capabilities.
     * @param syncedAt 参数 syncedAt；parameter synced at.
     */
    @Transactional
    @Override
    public void replaceCapabilities(
            String providerId,
            String fingerprint,
            List<McpRemoteCapabilityBO> capabilities,
            Instant syncedAt) {
        Long provider = numericKey(providerId, "providerId");
        for (McpRemoteCapabilityRecordPO superseded
                : remoteCapabilityPersistenceRepository.list(
                        boundPredicate(Wrappers.<McpRemoteCapabilityRecordPO>lambdaQuery()
                                .eq(McpRemoteCapabilityRecordPO::getProviderId, provider))
                )) {
            remoteCapabilityPersistenceRepository.removeById(superseded);
        }
        for (McpRemoteCapabilityBO capability : capabilities) {
            McpRemoteCapabilityRecordPO fresh =
                    remoteCapabilityPersistenceConverter.newRow(capability);
            fresh.setProviderId(provider);
            fresh.setCapabilityFingerprint(fingerprint);
            fresh.setSyncedAt(syncedAt);
            if (!remoteCapabilityPersistenceRepository.save(fresh)) {
                throw new IllegalStateException(
                        "GATEWAY_MCP_REMOTE_CAPABILITY_INSERT_CONFLICT"
                );
            }
        }
        Optional<McpRemoteProviderDraftRecordPO> target =
                remoteProviderDraftPersistenceRepository.list(
                        boundPredicate(Wrappers.<McpRemoteProviderDraftRecordPO>lambdaQuery()
                                .eq(McpRemoteProviderDraftRecordPO::getId, provider))
                ).stream().findFirst();
        if (target.isEmpty()) {
            return;
        }
        McpRemoteProviderDraftRecordPO row = target.get();
        McpRemoteProviderDraftRecordPO synced = new McpRemoteProviderDraftRecordPO();
        synced.setId(row.getId());
        synced.setVersion(row.getVersion());
        synced.setCapabilityFingerprint(fingerprint);
        synced.setStatus(SYNCED_STATUS);
        synced.setRevision(row.getRevision() == null ? 1L : row.getRevision() + 1L);
        remoteProviderDraftPersistenceRepository.update(
                synced,
                boundPredicate(Wrappers.<McpRemoteProviderDraftRecordPO>lambdaUpdate()
                        .eq(McpRemoteProviderDraftRecordPO::getId, row.getId())
                        .set(
                                fingerprint == null,
                                McpRemoteProviderDraftRecordPO::getCapabilityFingerprint,
                                null
                        ))
        );
    }

    /**
     * 中文说明：执行 mounts 操作；等价遗留 {@code SELECT ... WHERE gateway_group_id = ? AND deleted = FALSE
     * ORDER BY server_id, namespace}：迁移后分组列是 {@code bigint} 外键，无法表示的文本如实读为空集，
     * 追加技术 id 升序保证同序时的稳定次序，每行都复核 {@code McpRemoteMountDraftBO.normalized(...)}。
     * English summary: Executes the mounts operation; equivalent to the legacy
     * {@code SELECT ... WHERE gateway_group_id = ? AND deleted = FALSE ORDER BY server_id, namespace}: the group column is a
     * {@code bigint} foreign key after migration so an unrepresentable text truthfully reads as the empty set, an appended ascending
     * technical id keeps a stable order among tied rows, and every row is re-checked by
     * {@code McpRemoteMountDraftBO.normalized(...)}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code mpMcpRemoteProviderRepository.mounts(gatewayGroupId)}。
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id.
     * @return 返回 mounts 的处理结果；returns the mount drafts of one Gateway Group in server and namespace order.
     */
    @Override
    public List<McpRemoteMountDraftBO> mounts(String gatewayGroupId) {
        Long group = columnValue(gatewayGroupId);
        if (group == null) {
            return List.of();
        }
        return remoteMountDraftPersistenceRepository.list(
                boundPredicate(Wrappers.<McpRemoteMountDraftRecordPO>lambdaQuery()
                        .eq(McpRemoteMountDraftRecordPO::getGatewayGroupId, group)
                        .orderByAsc(McpRemoteMountDraftRecordPO::getServerId)
                        .orderByAsc(McpRemoteMountDraftRecordPO::getNamespace)
                        .orderByAsc(McpRemoteMountDraftRecordPO::getId))
        ).stream().map(this::mount).toList();
    }

    /**
     * 中文说明：执行 saveMount 操作；等价遗留「UPDATE gateway_mcp_remote_mount_draft SET server_id,
     * provider_id, namespace, capability_fingerprint, content, enabled, revision = revision + 1,
     * updated_at, updated_by WHERE id = ? AND revision = ? AND deleted = FALSE」，命中即按
     * {@code expectedRevision + 1} 返回，否则读回当前修订并据其决定冲突或以 {@code revision = 0} 插入；
     * 挂载行的六列在端口上都是必填值，故无需下推任何 SQL NULL，{@code gateway_group_id} 与旧语句一致不被回写。
     * English summary: Executes the saveMount operation; equivalent to the legacy
     * {@code UPDATE gateway_mcp_remote_mount_draft SET server_id, provider_id, namespace, capability_fingerprint, content, enabled,
     * revision = revision + 1, updated_at, updated_by WHERE id = ? AND revision = ? AND deleted = FALSE}: a hit returns
     * {@code expectedRevision + 1}, otherwise the current revision is read back and decides between a conflict and an insert at
     * {@code revision = 0}; all six columns of a mount row are required on the port, so no SQL NULL has to be pushed, and
     * {@code gateway_group_id} is never rewritten, exactly like the legacy statement.
     *
     * 用法 / Usage: 调用方式 / Usage:
     * {@code mpMcpRemoteProviderRepository.saveMount(mountBO, expectedRevision, actor, now)}。
     * @param mount 参数 挂载；parameter mount.
     * @param expectedRevision 参数 expectedRevision；parameter expected revision.
     * @param actor 参数 actor；parameter actor.
     * @param now 参数 now；parameter now.
     * @return 返回 saveMount 的处理结果；returns the mutated mount identifier and its new revision.
     */
    @Override
    public McpRemoteProviderDraftMutationDTO saveMount(
            McpRemoteMountDraftBO mount,
            long expectedRevision,
            AdminActor actor,
            Instant now) {
        Objects.requireNonNull(mount, "mount");
        validateExpectedRevision(expectedRevision);
        McpRemoteMountDraftBO carrier = renormalize(mount);
        Objects.requireNonNull(now, "now");
        Objects.requireNonNull(actor, "actor");
        Long key = columnValue(carrier.getId());
        Optional<McpRemoteMountDraftRecordPO> located = key == null
                ? Optional.empty()
                : remoteMountDraftPersistenceRepository.list(
                        boundPredicate(mountKeyPredicate(
                                Wrappers.<McpRemoteMountDraftRecordPO>lambdaQuery(),
                                key,
                                expectedRevision))
                ).stream().findFirst();
        if (located.isPresent()) {
            McpRemoteMountDraftRecordPO row = located.get();
            McpRemoteMountDraftRecordPO changed =
                    remoteMountDraftPersistenceConverter.toPersistence(carrier);
            changed.setId(row.getId());
            changed.setVersion(row.getVersion());
            changed.setRevision(expectedRevision + 1L);
            changed.setGatewayGroupId(null);
            if (remoteMountDraftPersistenceRepository.update(
                    changed,
                    boundPredicate(mountKeyPredicate(
                            Wrappers.<McpRemoteMountDraftRecordPO>lambdaUpdate(),
                            row.getId(),
                            expectedRevision))
            )) {
                return new McpRemoteProviderDraftMutationDTO(
                        carrier.getId(),
                        expectedRevision + 1
                );
            }
        }
        Long current = currentRevision(
                remoteMountDraftPersistenceRepository,
                McpRemoteMountDraftRecordPO::getId,
                McpRemoteMountDraftRecordPO::getRevision,
                key
        );
        if (current != null || expectedRevision != 0) {
            throw revisionConflict(current);
        }
        McpRemoteMountDraftRecordPO fresh =
                remoteMountDraftPersistenceConverter.newRow(carrier);
        fresh.setRevision(0L);
        if (!remoteMountDraftPersistenceRepository.save(fresh)) {
            throw new IllegalStateException(
                    "YUHENG_ADMIN_MCP_REMOTE_MOUNT_INSERT_CONFLICT"
            );
        }
        return new McpRemoteProviderDraftMutationDTO(carrier.getId(), 0);
    }

    /**
     * 中文说明：执行 softDeleteProvider 操作；与 {@code softDeleteMount} 共用旧实现的同一条按表模板，
     * 语义为「按 {@code (id, revision)} 命中一行才软删并把修订按 {@code expectedRevision + 1} 回报」，
     * 未命中时读回当前修订并抛 {@code GatewayAdminRevisionConflictException}（读不到行为 {@code -1}）；
     * 受守卫边界只提供版本化软删，故旧语句同时写下的 {@code enabled = FALSE} 与自增修订不再有意义。
     * English summary: Executes the softDeleteProvider operation, sharing the single per-table template of the legacy implementation with
     * {@code softDeleteMount}: a row is soft-deleted only when {@code (id, revision)} matches, and the revision is then reported as
     * {@code expectedRevision + 1}, while a miss reads the current revision back and raises
     * {@code GatewayAdminRevisionConflictException} ({@code -1} when nothing is visible); the guarded boundary only offers a versioned
     * soft delete, so the {@code enabled = FALSE} and the incremented revision that the legacy statement wrote in the same breath lose
     * their meaning.
     *
     * 用法 / Usage: 调用方式 / Usage:
     * {@code mpMcpRemoteProviderRepository.softDeleteProvider(id, expectedRevision, actor, now)}。
     * @param id 参数 草稿Id；parameter draft id.
     * @param expectedRevision 参数 expectedRevision；parameter expected revision.
     * @param actor 参数 actor；parameter actor.
     * @param now 参数 now；parameter now.
     * @return 返回 softDeleteProvider 的处理结果；returns the soft-deleted Provider identifier and its new revision.
     */
    @Override
    public McpRemoteProviderDraftMutationDTO softDeleteProvider(
            String id,
            long expectedRevision,
            AdminActor actor,
            Instant now) {
        validateExpectedRevision(expectedRevision);
        Objects.requireNonNull(now, "now");
        Objects.requireNonNull(actor, "actor");
        Long key = columnValue(id);
        return removeRow(
                remoteProviderDraftPersistenceRepository,
                McpRemoteProviderDraftRecordPO::getId,
                McpRemoteProviderDraftRecordPO::getRevision,
                key,
                id,
                expectedRevision
        );
    }

    /**
     * 中文说明：执行 softDeleteMount 操作；与 {@code softDeleteProvider} 共用同一条受守卫软删骨架，
     * 守护顺序与旧模板一致（期望修订非负、{@code now} 与 {@code actor} 必填）。
     * English summary: Executes the softDeleteMount operation on the very same guarded soft-delete skeleton as
     * {@code softDeleteProvider}, keeping the guard order of the legacy template (a non-negative expected revision, then required
     * {@code now} and {@code actor}).
     *
     * 用法 / Usage: 调用方式 / Usage:
     * {@code mpMcpRemoteProviderRepository.softDeleteMount(id, expectedRevision, actor, now)}。
     * @param id 参数 草稿Id；parameter draft id.
     * @param expectedRevision 参数 expectedRevision；parameter expected revision.
     * @param actor 参数 actor；parameter actor.
     * @param now 参数 now；parameter now.
     * @return 返回 softDeleteMount 的处理结果；returns the soft-deleted mount identifier and its new revision.
     */
    @Override
    public McpRemoteProviderDraftMutationDTO softDeleteMount(
            String id,
            long expectedRevision,
            AdminActor actor,
            Instant now) {
        validateExpectedRevision(expectedRevision);
        Objects.requireNonNull(now, "now");
        Objects.requireNonNull(actor, "actor");
        Long key = columnValue(id);
        return removeRow(
                remoteMountDraftPersistenceRepository,
                McpRemoteMountDraftRecordPO::getId,
                McpRemoteMountDraftRecordPO::getRevision,
                key,
                id,
                expectedRevision
        );
    }

    /**
     * 中文说明：Provider 与挂载共用的受守卫软删骨架：按 {@code (id, revision)} 读定位后交给
     * 边界的版本化软删，行缺失、已被软删或 {@code version} 竞争失败都按旧契约回读当前修订并抛冲突。
     * English summary: The guarded soft-delete skeleton shared by Provider and mount rows: a row located by {@code (id, revision)} is
     * handed to the versioned soft delete of the boundary, and a missing row, an already soft-deleted row or a lost {@code version} race
     * all read the current revision back and raise the legacy conflict.
     * @param store 参数 受守卫仓储；parameter the guarded store.
     * @param idColumn 参数 技术主键列；parameter the technical key column.
     * @param revisionColumn 参数 业务修订列；parameter the business revision column.
     * @param key 参数 草稿主键或 {@code null}；parameter the draft key or {@code null}.
     * @param id 参数 端口标识；parameter the port identifier.
     * @param expectedRevision 参数 期望修订；parameter the expected revision.
     * @return 返回 变更结果；returns the mutation outcome.
     */
    private <P extends EgonModel<P>> McpRemoteProviderDraftMutationDTO removeRow(
            EgonColaRepository<?, P> store,
            SFunction<P, ?> idColumn,
            SFunction<P, ?> revisionColumn,
            Long key,
            String id,
            long expectedRevision) {
        Optional<P> located = key == null
                ? Optional.empty()
                : store.list(boundPredicate(Wrappers.<P>lambdaQuery()
                        .eq(idColumn, key)
                        .eq(revisionColumn, expectedRevision))
                ).stream().findFirst();
        if (located.isPresent() && store.removeById(located.get())) {
            return new McpRemoteProviderDraftMutationDTO(
                    id,
                    expectedRevision + 1
            );
        }
        throw revisionConflict(
                currentRevision(store, idColumn, revisionColumn, key)
        );
    }

    /**
     * 中文说明：执行 currentRevision 操作；等价旧 {@code SELECT revision FROM <table> WHERE id = ?} 的
     * {@code findFirst().orElse(null)}，行不存在（含被软删或属于其它租户）时如实返回 {@code null}。
     * English summary: Executes the currentRevision operation; equivalent to the legacy {@code SELECT revision FROM <table> WHERE id = ?}
     * combined with {@code findFirst().orElse(null)}, truthfully returning {@code null} when no row exists - including one soft-deleted
     * or owned by another tenant.
     * @param store 参数 受守卫仓储；parameter the guarded store.
     * @param idColumn 参数 技术主键列；parameter the technical key column.
     * @param revisionColumn 参数 业务修订列；parameter the business revision column.
     * @param key 参数 草稿主键或 {@code null}；parameter the draft key or {@code null}.
     * @return 返回 当前修订或 {@code null}；returns the stored revision or {@code null}.
     */
    private <P extends EgonModel<P>> Long currentRevision(
            EgonColaRepository<?, P> store,
            SFunction<P, ?> idColumn,
            SFunction<P, ?> revisionColumn,
            Long key) {
        if (key == null) {
            return null;
        }
        return store.list(boundPredicate(Wrappers.<P>lambdaQuery()
                .eq(idColumn, key))
        ).stream().findFirst()
                .map(row -> (Long) revisionColumn.apply(row))
                .orElse(null);
    }

    /**
     * 中文说明：把 Provider 行经转换器投影为业务载体后立即复核旧构造器不变量（读边界）。
     * English summary: Projects a Provider row onto the business carrier through the converter and immediately re-checks the legacy
     * constructor invariants, which is the read boundary.
     * @param row 参数 行模型；parameter the row model.
     * @return 返回 业务载体；returns the business carrier.
     */
    private McpRemoteProviderDraftBO provider(McpRemoteProviderDraftRecordPO row) {
        return renormalize(remoteProviderDraftPersistenceConverter.toBusiness(row));
    }

    /**
     * 中文说明：把挂载行经转换器投影为业务载体后立即复核旧构造器不变量（读边界）。
     * English summary: Projects a mount row onto the business carrier through the converter and immediately re-checks the legacy
     * constructor invariants, which is the read boundary.
     * @param row 参数 行模型；parameter the row model.
     * @return 返回 业务载体；returns the business carrier.
     */
    private McpRemoteMountDraftBO mount(McpRemoteMountDraftRecordPO row) {
        return renormalize(remoteMountDraftPersistenceConverter.toBusiness(row));
    }

    /**
     * 中文说明：执行 renormalize 操作：把已完成列映射的 Provider 载体逐字段送回
     * {@code McpRemoteProviderDraftBO.normalized(...)}，因此载入路径不可能持有未经校验的载体。
     * English summary: Executes the renormalize operation, feeding an already column-mapped Provider carrier field by field back into
     * {@code McpRemoteProviderDraftBO.normalized(...)}, so no load path can hold an unvalidated carrier.
     * @param carrier 参数 已映射的载体；parameter the mapped carrier.
     * @return 返回 复核后的载体；returns the re-validated carrier.
     */
    private static McpRemoteProviderDraftBO renormalize(McpRemoteProviderDraftBO carrier) {
        return McpRemoteProviderDraftBO.normalized(
                carrier.getId(),
                carrier.getGatewayGroupId(),
                carrier.getProviderCode(),
                carrier.getContent(),
                carrier.isEnabled(),
                carrier.getRevision()
        );
    }

    /**
     * 中文说明：执行 renormalize 操作：把已完成列映射的挂载载体逐字段送回
     * {@code McpRemoteMountDraftBO.normalized(...)}，六个必填守护一律按旧文案生效。
     * English summary: Executes the renormalize operation, feeding an already column-mapped mount carrier field by field back into
     * {@code McpRemoteMountDraftBO.normalized(...)}, so all six required guards apply with the legacy messages.
     * @param carrier 参数 已映射的载体；parameter the mapped carrier.
     * @return 返回 复核后的载体；returns the re-validated carrier.
     */
    private static McpRemoteMountDraftBO renormalize(McpRemoteMountDraftBO carrier) {
        return McpRemoteMountDraftBO.normalized(
                carrier.getId(),
                carrier.getGatewayGroupId(),
                carrier.getServerId(),
                carrier.getProviderId(),
                carrier.getNamespace(),
                carrier.getCapabilityFingerprint(),
                carrier.getContent(),
                carrier.isEnabled(),
                carrier.getRevision()
        );
    }

    /**
     * 中文说明：沿用旧实现的期望修订守护：负数 {@code expectedRevision} 抛
     * {@code IllegalArgumentException("expectedRevision must not be negative")}。
     * English summary: Keeps the legacy expected-revision guard: a negative {@code expectedRevision} raises
     * {@code IllegalArgumentException("expectedRevision must not be negative")}.
     * @param expectedRevision 参数 期望修订；parameter the expected revision.
     */
    private static void validateExpectedRevision(long expectedRevision) {
        if (expectedRevision < 0) {
            throw new IllegalArgumentException(
                    "expectedRevision must not be negative"
            );
        }
    }

    /**
     * 中文说明：执行 revisionConflict 操作，逐字沿用旧实现：读不到当前修订时以 {@code -1} 上报冲突。
     * English summary: Executes the revisionConflict operation, keeping the legacy shape word for word: an unreadable current revision is
     * reported as {@code -1}.
     * @param currentRevision 参数 当前修订；parameter the current revision.
     * @return 返回 冲突异常；returns the conflict failure.
     */
    private static GatewayAdminRevisionConflictException revisionConflict(Long currentRevision) {
        return new GatewayAdminRevisionConflictException(
                currentRevision == null ? -1 : currentRevision
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
     * 中文说明：把写入侧必需的 Provider 外键换算为 {@code bigint} 列值；迁移后该外键是数值列，
     * 无法表示的标识按调用方契约错误抛出，而不是写出 {@code NULL} 冒充外键。
     * English summary: Converts the write-side Provider foreign key into the {@code bigint} column value; after migration that foreign
     * key is numeric, so an unrepresentable identifier is raised as a caller contract error instead of writing {@code NULL} and
     * pretending the key held.
     * @param opaqueId 参数 标识文本；parameter the identifier text.
     * @param field 参数 字段名；parameter the field name.
     * @return 返回 列值；returns the column value.
     */
    private static Long numericKey(String opaqueId, String field) {
        Long column = columnValue(opaqueId);
        if (column == null) {
            throw new IllegalArgumentException(
                    field + " must be a numeric identifier"
            );
        }
        return column;
    }

    /**
     * 中文说明：把旧语句的 {@code (id, revision)} 主键谓词挂到 Provider 行的任意 lambda 条件上，
     * 定位读取与 CAS 写入共用同一份谓词定义。
     * English summary: Attaches the legacy {@code (id, revision)} primary-key predicates to any lambda condition of a Provider row, so the
     * locating read and the compare-and-set write share one predicate definition.
     * @param predicate 参数 待挂载的条件；parameter the condition to enrich.
     * @param key 参数 Provider 主键；parameter the Provider key.
     * @param expectedRevision 参数 期望修订；parameter the expected revision.
     * @return 返回 同一份挂好业务谓词的条件；returns the very same condition carrying the business predicates.
     */
    private static <W extends com.baomidou.mybatisplus.core.conditions.AbstractLambdaWrapper<
            McpRemoteProviderDraftRecordPO, W>> W keyPredicate(
            W predicate,
            Long key,
            long expectedRevision) {
        return predicate
                .eq(McpRemoteProviderDraftRecordPO::getId, key)
                .eq(McpRemoteProviderDraftRecordPO::getRevision, expectedRevision);
    }

    /**
     * 中文说明：把旧语句的 {@code (id, revision)} 主键谓词挂到挂载行的任意 lambda 条件上，
     * 定位读取与 CAS 写入共用同一份谓词定义。
     * English summary: Attaches the legacy {@code (id, revision)} primary-key predicates to any lambda condition of a mount row, so the
     * locating read and the compare-and-set write share one predicate definition.
     * @param predicate 参数 待挂载的条件；parameter the condition to enrich.
     * @param key 参数 挂载主键；parameter the mount key.
     * @param expectedRevision 参数 期望修订；parameter the expected revision.
     * @return 返回 同一份挂好业务谓词的条件；returns the very same condition carrying the business predicates.
     */
    private static <W extends com.baomidou.mybatisplus.core.conditions.AbstractLambdaWrapper<
            McpRemoteMountDraftRecordPO, W>> W mountKeyPredicate(
            W predicate,
            Long key,
            long expectedRevision) {
        return predicate
                .eq(McpRemoteMountDraftRecordPO::getId, key)
                .eq(McpRemoteMountDraftRecordPO::getRevision, expectedRevision);
    }

    /**
     * 中文说明：把条件交付受守卫边界之前先成形一次：MyBatis-Plus 的 {@code eq/orderByAsc} 只在 SQL 真正成形时才把取值写进
     * {@code paramNameValuePairs}，故此处先成形一次，让谓词在离开门面时参数已完全绑定；之后（包括 MyBatis 自己下发时）
     * 命中同一份片段缓存，参数与 SQL 都不再改变，列解析失败（lambda 缓存缺失）也如实在门面这一层暴露，
     * 而不是留到语句下发时。
     * English summary: Forms a condition once before it is handed to the guarded boundary: MyBatis-Plus only moves the values of
     * {@code eq/orderByAsc} into {@code paramNameValuePairs} while the SQL is being formed, so forming it here first means the parameters
     * are fully bound when the predicate leaves the facade; later renders (including the one MyBatis performs) hit the same segment cache
     * and change neither the parameters nor the SQL, while a column-resolution failure (a missing lambda cache) surfaces truthfully at the
     * facade instead of at statement time.
     * @param predicate 参数 已构造完成的业务条件；parameter the completed business condition.
     * @return 返回 同一份参数已绑定的条件；returns the very same condition with its parameters bound.
     */
    private static <C extends Wrapper<?>> C boundPredicate(C predicate) {
        predicate.getSqlSegment();
        return predicate;
    }
}
