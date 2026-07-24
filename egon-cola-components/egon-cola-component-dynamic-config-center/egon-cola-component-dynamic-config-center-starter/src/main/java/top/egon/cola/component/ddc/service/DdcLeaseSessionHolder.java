package top.egon.cola.component.ddc.service;

import top.egon.cola.component.ddc.model.vo.DdcLeaseSession;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class DdcLeaseSessionHolder {

    private final AtomicReference<DdcLeaseSession> current = new AtomicReference<>();

    public Optional<DdcLeaseSession> current() {
        return Optional.ofNullable(current.get());
    }

    public void replace(DdcLeaseSession session) {
        current.set(Objects.requireNonNull(session, "session must not be null"));
    }

    public boolean compareAndClear(DdcLeaseSession expected) {
        return current.compareAndSet(expected, null);
    }
}
