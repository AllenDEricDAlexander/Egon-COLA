package top.egon.cola.component.bytecode.runtime.observation;

final class ObservationReentrancyGuard {

    private final ThreadLocal<Integer> depth = ThreadLocal.withInitial(() -> 0);

    boolean active() {
        return depth.get() > 0;
    }

    void run(Runnable action) {
        int current = depth.get();
        depth.set(current + 1);
        try {
            action.run();
        } finally {
            if (current == 0) {
                depth.remove();
            } else {
                depth.set(current);
            }
        }
    }
}
