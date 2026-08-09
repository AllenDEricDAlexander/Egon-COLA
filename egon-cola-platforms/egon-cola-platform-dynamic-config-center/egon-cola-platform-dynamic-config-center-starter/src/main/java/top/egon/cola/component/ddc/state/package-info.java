/**
 * DDC 进程内可变运行态，保存本地配置元数据、活动注册和当前租约会话。
 * 本包只封装线程安全状态与原子操作，不执行远程调用、事件监听或生命周期调度；服务和监听器依赖本包。
 *
 * <p>Mutable in-process DDC state for local configuration metadata, active registrations, and the
 * current lease session. It encapsulates thread-safe state and atomic operations only; remote calls,
 * event listening, and lifecycle scheduling are excluded.</p>
 */
@NonNullApi
package top.egon.cola.component.ddc.state;

import org.springframework.lang.NonNullApi;
