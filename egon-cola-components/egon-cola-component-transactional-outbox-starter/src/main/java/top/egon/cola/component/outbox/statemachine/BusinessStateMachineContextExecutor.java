package top.egon.cola.component.outbox.statemachine;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

import java.util.function.Supplier;

/** Synchronous application hook that binds and restores the trusted business tenant context. */
@Validated
public interface BusinessStateMachineContextExecutor {

    <T> T execute(@NotNull @Valid BusinessStateMachineEvent event, @NotNull Supplier<T> action);
}
