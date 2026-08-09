/**
 * DDC 异步配置与注册事件入口的组织包，按配置变更和服务注册事件分组。
 * 监听器依赖 {@code redis}、{@code service} 和 {@code state}，但不定义公共端口或领域模型。
 *
 * <p>Organizational package for asynchronous DDC configuration and registry event entry points.
 * Listeners depend on {@code redis}, {@code service}, and {@code state}, but define neither public
 * ports nor domain models.</p>
 */
@NonNullApi
package top.egon.cola.component.ddc.listener;

import org.springframework.lang.NonNullApi;
