/**
 * DDC 远程客户端适配器的组织包，按配置、管理、注册和共享 HTTP 能力分组。
 * 客户端实现依赖 {@code api} 与 {@code model}，但公共端口和领域模型不得反向依赖本包。
 *
 * <p>Organizational package for DDC remote client adapters grouped by configuration, management,
 * registry, and shared HTTP support. Implementations depend on {@code api} and {@code model}; those
 * public layers never depend back on this package.</p>
 */
@NonNullApi
package top.egon.cola.component.ddc.client;

import org.springframework.lang.NonNullApi;
