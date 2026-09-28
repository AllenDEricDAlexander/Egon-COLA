package top.egon.cola.component.tianshu.admin.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class DdcConfigCreateRequest {

    @NotBlank
    @Size(max = 128)
    private String bizCode;

    @NotBlank
    @Size(max = 32)
    private String env;

    @NotBlank
    @Size(max = 128)
    private String appCode;

    @Size(max = 128)
    private String namespaceCode;

    @NotBlank
    @Size(max = 256)
    private String resourceName;

    @NotNull
    private String content;

    @NotBlank
    @Size(max = 32)
    private String format;

    @Size(max = 512)
    private String description;
}
