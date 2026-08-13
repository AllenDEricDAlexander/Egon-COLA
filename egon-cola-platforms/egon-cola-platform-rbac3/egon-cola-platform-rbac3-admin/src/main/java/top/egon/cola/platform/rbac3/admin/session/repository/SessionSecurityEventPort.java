package top.egon.cola.platform.rbac3.admin.session.repository;

import top.egon.cola.platform.rbac3.admin.session.domain.vo.TerminationVO;

/**
 * 记录会话终止安全事件的输出端口。
 * Output port for recording terminal-session security events.
 */
@FunctionalInterface
public interface SessionSecurityEventPort {

    /**
     * 记录会话终止事件。
     * Records one terminal-session event.
     *
     * @param termination 会话终止事实；session termination facts
     */
    void record(TerminationVO termination);
}
