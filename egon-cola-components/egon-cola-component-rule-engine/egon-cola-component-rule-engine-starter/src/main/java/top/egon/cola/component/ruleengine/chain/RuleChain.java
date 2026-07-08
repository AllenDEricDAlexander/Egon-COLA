package top.egon.cola.component.ruleengine.chain;

import java.util.ArrayList;
import java.util.List;

public record RuleChain<T, R>(
        String code,
        String name,
        List<ChainHandler<T, R>> handlers,
        int maxSteps,
        long timeoutMillis
) {

    public RuleChain {
        handlers = List.copyOf(handlers);
    }

    public static <T, R> Builder<T, R> builder(String code) {
        return new Builder<>(code);
    }

    public static final class Builder<T, R> {

        private final String code;

        private final List<ChainHandler<T, R>> handlers = new ArrayList<>();

        private String name;

        private int maxSteps = 100;

        private long timeoutMillis = 3000L;

        private Builder(String code) {
            this.code = code;
            this.name = code;
        }

        public Builder<T, R> name(String name) {
            this.name = name;
            return this;
        }

        public Builder<T, R> handler(ChainHandler<T, R> handler) {
            this.handlers.add(handler);
            return this;
        }

        public Builder<T, R> maxSteps(int maxSteps) {
            this.maxSteps = maxSteps;
            return this;
        }

        public Builder<T, R> timeoutMillis(long timeoutMillis) {
            this.timeoutMillis = timeoutMillis;
            return this;
        }

        public RuleChain<T, R> build() {
            return new RuleChain<>(code, name, handlers, maxSteps, timeoutMillis);
        }
    }
}
