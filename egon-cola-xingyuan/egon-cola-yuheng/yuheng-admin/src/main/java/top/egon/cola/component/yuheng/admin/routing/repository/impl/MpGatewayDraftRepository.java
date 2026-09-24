package top.egon.cola.component.yuheng.admin.routing.repository.impl;


import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.yuheng.admin.routing.converter.GatewayPolicyDraftPersistenceConverter;
import top.egon.cola.component.yuheng.admin.routing.converter.GatewayRouteDraftPersistenceConverter;
import top.egon.cola.component.yuheng.admin.routing.domain.bo.GatewayPolicyDraftBO;
import top.egon.cola.component.yuheng.admin.routing.domain.bo.GatewayRouteDraftBO;
import top.egon.cola.component.yuheng.admin.routing.domain.po.GatewayPolicyDraftRecordPO;
import top.egon.cola.component.yuheng.admin.routing.domain.po.GatewayRouteDraftRecordPO;
import top.egon.cola.component.yuheng.admin.routing.repository.GatewayDraftRepository;
import top.egon.cola.component.yuheng.admin.routing.repository.mp.GatewayPolicyDraftPersistenceRepository;
import top.egon.cola.component.yuheng.admin.routing.repository.mp.GatewayRouteDraftPersistenceRepository;

import java.util.List;
import java.util.Optional;

/**
 * 中文说明：{@code MpGatewayDraftRepository} 是存储组件，位于当前 Gateway 模块的相关包中，负责网关草稿路由与策略存储（MyBatis-Plus 门面）的职责与边界：逐方法替换旧手写 JDBC 草稿实现，读写一律经各表的受守卫 {@code EgonColaRepository} 持久化边界（租户过滤、活跃读取、乐观锁 CAS、软删），列与 JSON 映射只经 MapStruct 转换器完成，公开端口不泄漏 RecordPO。
 * English summary: {@code MpGatewayDraftRepository} is a MyBatis-Plus facade store in the current Gateway module replacing the legacy hand-written JDBC draft store method by method; every read and write goes through the table-specific guarded {@code EgonColaRepository} persistence boundary (tenant filtering, active-only reads, optimistic-lock CAS, versioned soft delete), column and JSON mapping happens only via the MapStruct converters, and the public port never leaks a RecordPO.
 *
 * 用法 / Usage: 通过业务端口 {@code GatewayDraftRepository} 由 Spring 容器注入；写入组合在调用方（{@code gatewayTransactionManager}）的同一事务内执行，影响 0 行的写按冲突如实抛出，绝不伪造成功。/ Use it through the business port {@code GatewayDraftRepository}; writes compose inside the caller's transaction managed by {@code gatewayTransactionManager}, and a zero-row write surfaces as a conflict instead of a fake success.
 */
@Slf4j
@Repository("mpGatewayDraftRepository")
@RequiredArgsConstructor
@Validated
public class MpGatewayDraftRepository implements GatewayDraftRepository {

    @Qualifier("gatewayRouteDraftPersistenceRepository")
    private final GatewayRouteDraftPersistenceRepository routeDraftPersistenceRepository;

    @Qualifier("gatewayPolicyDraftPersistenceRepository")
    private final GatewayPolicyDraftPersistenceRepository policyDraftPersistenceRepository;

    @Qualifier("gatewayRouteDraftPersistenceConverter")
    private final GatewayRouteDraftPersistenceConverter routeDraftPersistenceConverter;

    @Qualifier("gatewayPolicyDraftPersistenceConverter")
    private final GatewayPolicyDraftPersistenceConverter policyDraftPersistenceConverter;

    /**
     * 中文说明：执行 routes 操作；按网关组读取全部活跃草稿路由，保留原 SQL 的组内过滤与 {@code route_id} 升序（追加 {@code id} 稳定次序），再经转换器投影为业务载体列表。
     * English summary: Executes the routes operation; lists every active draft route scoped to the gateway group, preserving the original group filter and {@code route_id} ascending order (with a stable {@code id} tie-break) and projecting through the converter into business carriers.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code MpGatewayDraftRepository.routes(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @return 返回 routes 的处理结果；returns the result of the operation.
     */
    @Override
    public List<GatewayRouteDraftBO> routes(String gatewayGroupId) {
        Long gatewayGroupColumn = gatewayGroupColumn(gatewayGroupId);
        if (gatewayGroupColumn == null) {
            return List.of();
        }
        List<GatewayRouteDraftRecordPO> rows = routeDraftPersistenceRepository.list(
                Wrappers.<GatewayRouteDraftRecordPO>lambdaQuery()
                        .eq(GatewayRouteDraftRecordPO::getGatewayGroupId, gatewayGroupColumn)
                        .orderByAsc(GatewayRouteDraftRecordPO::getRouteId)
                        .orderByAsc(GatewayRouteDraftRecordPO::getId)
        );
        return routeDraftPersistenceConverter.toTargetList(rows);
    }

    /**
     * 中文说明：执行 policies 操作；按网关组读取全部活跃草稿策略，保留原 SQL 的组内过滤与 {@code policy_id} 升序（追加 {@code id} 稳定次序），再经转换器投影为业务载体列表。
     * English summary: Executes the policies operation; lists every active draft policy scoped to the gateway group, preserving the original group filter and {@code policy_id} ascending order (with a stable {@code id} tie-break) and projecting through the converter into business carriers.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code MpGatewayDraftRepository.policies(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @return 返回 policies 的处理结果；returns the result of the operation.
     */
    @Override
    public List<GatewayPolicyDraftBO> policies(String gatewayGroupId) {
        Long gatewayGroupColumn = gatewayGroupColumn(gatewayGroupId);
        if (gatewayGroupColumn == null) {
            return List.of();
        }
        List<GatewayPolicyDraftRecordPO> rows = policyDraftPersistenceRepository.list(
                Wrappers.<GatewayPolicyDraftRecordPO>lambdaQuery()
                        .eq(GatewayPolicyDraftRecordPO::getGatewayGroupId, gatewayGroupColumn)
                        .orderByAsc(GatewayPolicyDraftRecordPO::getPolicyId)
                        .orderByAsc(GatewayPolicyDraftRecordPO::getId)
        );
        return policyDraftPersistenceConverter.toTargetList(rows);
    }

    /**
     * 中文说明：执行 upsert路由 操作；等价于原 INSERT ... ON CONFLICT (gateway_group_id, route_id) DO UPDATE：先按组与路由键读取活跃行，缺失则受守卫插入（技术元数据由边界补齐），存在则沿用原行的技术 id/version 后以转换器生成的业务列做乐观锁 CAS 替换；任一 0 行写入按冲突如实抛出。
     * English summary: Executes the upsert route operation; equivalent to the legacy INSERT ... ON CONFLICT (gateway_group_id, route_id) DO UPDATE: it loads the active row by group and route key, performs a guarded insert (the boundary fills the technical metadata) when absent, and otherwise replaces the business columns produced by the converter under optimistic-lock CAS reusing the original row's technical id/version; any zero-row write surfaces as a conflict.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code MpGatewayDraftRepository.upsertRoute(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param route 参数 路由；parameter route。
     */
    @Override
    public void upsertRoute(GatewayRouteDraftBO route) {
        GatewayRouteDraftRecordPO candidate = routeDraftPersistenceConverter.newRow(route);
        Optional<GatewayRouteDraftRecordPO> current = routeDraftPersistenceRepository.getOneOpt(
                Wrappers.<GatewayRouteDraftRecordPO>lambdaQuery()
                        .eq(GatewayRouteDraftRecordPO::getGatewayGroupId, candidate.getGatewayGroupId())
                        .eq(GatewayRouteDraftRecordPO::getRouteId, candidate.getRouteId())
        );
        if (current.isEmpty()) {
            if (!routeDraftPersistenceRepository.save(candidate)) {
                throw new IllegalStateException("YUHENG_ADMIN_DRAFT_ROUTE_WRITE_CONFLICT");
            }
            return;
        }
        candidate.setId(current.get().getId());
        candidate.setVersion(current.get().getVersion());
        if (!routeDraftPersistenceRepository.updateById(candidate)) {
            throw new IllegalStateException("YUHENG_ADMIN_DRAFT_ROUTE_WRITE_CONFLICT");
        }
    }

    /**
     * 中文说明：执行 delete路由 操作；等价于原按 (gateway_group_id, route_id) 的删除：键无法命中或行已不存在时如实保持 0 行无害语义（与原 DELETE 未命中一致），命中则以受守卫的版本化软删除落库，软删 0 行（并发改写）按冲突如实抛出；同微秒软删唯一键冲突由数据库唯一约束如实上抛。
     * English summary: Executes the delete route operation; equivalent to the legacy delete by (gateway_group_id, route_id): an unmatchable key or an already-absent row keeps the original harmless zero-row semantics, a matched row is removed through the guarded versioned soft delete, a zero-row soft delete (concurrent change) surfaces as a conflict, and a same-microsecond soft-delete unique-key collision propagates truthfully from the database constraint.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code MpGatewayDraftRepository.deleteRoute(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @param routeId 参数 路由Id；parameter route id。
     */
    @Override
    public void deleteRoute(String gatewayGroupId, String routeId) {
        Long gatewayGroupColumn = gatewayGroupColumn(gatewayGroupId);
        if (gatewayGroupColumn == null) {
            return;
        }
        routeDraftPersistenceRepository.getOneOpt(
                Wrappers.<GatewayRouteDraftRecordPO>lambdaQuery()
                        .eq(GatewayRouteDraftRecordPO::getGatewayGroupId, gatewayGroupColumn)
                        .eq(GatewayRouteDraftRecordPO::getRouteId, routeId)
        ).ifPresent(current -> {
            if (!routeDraftPersistenceRepository.removeById(current)) {
                throw new IllegalStateException("YUHENG_ADMIN_DRAFT_ROUTE_DELETE_CONFLICT");
            }
        });
    }

    /**
     * 中文说明：执行 upsert策略 操作；等价于原 INSERT ... ON CONFLICT (gateway_group_id, policy_id) DO UPDATE：先按组与策略键读取活跃行，缺失则受守卫插入（技术元数据由边界补齐），存在则沿用原行的技术 id/version 后以转换器生成的业务列做乐观锁 CAS 替换；任一 0 行写入按冲突如实抛出。
     * English summary: Executes the upsert policy operation; equivalent to the legacy INSERT ... ON CONFLICT (gateway_group_id, policy_id) DO UPDATE: it loads the active row by group and policy key, performs a guarded insert (the boundary fills the technical metadata) when absent, and otherwise replaces the business columns produced by the converter under optimistic-lock CAS reusing the original row's technical id/version; any zero-row write surfaces as a conflict.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code MpGatewayDraftRepository.upsertPolicy(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param policy 参数 策略；parameter policy。
     */
    @Override
    public void upsertPolicy(GatewayPolicyDraftBO policy) {
        GatewayPolicyDraftRecordPO candidate = policyDraftPersistenceConverter.newRow(policy);
        Optional<GatewayPolicyDraftRecordPO> current = policyDraftPersistenceRepository.getOneOpt(
                Wrappers.<GatewayPolicyDraftRecordPO>lambdaQuery()
                        .eq(GatewayPolicyDraftRecordPO::getGatewayGroupId, candidate.getGatewayGroupId())
                        .eq(GatewayPolicyDraftRecordPO::getPolicyId, candidate.getPolicyId())
        );
        if (current.isEmpty()) {
            if (!policyDraftPersistenceRepository.save(candidate)) {
                throw new IllegalStateException("YUHENG_ADMIN_DRAFT_POLICY_WRITE_CONFLICT");
            }
            return;
        }
        candidate.setId(current.get().getId());
        candidate.setVersion(current.get().getVersion());
        if (!policyDraftPersistenceRepository.updateById(candidate)) {
            throw new IllegalStateException("YUHENG_ADMIN_DRAFT_POLICY_WRITE_CONFLICT");
        }
    }

    /**
     * 中文说明：执行 delete策略 操作；等价于原按 (gateway_group_id, policy_id) 的删除：键无法命中或行已不存在时如实保持 0 行无害语义（与原 DELETE 未命中一致），命中则以受守卫的版本化软删除落库，软删 0 行（并发改写）按冲突如实抛出；同微秒软删唯一键冲突由数据库唯一约束如实上抛。
     * English summary: Executes the delete policy operation; equivalent to the legacy delete by (gateway_group_id, policy_id): an unmatchable key or an already-absent row keeps the original harmless zero-row semantics, a matched row is removed through the guarded versioned soft delete, a zero-row soft delete (concurrent change) surfaces as a conflict, and a same-microsecond soft-delete unique-key collision propagates truthfully from the database constraint.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code MpGatewayDraftRepository.deletePolicy(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @param policyId 参数 策略Id；parameter policy id。
     */
    @Override
    public void deletePolicy(String gatewayGroupId, String policyId) {
        Long gatewayGroupColumn = gatewayGroupColumn(gatewayGroupId);
        if (gatewayGroupColumn == null) {
            return;
        }
        policyDraftPersistenceRepository.getOneOpt(
                Wrappers.<GatewayPolicyDraftRecordPO>lambdaQuery()
                        .eq(GatewayPolicyDraftRecordPO::getGatewayGroupId, gatewayGroupColumn)
                        .eq(GatewayPolicyDraftRecordPO::getPolicyId, policyId)
        ).ifPresent(current -> {
            if (!policyDraftPersistenceRepository.removeById(current)) {
                throw new IllegalStateException("YUHENG_ADMIN_DRAFT_POLICY_DELETE_CONFLICT");
            }
        });
    }

    /**
     * 中文说明：执行 组键投影 操作；该私有辅助方法把端口携带的十进制不透明组标识字符串转换为新 {@code bigint} 列的查询谓词值；null、空白或非十进制输入在旧 VARCHAR 外键模型下永远不可能命中任何行，因此如实返回 null 由调用方映射为“无匹配行”的空读取或 0 行删除。
     * English summary: Executes the group-key projection; this private helper converts the decimal opaque group identifier carried by the port into the predicate value of the new {@code bigint} column. A null, blank or non-decimal input could never match any row under the legacy VARCHAR foreign-key model, so it truthfully yields null and the caller maps that to an empty read or a zero-row delete.
     *
     * 用法 / Usage: 仅在本类内部为守卫查询与删除准备谓词值时调用；/ Used only inside this class to prepare predicate values for guarded queries and deletes; upsert paths take the group key already converted by the MapStruct converter.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @return 返回 组键投影 的处理结果；returns the result of the operation.
     */
    private static Long gatewayGroupColumn(String gatewayGroupId) {
        if (gatewayGroupId == null || gatewayGroupId.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(gatewayGroupId.trim());
        } catch (NumberFormatException invalidOpaqueId) {
            return null;
        }
    }
}
