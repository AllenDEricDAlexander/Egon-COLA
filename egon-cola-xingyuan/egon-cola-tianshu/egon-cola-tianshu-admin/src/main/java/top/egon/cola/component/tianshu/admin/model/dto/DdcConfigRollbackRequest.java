package top.egon.cola.component.tianshu.admin.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
public class DdcConfigRollbackRequest {

    @NotBlank
    @Size(max = 64)
    private String configId;

    @NotNull
    @Positive
    private Long version;

    @Size(max = 512)
    private String reason;
}
