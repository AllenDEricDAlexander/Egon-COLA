package top.egon.cola.component.tianshu.admin.service.ai.validation;

import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * 后台作业线程的系统主体；作业已绑定租户上下文时，作用域校验以系统身份放行。
 * / System actor of background job threads; scope validation admits it once the job has bound a tenant context.
 */
public final class DdcAiSystemActor {

    public static final String JUDGE = "system:ai-judge";

    public static final String OPTIMIZER = "system:ai-optimizer";

    public static final String RETENTION = "system:ai-retention";

    private static final Pattern ACTOR = Pattern.compile("^system:[a-z-]{2,32}$");

    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    private DdcAiSystemActor() {
    }

    /** 在系统主体下执行并恢复原值。 / Runs the body as a system actor and restores the previous value. */
    public static <T> T bind(String actorId, Supplier<T> body) {
        if (actorId == null || !ACTOR.matcher(actorId).matches()) {
            throw new IllegalArgumentException("Invalid AI system actor");
        }
        String previous = CURRENT.get();
        CURRENT.set(actorId);
        try {
            return body.get();
        } finally {
            if (previous == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(previous);
            }
        }
    }

    /** 当前线程绑定的系统主体。 / The system actor bound to the current thread. */
    public static Optional<String> current() {
        return Optional.ofNullable(CURRENT.get());
    }
}
