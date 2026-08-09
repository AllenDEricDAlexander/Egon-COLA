/**
 * DDC 服务注册客户端端口的 HTTP 适配器，负责注册、续租、注销、查询和快照加载请求。
 * 本包不持有活动注册或订阅线程；状态与监听分别位于 {@code state} 和 {@code listener.registry}。
 *
 * <p>HTTP adapter for DDC service registration, renewal, deregistration, queries, and snapshot
 * loading. Active registrations and subscription threads are excluded and belong to {@code state}
 * and {@code listener.registry}.</p>
 */
@NonNullApi
package top.egon.cola.component.ddc.client.registry;

import org.springframework.lang.NonNullApi;
