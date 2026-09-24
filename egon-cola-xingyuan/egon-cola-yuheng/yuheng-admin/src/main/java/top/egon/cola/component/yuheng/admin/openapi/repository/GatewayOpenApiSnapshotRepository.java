package top.egon.cola.component.yuheng.admin.openapi.repository;


import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.yuheng.admin.openapi.domain.bo.GatewayOpenApiSnapshotBO;

import java.util.List;
import java.util.Optional;

/**
 * 中文说明：{@code GatewayOpenApiSnapshotRepository} 是 {@code gateway_openapi_snapshot} 的业务端口，只声明调用方
 * 实际使用的命名方法；它不继承 Spring Data/JPA 泛型 CRUD，也不外泄 MyBatis-Plus 行模型，方法名、返回形状与可选
 * 语义都按业务契约固定。
 * English summary: {@code GatewayOpenApiSnapshotRepository} is the business port for
 * {@code gateway_openapi_snapshot}; it declares only the named methods current callers actually invoke, inherits no
 * Spring Data/JPA generic CRUD, leaks no MyBatis-Plus row model, and keeps method names, return shapes and
 * optionality pinned to the business contract.
 *
 * 用法 / Usage: 由 {@code MpGatewayOpenApiSnapshotRepository} 在调用方 {@code gatewayTransactionManager} 事务内以受守卫的
 * MP 读写实现；不可变契约（同一 {@code application/build/group/canonical_sha256} 只能对应一份文档）由
 * {@code GatewayOpenApiSnapshotBO.validated(...)} 与实现的 {@code insertOrReuse} 冲突分支共同保证。/
 * Implement it through {@code MpGatewayOpenApiSnapshotRepository} inside the caller's
 * {@code gatewayTransactionManager} transaction; the immutable-contract rule (one document per
 * {@code application/build/group/canonical_sha256}) is held by {@code GatewayOpenApiSnapshotBO.validated(...)} plus the
 * conflict branch of {@code insertOrReuse}.
 */
@Validated
public interface GatewayOpenApiSnapshotRepository {

    /**
     * 中文说明：执行 find 操作；按不可变快照 id 读取单行，旧实现返回空 Optional 而不是抛异常，因此非十进制或不存在的
     * 不透明 id 同样按“无行”处理。
     * English summary: Executes the find operation; loads one row by immutable snapshot id and, like the legacy
     * implementation, yields an empty Optional instead of failing, so a non-decimal or absent opaque id is “no row”.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayOpenApiSnapshotRepository.findById(snapshotId)}。
     * @param snapshotId 参数 快照Id；parameter snapshot identifier。
     * @return 返回 find 的处理结果；returns the snapshot carrier when present.
     */
    Optional<GatewayOpenApiSnapshotBO> findById(@NotBlank String snapshotId);

    /**
     * 中文说明：执行 find 操作；按业务唯一键 {@code applicationId + buildId + openapiGroup + canonicalSha256} 定位快照，
     * 与旧 SQL 的四列谓词一致，并在多行历史脏数据下保持“取首行”的旧语义。
     * English summary: Executes the find operation; locates the snapshot by the business unique key
     * {@code applicationId + buildId + openapiGroup + canonicalSha256}, the four predicates the legacy SQL used, and keeps
     * the legacy first-row selection if dirty history holds more than one row.
     *
     * 用法 / Usage: 调用方式 / Usage:
     * {@code GatewayOpenApiSnapshotRepository.findByContract(applicationId, buildId, openapiGroup, canonicalSha256)}。
     * @param applicationId 参数 归属应用；parameter owning application identifier。
     * @param buildId 参数 不可变提供方构建；parameter immutable provider build。
     * @param openapiGroup 参数 来源Group；parameter source Group。
     * @param canonicalSha256 参数 规范文档哈希；parameter canonical document hash。
     * @return 返回 find 的处理结果；returns an existing snapshot carrier when present.
     */
    Optional<GatewayOpenApiSnapshotBO> findByContract(
            @NotBlank String applicationId,
            @NotBlank String buildId,
            @NotBlank String openapiGroup,
            @NotBlank String canonicalSha256);

    /**
     * 中文说明：执行 find 操作；按应用 Group 与规范哈希解析最新快照（旧 SQL 的
     * {@code ORDER BY fetched_at DESC, id}），用于把 Operation 溯源信息还原为不可变快照。
     * English summary: Executes the find operation; resolves the newest snapshot by application Group and canonical hash
     * (the legacy {@code ORDER BY fetched_at DESC, id}) so operation provenance maps back onto an immutable snapshot.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayOpenApiSnapshotRepository
     * .findByApplicationGroupAndCanonicalSha256(applicationId, openapiGroup, canonicalSha256)}。
     * @param applicationId 参数 归属应用；parameter owning application identifier。
     * @param openapiGroup 参数 来源Group；parameter source Group。
     * @param canonicalSha256 参数 规范文档哈希；parameter canonical document hash。
     * @return 返回 find 的处理结果；returns the newest matching snapshot carrier when present.
     */
    Optional<GatewayOpenApiSnapshotBO> findByApplicationGroupAndCanonicalSha256(
            @NotBlank String applicationId,
            @NotBlank String openapiGroup,
            @NotBlank String canonicalSha256);

    /**
     * 中文说明：执行 insertOrReuse 操作；插入不可变快照，业务唯一键已存在时返回既有的同一契约行，
     * 契约不一致时按旧实现的 {@code YUHENG_OPENAPI_SNAPSHOT_CONFLICT} 抛出；影响 0 行不得被报告为新建成功。
     * English summary: Executes the insert-or-reuse operation; inserts the immutable snapshot, returns the pre-existing
     * row when the business unique key already holds the same contract, and raises the legacy
     * {@code YUHENG_OPENAPI_SNAPSHOT_CONFLICT} when the contract differs; a zero-row effect is never reported as a fresh
     * insert.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayOpenApiSnapshotRepository.insertOrReuse(snapshot)}。
     * @param snapshot 参数 不可变快照载体；parameter the immutable snapshot carrier.
     * @return 返回 insertOrReuse 的处理结果；returns the inserted or reused carrier.
     */
    GatewayOpenApiSnapshotBO insertOrReuse(
            @Valid GatewayOpenApiSnapshotBO snapshot);

    /**
     * 中文说明：执行 find 操作；只列出请求 Group 集合内的快照，按旧 SQL 的 {@code openapi_group, id} 稳定次序返回，
     * 并在去空白/去重后的集合为空时不触碰任何语句。
     * English summary: Executes the find operation; lists snapshots for exactly the requested Group collection in the
     * legacy {@code openapi_group, id} order, and touches no statement once the trimmed and de-duplicated set is empty.
     *
     * 用法 / Usage: 调用方式 / Usage:
     * {@code GatewayOpenApiSnapshotRepository.findByBuildGroups(applicationId, buildId, openapiGroups)}。
     * @param applicationId 参数 归属应用；parameter owning application identifier。
     * @param buildId 参数 不可变提供方构建；parameter immutable provider build。
     * @param openapiGroups 参数 通告的Group编码；parameter advertised Group codes。
     * @return 返回 find 的处理结果；returns the carriers ordered by Group and id.
     */
    List<GatewayOpenApiSnapshotBO> findByBuildGroups(
            @NotBlank String applicationId,
            @NotBlank String buildId,
            List<String> openapiGroups);

    /**
     * 中文说明：执行 linkAll 操作；把每个快照链接到同一个聚合 Definition Set，已存在的相同链接按幂等计入，
     * 链接到其它 Definition Set 或未找到行都按旧实现的冲突/未找到异常抛出。
     * English summary: Executes the link-all operation; links every snapshot to one aggregate Definition Set, counts an
     * existing identical link as idempotent, and raises the legacy conflict or not-found exception for a foreign link or
     * a missing row.
     *
     * 用法 / Usage: 调用方式 / Usage:
     * {@code GatewayOpenApiSnapshotRepository.linkAllToDefinitionSet(snapshotIds, definitionSetId)}。
     * @param snapshotIds 参数 不可变快照标识；parameter immutable snapshot identifiers。
     * @param definitionSetId 参数 聚合Definition Set标识；parameter aggregate Definition Set identifier。
     * @return 返回 linkAll 的处理结果；returns the number of snapshots linked or already linked.
     */
    int linkAllToDefinitionSet(
            List<String> snapshotIds,
            @NotBlank String definitionSetId);

    /**
     * 中文说明：执行 find 操作；列出链接到某聚合 Definition Set 的全部快照，沿用旧 SQL 的
     * {@code definition_set_id = ? ORDER BY openapi_group, id}。
     * English summary: Executes the find operation; lists every snapshot linked to an aggregate Definition Set with the
     * legacy {@code definition_set_id = ? ORDER BY openapi_group, id}.
     *
     * 用法 / Usage: 调用方式 / Usage:
     * {@code GatewayOpenApiSnapshotRepository.findByDefinitionSetId(definitionSetId)}。
     * @param definitionSetId 参数 聚合Definition Set标识；parameter aggregate Definition Set identifier。
     * @return 返回 find 的处理结果；returns the carriers ordered by Group and id.
     */
    List<GatewayOpenApiSnapshotBO> findByDefinitionSetId(@NotBlank String definitionSetId);
}
