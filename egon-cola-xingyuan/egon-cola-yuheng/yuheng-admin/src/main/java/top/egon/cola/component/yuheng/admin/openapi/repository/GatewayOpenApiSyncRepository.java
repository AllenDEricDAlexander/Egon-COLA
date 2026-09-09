package top.egon.cola.component.yuheng.admin.openapi.repository;

import top.egon.cola.component.yuheng.admin.openapi.domain.dto.GatewayOpenApiSyncKeyDTO;
import top.egon.cola.component.yuheng.admin.openapi.domain.enums.GatewayOpenApiSyncStateEnum;
import top.egon.cola.component.yuheng.admin.openapi.domain.po.GatewayOpenApiSyncPO;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Persistence boundary for per-Group OpenAPI synchronization state.
 *
 * <p>中文：每个 application/build/group 一个同步状态行，所有工作者更新都经过
 * revision CAS。
 */
public interface GatewayOpenApiSyncRepository {

    /**
     * Finds a synchronization row by its stable key.
     *
     * @param key application/build/Group key
     * @return an existing row when present
     */
    Optional<GatewayOpenApiSyncPO> findByKey(GatewayOpenApiSyncKeyDTO key);

    /**
     * Finds a synchronization row by identifier.
     *
     * @param syncId state row identifier
     * @return an existing row when present
     */
    Optional<GatewayOpenApiSyncPO> findById(String syncId);

    /**
     * Lists synchronization rows owned by one Gateway application.
     *
     * @param applicationId physical Gateway application identifier
     * @return rows in stable build/group order
     */
    default List<GatewayOpenApiSyncPO> findByApplicationId(
            String applicationId) {
        return List.of();
    }

    /**
     * Lists rows in a lifecycle state for restart repair and diagnostics.
     *
     * @param state state to query
     * @return rows in stable update order
     */
    default List<GatewayOpenApiSyncPO> findByStatus(
            GatewayOpenApiSyncStateEnum state) {
        return List.of();
    }

    /**
     * Inserts a discovered row or updates only its provider observation fields.
     * Existing lifecycle state and immutable links are preserved.
     *
     * @param state discovered observation
     * @return current persisted row
     */
    GatewayOpenApiSyncPO upsertDiscovered(GatewayOpenApiSyncPO state);

    /**
     * Loads due rows in a bounded stable order.
     *
     * @param now current UTC instant
     * @param limit maximum number of rows
     * @return due rows
     */
    List<GatewayOpenApiSyncPO> findDue(Instant now, int limit);

    /**
     * Claims a due row with an optimistic revision check.
     *
     * @param syncId row identifier
     * @param expectedRevision revision observed by the caller
     * @param now claim timestamp
     * @return {@code true} when ownership was acquired
     */
    boolean claim(String syncId, long expectedRevision, Instant now);

    /**
     * Moves a row between legal state-machine states using revision CAS.
     *
     * @param syncId row identifier
     * @param expectedRevision revision observed by the caller
     * @param expectedState current state expected by the caller
     * @param nextState requested next state
     * @param now transition timestamp
     * @return {@code true} when one row was updated
     */
    boolean transition(
            String syncId,
            long expectedRevision,
            GatewayOpenApiSyncStateEnum expectedState,
            GatewayOpenApiSyncStateEnum nextState,
            Instant now);

    /**
     * Completes ingestion and records the shared Definition Set link.
     *
     * @param syncId row identifier
     * @param expectedRevision revision observed by the caller
     * @param snapshotId latest Group snapshot
     * @param definitionSetId aggregate Definition Set
     * @param now success timestamp
     * @return {@code true} when one row was updated
     */
    boolean setValid(
            String syncId,
            long expectedRevision,
            String snapshotId,
            String definitionSetId,
            Instant now);

    /**
     * Stores a classified failure with an optional retry time.
     *
     * @param syncId row identifier
     * @param expectedRevision revision observed by the caller
     * @param nextState failure or terminal state
     * @param errorCode bounded stable error code
     * @param errorMessage bounded safe operator message
     * @param nextRetryAt retry timestamp, or {@code null}
     * @param now transition timestamp
     * @return {@code true} when one row was updated
     */
    boolean markFailure(
            String syncId,
            long expectedRevision,
            GatewayOpenApiSyncStateEnum nextState,
            String errorCode,
            String errorMessage,
            Instant nextRetryAt,
            Instant now);
}
