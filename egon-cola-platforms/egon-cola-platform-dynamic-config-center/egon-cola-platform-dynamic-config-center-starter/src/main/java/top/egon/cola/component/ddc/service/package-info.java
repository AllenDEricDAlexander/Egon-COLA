/**
 * DDC 同步编排与默认实现的组织包，按绑定、生命周期、刷新和注册职责分组。
 * 服务实现依赖公共端口和模型，不拥有 HTTP/Redis 连接，也不向 {@code api} 或 {@code model} 反向暴露实现。
 *
 * <p>Organizational package for synchronous DDC orchestration and default implementations grouped
 * by binding, lifecycle, refresh, and registry responsibilities. Services depend on public ports
 * and models, never owning HTTP or Redis connections or leaking implementations into public layers.</p>
 */
@NonNullApi
package top.egon.cola.component.ddc.service;

import org.springframework.lang.NonNullApi;
