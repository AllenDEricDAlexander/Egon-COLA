package top.egon.cola.component.accessguard.core.plan;

import java.util.Optional;
import java.util.function.Consumer;

public interface GuardPlanSource {

    String name();

    int priority();

    Optional<GuardPlanSnapshot> current(String ruleId);

    AutoCloseable subscribe(Consumer<GuardPlanSnapshot> listener);
}
