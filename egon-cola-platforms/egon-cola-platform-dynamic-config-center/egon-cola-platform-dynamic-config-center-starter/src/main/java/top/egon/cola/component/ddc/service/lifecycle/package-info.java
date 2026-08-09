/**
 * DDC 实例身份创建、注册、默认值上报、ACK 投递、心跳、就绪和下线编排。
 * 本包协调 {@code api.client} 与 {@code state}，但不保存协议 DTO、Redis 连接或 Spring 自动装配定义。
 *
 * <p>Orchestration for DDC identity creation, registration, default reporting, acknowledgement
 * delivery, heartbeat, readiness, and shutdown. It coordinates {@code api.client} and {@code state}
 * without owning protocol DTOs, Redis connections, or Spring automatic configuration.</p>
 */
@NonNullApi
package top.egon.cola.component.ddc.service.lifecycle;

import org.springframework.lang.NonNullApi;
