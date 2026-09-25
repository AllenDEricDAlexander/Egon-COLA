package top.egon.cola.component.outbox.statemachine;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import top.egon.cola.component.common.core.pojo.BasePojo;

import java.time.Instant;

/** A business fact carried by the transactional outbox state-machine channel. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
public class BusinessStateMachineEvent implements BasePojo {

    @NotBlank
    @Size(max = 64)
    private String eventId;

    @NotNull
    @Positive
    private Long tenantId;

    @NotBlank
    @Pattern(regexp = "[a-z][a-z0-9.-]{0,63}")
    private String machineKey;

    @NotNull
    @Min(1)
    private Integer definitionVersion;

    @NotBlank
    @Size(max = 128)
    @Pattern(regexp = "[^\\p{Cntrl}]+")
    private String businessId;

    @NotBlank
    @Pattern(regexp = "[A-Z][A-Z0-9_]{0,63}")
    private String eventType;

    @PositiveOrZero
    private Long expectedVersion;

    @NotNull
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant occurredAt;

    @NotNull
    private ObjectNode payload;
}
