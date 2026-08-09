/**
 * DDC 服务键构造与注册快照读取协作能力，为注册客户端和监听器提供同步服务。
 * 本包不调度订阅、不持有活动注册；监听与状态分别依赖本包并位于 {@code listener.registry} 和 {@code state}。
 *
 * <p>Synchronous services for DDC service-key construction and registry snapshot loading.
 * Subscription scheduling and active-registration state are excluded; {@code listener.registry}
 * and {@code state} depend on these services.</p>
 */
@NonNullApi
package top.egon.cola.component.ddc.service.registry;

import org.springframework.lang.NonNullApi;
