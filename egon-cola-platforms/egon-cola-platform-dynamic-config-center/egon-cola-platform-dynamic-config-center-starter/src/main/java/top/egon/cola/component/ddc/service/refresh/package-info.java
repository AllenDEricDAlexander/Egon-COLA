/**
 * DDC 配置应用器注册、YAML 应用、字段刷新和配置属性重绑定编排。
 * 本包实现 {@code api.refresh} 端口并依赖 {@code format}，不监听 Redis Topic，也不拥有远程客户端。
 *
 * <p>Orchestration for applier registration, YAML application, field refresh, and configuration
 * property rebinding. It implements {@code api.refresh} and depends on {@code format}; Redis topic
 * listening and remote client ownership are excluded.</p>
 */
@NonNullApi
package top.egon.cola.component.ddc.service.refresh;

import org.springframework.lang.NonNullApi;
