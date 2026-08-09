/**
 * DDC 配置客户端端口的 HTTP 适配器，负责配置拉取、注册、默认值上报、确认与心跳请求。
 * 本包不处理刷新或 Redis 事件；它依赖 {@code api.client}、{@code model.config} 和 {@code client.http}。
 *
 * <p>HTTP adapter for the DDC configuration-client port, covering retrieval, registration,
 * default reporting, acknowledgements, and heartbeat requests. Refresh and Redis events are
 * excluded; this package depends on {@code api.client}, {@code model.config}, and {@code client.http}.</p>
 */
@NonNullApi
package top.egon.cola.component.ddc.client.config;

import org.springframework.lang.NonNullApi;
