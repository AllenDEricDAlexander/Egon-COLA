/**
 * DDC Trace 上下文提取、传播和异步任务包装能力。
 * 本包不定义业务事件或请求模型；客户端、服务和监听器单向依赖这里的可观测性支持。
 *
 * <p>Trace-context extraction, propagation, and asynchronous task wrapping for DDC. Business
 * events and request models are excluded; clients, services, and listeners depend on this
 * observability support in one direction.</p>
 */
@NonNullApi
package top.egon.cola.component.ddc.observability;

import org.springframework.lang.NonNullApi;
