package top.egon.cola.component.gateway.contract.rule;

import java.time.Duration;
import java.util.Objects;

/**
 * 策略嵌套记录共用的 Duration 正数校验工具。
 *
 * <p>独立成包内类是为了避免初始化 {@link ServiceCallPolicy} 时触发嵌套 record 的静态初始化
 * 循环；它不代表一个可单独发布的治理策略。
 */
final class PolicyDurations {

    private PolicyDurations() {
    }

    static Duration requirePositive(Duration value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isNegative() || value.isZero()) {
            throw new IllegalArgumentException(field + " must be positive but was " + value);
        }
        return value;
    }
}
