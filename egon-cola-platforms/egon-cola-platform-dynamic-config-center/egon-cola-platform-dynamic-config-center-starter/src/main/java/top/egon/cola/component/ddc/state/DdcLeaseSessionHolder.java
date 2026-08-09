package top.egon.cola.component.ddc.state;

import top.egon.cola.component.ddc.model.lease.DdcLeaseSession;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 线程安全地保存当前 DDC 租约会话。
 * Thread-safe holder for the current DDC lease session.
 */
public class DdcLeaseSessionHolder {

    /**
     * 当前租约会话的原子引用。 Atomic reference to the current lease session.
     */
    private final AtomicReference<DdcLeaseSession> current = new AtomicReference<>();

    /**
     * 获取当前租约会话。
     * Returns the current lease session.
     *
     * @return 包含当前会话的可选值; optional containing the current session
     */
    public Optional<DdcLeaseSession> current() {
        return Optional.ofNullable(current.get());
    }

    /**
     * 使用新会话替换当前会话。
     * Replaces the current session with a new session.
     *
     * @param session 新租约会话; new lease session
     */
    public void replace(DdcLeaseSession session) {
        current.set(Objects.requireNonNull(session, "session must not be null"));
    }

    /**
     * 仅在当前会话与预期会话相同时将其清除。
     * Clears the current session only when it equals the expected session.
     *
     * @param expected 预期的当前会话; expected current session
     * @return 成功清除时为 {@code true}; {@code true} when the session was cleared
     */
    public boolean compareAndClear(DdcLeaseSession expected) {
        return current.compareAndSet(expected, null);
    }
}
