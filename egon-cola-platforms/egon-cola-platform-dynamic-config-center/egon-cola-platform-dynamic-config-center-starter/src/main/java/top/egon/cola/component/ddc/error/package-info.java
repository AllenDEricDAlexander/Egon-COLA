/**
 * DDC 跨边界基础错误状态与异常，保存与具体传输或管理客户端无关的错误契约。
 * HTTP 和管理客户端专用错误位于子包；本包不记录日志、不重试，也不映射远程响应。
 *
 * <p>Transport-neutral DDC error status and exception contracts. HTTP- and management-specific
 * errors live in subpackages; logging, retries, and remote response mapping are excluded.</p>
 */
@NonNullApi
package top.egon.cola.component.ddc.error;

import org.springframework.lang.NonNullApi;
