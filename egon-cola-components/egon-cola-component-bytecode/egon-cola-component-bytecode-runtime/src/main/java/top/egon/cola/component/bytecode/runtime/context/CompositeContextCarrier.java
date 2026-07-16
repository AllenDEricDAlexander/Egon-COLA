package top.egon.cola.component.bytecode.runtime.context;

import top.egon.cola.component.bytecode.api.executor.ContextCarrier;
import top.egon.cola.component.bytecode.api.executor.ContextScope;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class CompositeContextCarrier implements ContextCarrier {

    private final List<ContextCarrier> carriers;

    public CompositeContextCarrier(List<? extends ContextCarrier> carriers) {
        Objects.requireNonNull(carriers, "carriers");
        this.carriers = List.copyOf(carriers);
    }

    @Override
    public String name() {
        return "composite";
    }

    @Override
    public ContextSnapshot capture() {
        List<ContextSnapshot.CapturedContext> values = new ArrayList<>(carriers.size());
        for (ContextCarrier carrier : carriers) {
            values.add(new ContextSnapshot.CapturedContext(carrier, carrier.capture()));
        }
        return new ContextSnapshot(values);
    }

    @Override
    public ContextScope restore(Object snapshot) {
        if (!(snapshot instanceof ContextSnapshot contextSnapshot)) {
            throw new IllegalArgumentException("Composite carrier requires a ContextSnapshot");
        }
        return contextSnapshot.restore();
    }
}
