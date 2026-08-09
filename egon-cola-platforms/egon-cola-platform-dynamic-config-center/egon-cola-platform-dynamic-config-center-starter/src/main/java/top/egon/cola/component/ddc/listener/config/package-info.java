/**
 * DDC 配置发布事件监听器，负责过滤目标、串联拉取与刷新并触发 ACK。
 * 本包不解析 YAML、不持有本地元数据，也不创建 Redis 客户端；它依赖对应的服务、状态和 Redis 资源句柄。
 *
 * <p>Listener for DDC configuration publication events, responsible for target filtering and
 * coordinating retrieval, refresh, and acknowledgement. YAML parsing, local state, and Redis
 * client creation are excluded and delegated to focused packages.</p>
 */
@NonNullApi
package top.egon.cola.component.ddc.listener.config;

import org.springframework.lang.NonNullApi;
