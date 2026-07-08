package top.egon.cola.component.ruleengine.tree;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record RuleTree<T, R>(
        String code,
        String name,
        RuleNode<T, R> root,
        Map<String, RuleNode<T, R>> nodes,
        String defaultEndNodeCode,
        int maxSteps,
        long timeoutMillis
) {

    public RuleTree {
        nodes = Collections.unmodifiableMap(new LinkedHashMap<>(nodes));
    }

    public static <T, R> Builder<T, R> builder(String code, RuleNode<T, R> root) {
        return new Builder<>(code, root);
    }

    public static final class Builder<T, R> {

        private final String code;

        private final RuleNode<T, R> root;

        private final Map<String, RuleNode<T, R>> nodes = new LinkedHashMap<>();

        private String name;

        private String defaultEndNodeCode;

        private int maxSteps = 100;

        private long timeoutMillis = 3000L;

        private Builder(String code, RuleNode<T, R> root) {
            this.code = code;
            this.root = root;
            this.name = code;
            if (root != null) {
                this.nodes.put(root.code(), root);
            }
        }

        public Builder<T, R> name(String name) {
            this.name = name;
            return this;
        }

        public Builder<T, R> node(RuleNode<T, R> node) {
            this.nodes.put(node.code(), node);
            return this;
        }

        public Builder<T, R> defaultEndNodeCode(String defaultEndNodeCode) {
            this.defaultEndNodeCode = defaultEndNodeCode;
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

        public RuleTree<T, R> build() {
            return new RuleTree<>(code, name, root, nodes, defaultEndNodeCode, maxSteps, timeoutMillis);
        }
    }
}
