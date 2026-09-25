package top.egon.cola.component.outbox.statemachine;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.statemachine.config.StateMachineFactory;

/** Application-owned state-machine definition and persistence binding for one exact destination. */
public interface BusinessStateMachineDefinitionStrategy {

    @NotBlank
    @Pattern(regexp = "[a-z][a-z0-9.-]{0,63}:[1-9][0-9]*")
    String destination();

    @NotNull
    StateMachineFactory<String, String> stateMachineFactory();

    @NotNull
    BusinessStateMachineRepository repository();

    void validateEvent(@NotNull @Valid BusinessStateMachineEvent event);
}
