package top.egon.cola.component.outbox.statemachine;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;

/** Business-application persistence port for one locked aggregate and its durable event receipt. */
@Validated
public interface BusinessStateMachineRepository {

    @NotNull
    @Valid
    BusinessStateMachineSnapshotBO lockAndLoad(
            @NotNull @Valid BusinessStateMachineEvent event,
            @NotBlank @Pattern(regexp = "[0-9a-f]{64}") String fingerprint
    );

    void saveTransition(
            @NotNull @Valid BusinessStateMachineEvent event,
            @NotNull @Valid BusinessStateMachineSnapshotBO snapshot,
            @NotBlank @Size(max = 128) String targetState,
            @NotBlank @Pattern(regexp = "[0-9a-f]{64}") String fingerprint
    );
}
