package top.egon.cola.component.gateway.admin.openapi.repository;

import top.egon.cola.component.gateway.admin.openapi.domain.po.GatewayOpenApiSnapshotPO;

import java.util.List;
import java.util.Optional;

/**
 * Persistence boundary for immutable OpenAPI Group snapshots.
 *
 * <p>中文：OpenAPI Group 不可变快照的持久化边界；Definition Set 链接由 ingestion
 * 事务调用。
 */
public interface GatewayOpenApiSnapshotRepository {

    /**
     * Finds a snapshot by its immutable identifier.
     *
     * @param snapshotId snapshot identifier
     * @return the snapshot when present
     */
    Optional<GatewayOpenApiSnapshotPO> findById(String snapshotId);

    /**
     * Finds a snapshot by its application/build/group/canonical identity.
     *
     * @param applicationId owning application
     * @param buildId immutable provider build
     * @param openapiGroup source Group
     * @param canonicalSha256 canonical document hash
     * @return an existing snapshot when present
     */
    Optional<GatewayOpenApiSnapshotPO> findByContract(
            String applicationId,
            String buildId,
            String openapiGroup,
            String canonicalSha256);

    /**
     * Inserts a snapshot or returns the identical existing row.
     *
     * @param snapshot immutable snapshot row
     * @return inserted or reused snapshot
     */
    GatewayOpenApiSnapshotPO insertOrReuse(
            GatewayOpenApiSnapshotPO snapshot);

    /**
     * Lists snapshots for exactly the requested build Groups in stable order.
     *
     * @param applicationId owning application
     * @param buildId immutable provider build
     * @param openapiGroups advertised Group codes
     * @return snapshots ordered by Group and id
     */
    List<GatewayOpenApiSnapshotPO> findByBuildGroups(
            String applicationId,
            String buildId,
            List<String> openapiGroups);

    /**
     * Links each snapshot to one aggregate Definition Set. Existing identical
     * links are treated as idempotent; a different link is a conflict.
     *
     * @param snapshotIds immutable snapshot identifiers
     * @param definitionSetId aggregate Definition Set identifier
     * @return number of snapshots linked or already linked
     */
    int linkAllToDefinitionSet(
            List<String> snapshotIds,
            String definitionSetId);

    /**
     * Finds all snapshots linked to an aggregate Definition Set.
     *
     * @param definitionSetId aggregate Definition Set identifier
     * @return snapshots ordered by Group and id
     */
    List<GatewayOpenApiSnapshotPO> findByDefinitionSetId(
            String definitionSetId);
}
