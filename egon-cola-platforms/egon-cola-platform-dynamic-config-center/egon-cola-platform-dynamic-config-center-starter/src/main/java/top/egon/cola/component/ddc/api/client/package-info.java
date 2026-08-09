/**
 * DDC 配置、管理和服务注册客户端的公共端口，只包含调用方可替换或直接调用的接口。
 * HTTP 适配器位于 {@code client}，Redis 与生命周期实现不得进入本包。
 *
 * <p>Public ports for DDC configuration, management, and service-registry clients. Only interfaces
 * that callers may replace or invoke belong here; HTTP adapters live in {@code client}, while Redis
 * and lifecycle implementations are excluded.</p>
 */
@NonNullApi
package top.egon.cola.component.ddc.api.client;

import org.springframework.lang.NonNullApi;
