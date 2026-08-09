/**
 * DDC 客户端连接与传输配置模型，保存调用方需要显式构造的安全和管理客户端参数。
 * 本包不创建 HTTP 客户端或读取 Spring 属性；{@code client.http} 和 {@code autoconfigure} 消费这些模型。
 *
 * <p>Client connection and transport configuration models explicitly constructed by DDC callers.
 * HTTP client creation and Spring property binding are excluded; {@code client.http} and
 * {@code autoconfigure} consume these models.</p>
 */
@NonNullApi
package top.egon.cola.component.ddc.model.client;

import org.springframework.lang.NonNullApi;
