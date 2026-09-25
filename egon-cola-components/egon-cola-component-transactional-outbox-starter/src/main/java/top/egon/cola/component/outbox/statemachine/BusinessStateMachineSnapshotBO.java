package top.egon.cola.component.outbox.statemachine;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import top.egon.cola.component.common.core.pojo.BasePojo;

/** A locked snapshot of the business state and durable event-consumption receipt. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
public class BusinessStateMachineSnapshotBO implements BasePojo {

    @NotBlank
    @Size(max = 128)
    private String currentState;

    @NotNull
    @PositiveOrZero
    private Long version;

    @NotNull
    @Min(1)
    private Integer definitionVersion;

    @Pattern(regexp = "[0-9a-f]{64}")
    private String appliedFingerprint;

    @NotNull
    @Builder.Default
    private ObjectNode facts = JsonNodeFactory.instance.objectNode();

    public ObjectNode getFacts() {
        return facts == null ? null : facts.deepCopy();
    }

    public BusinessStateMachineSnapshotBO setFacts(ObjectNode facts) {
        this.facts = facts == null ? null : facts.deepCopy();
        return this;
    }
}
