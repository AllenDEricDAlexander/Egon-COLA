/**
 * DDC 服务键、实例、注册、查询、快照、目录事件和健康元数据模型。
 * 本包只表达服务注册领域数据，不执行 HTTP 查询、Redis 监听或本地过期；相应实现位于 {@code client}、{@code listener} 和 {@code state}。
 *
 * <p>Models for DDC service keys, instances, registrations, queries, snapshots, catalog events,
 * and health metadata. HTTP querying, Redis listening, and local expiry are excluded and belong to
 * {@code client}, {@code listener}, and {@code state}.</p>
 */
@NonNullApi
package top.egon.cola.component.ddc.model.registry;

import org.springframework.lang.NonNullApi;
