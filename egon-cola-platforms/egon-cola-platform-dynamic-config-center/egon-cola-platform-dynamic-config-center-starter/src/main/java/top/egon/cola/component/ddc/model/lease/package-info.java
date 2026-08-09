/**
 * DDC 租约角色、会话、操作状态和结果值对象，供配置与注册生命周期共享。
 * 本包不续租、不调度心跳、不保存当前会话；会话状态位于 {@code state}，编排位于 {@code service.lifecycle}。
 *
 * <p>Lease roles, sessions, operation statuses, and results shared by DDC lifecycles. Renewal,
 * heartbeat scheduling, and current-session storage are excluded; state belongs to {@code state}
 * and orchestration to {@code service.lifecycle}.</p>
 */
@NonNullApi
package top.egon.cola.component.ddc.model.lease;

import org.springframework.lang.NonNullApi;
