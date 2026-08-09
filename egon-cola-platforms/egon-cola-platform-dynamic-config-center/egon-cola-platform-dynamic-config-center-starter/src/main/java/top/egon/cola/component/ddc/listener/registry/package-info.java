/**
 * DDC 服务目录与实例事件监听、刷新合并和周期对账实现。
 * 包内生命周期协作者只服务订阅编排；本包依赖 {@code service.registry}、{@code state} 和 {@code redis}，不实现 HTTP 查询。
 *
 * <p>Listeners for DDC service-catalog and instance events, refresh coalescing, and periodic
 * reconciliation. Package-private lifecycle collaborators serve subscription orchestration only;
 * this package depends on registry services, state, and Redis without implementing HTTP queries.</p>
 */
@NonNullApi
package top.egon.cola.component.ddc.listener.registry;

import org.springframework.lang.NonNullApi;
