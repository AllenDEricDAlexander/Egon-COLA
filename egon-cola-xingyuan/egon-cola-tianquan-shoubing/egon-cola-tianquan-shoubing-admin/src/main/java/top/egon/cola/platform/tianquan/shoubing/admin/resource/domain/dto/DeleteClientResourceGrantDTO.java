package top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import top.egon.cola.platform.tianquan.shoubing.core.resource.ResourceGrantType;

/** Input for deleting a Client Resource Grant. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
public class DeleteClientResourceGrantDTO {

    @NotNull
    private ResourceGrantType grantType;
    private String tenantId;
    @PositiveOrZero
    private long expectedResourceVersion;
    @PositiveOrZero
    private long expectedGrantVersion;
}
