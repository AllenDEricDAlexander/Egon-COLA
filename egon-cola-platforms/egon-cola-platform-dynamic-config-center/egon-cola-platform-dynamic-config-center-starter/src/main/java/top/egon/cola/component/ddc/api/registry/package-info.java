/**
 * DDC 服务注册订阅公共契约，定义订阅资源的生命周期边界。
 * 本包不处理 Redis Topic、快照读取或事件调度；监听实现依赖本接口并位于 {@code listener.registry}。
 *
 * <p>Public contract for DDC service-registry subscriptions and their lifecycle. Redis topics,
 * snapshot loading, and event scheduling are excluded; listener implementations depend on this
 * contract and live in {@code listener.registry}.</p>
 */
@NonNullApi
package top.egon.cola.component.ddc.api.registry;

import org.springframework.lang.NonNullApi;
