package top.egon.cola.component.yuheng.admin.reporting.repository;


import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.yuheng.admin.reporting.domain.vo.GatewayReconcileResultVO;

import java.time.Instant;
import java.util.Set;

/**
 * 中文说明：{@code GatewayDefinitionLifecycleRepository} 是网关定义生命周期的公开业务端口，签名与原
 * {@code JdbcGatewayDefinitionLifecycleRepository} 完全一致，只声明调用方（{@code GatewayDefinitionLifecycleReconciler}）实际使用的方法；
 * 它不继承任何 Spring Data/JPA 仓储，不暴露 {@code *RecordPO}、DAO 或 Wrapper，也不承载默认业务分支——事务组合与受守卫查询归实现类所有。
 * English summary: {@code GatewayDefinitionLifecycleRepository} is the public gateway definition-lifecycle port whose signatures mirror {@code JdbcGatewayDefinitionLifecycleRepository} exactly and declare only the methods its callers
 * ({@code GatewayDefinitionLifecycleReconciler}) actually use; it extends no Spring Data/JPA repository, never leaks a {@code *RecordPO}, DAO or wrapper type, and carries no default business branch — transaction composition and guarded queries belong to the implementation.
 *
 * 用法 / Usage: 由定时协调器经 Spring 注入该端口，在调用方事务内触发对账；返回值只是行数统计，0 行变更以 {@code changed() == false} 如实表达，绝不伪造成功。/ Inject this port into the scheduled reconciler and reconcile inside the caller's transaction; the result carries row counts only, and a zero-row change is reported truthfully as {@code changed() == false} instead of a fake success.
 */
@Validated
public interface GatewayDefinitionLifecycleRepository {

    /**
     * 中文说明：执行 reconcile 操作；以当前在线提供者上报的定义集合作为期望态，按应用逐个激活/退役定义集，并把该应用下网关操作的当前定义指向、方法身份、提供方服务身份、外部可达标记与生命周期状态对齐到最新一次成员关系，最后把不再被任何激活定义集引用的 RPC/OpenAPI 描述型操作下线；返回四类受影响行数。
     * {@code activeDefinitionSetIds} 为 {@code null} 时按空集处理，与原实现一致；因此本端口不对该参数施加约束校验，以免拒绝原本可接受的输入。
     * English summary: Executes the reconcile operation; treating the reported definition sets of the currently online providers as the desired state, it activates and retires definition sets per application, aligns each application's gateway operations (current-definition pointer, method identity, provider service identity, external-accessible flag and lifecycle status) with the newest membership row, and finally offlines RPC/OpenAPI descriptor-backed operations no longer referenced by any activated definition set; it returns the four affected-row counts.
     * A {@code null} {@code activeDefinitionSetIds} is handled as the empty set exactly as before, which is why the port adds no constraint annotation on it and would otherwise reject previously accepted input.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDefinitionLifecycleRepository.reconcile(activeDefinitionSetIds, now)}。调用方须在同一事务内执行并依据 {@code changed()} 决定是否写审计。/ Call it inside one transaction and decide on auditing from {@code changed()}.
     * @param activeDefinitionSetIds 参数 当前在线的定义集合 id；parameter ids of the currently active definition sets.
     * @param now 参数 本次对账时间戳；parameter reconciliation timestamp.
     * @return 返回激活/退役定义集与激活/下线操作的四类计数；returns the activated and retired definition-set counts plus the activated and offlined operation counts.
     */
    GatewayReconcileResultVO reconcile(
            Set<String> activeDefinitionSetIds,
            Instant now);

    /**
     * 中文说明：执行 activeOpenApiDefinitionSetIds 操作；返回聚合 OpenAPI 定义集合中整组记录当前处于 {@code VALID} 的那些定义集合 id。
     * RPC 元数据仍归 Tianshu 所有，由生命周期协调器在应用边界完成合并；实现无结果时返回空集而非 {@code null}。
     * English summary: Executes the active OpenAPI definition-set ids operation; returns the aggregate OpenAPI Definition Sets whose complete Group rows are currently {@code VALID}. RPC metadata remains owned by Tianshu and is merged by the lifecycle reconciler at the application boundary; an implementation returns an empty set, never {@code null}, when nothing qualifies.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDefinitionLifecycleRepository.activeOpenApiDefinitionSetIds()}。
     * @return 返回有效 OpenAPI 定义集合 id 集合；returns the valid OpenAPI definition set ids.
     */
    Set<String> activeOpenApiDefinitionSetIds();
}
