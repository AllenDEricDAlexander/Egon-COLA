/**
 * DDC 管理客户端端口的 HTTP 适配器，负责管理查询、写入与发布 API 调用及错误映射。
 * 本包不保存管理 DTO 或 Admin 持久化逻辑；它依赖 {@code api.client}、{@code model.management} 和 {@code client.http}。
 *
 * <p>HTTP adapter for DDC management queries, writes, publication APIs, and error mapping.
 * Management DTOs and Admin persistence are excluded; this package depends on {@code api.client},
 * {@code model.management}, and {@code client.http}.</p>
 */
@NonNullApi
package top.egon.cola.component.ddc.client.management;

import org.springframework.lang.NonNullApi;
