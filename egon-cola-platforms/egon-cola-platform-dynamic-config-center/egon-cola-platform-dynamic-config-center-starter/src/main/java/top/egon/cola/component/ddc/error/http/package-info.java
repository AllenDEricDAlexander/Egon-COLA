/**
 * DDC HTTP 请求构造、签名和序列化失败的专用异常契约。
 * 本包只表达客户端 HTTP 边界错误，不包含管理业务错误码；{@code client.http} 创建这些异常。
 *
 * <p>Specialized errors for DDC HTTP request construction, signing, and serialization. Management
 * business error codes are excluded; {@code client.http} creates these exceptions at its boundary.</p>
 */
@NonNullApi
package top.egon.cola.component.ddc.error.http;

import org.springframework.lang.NonNullApi;
