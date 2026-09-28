package top.egon.cola.component.tianshu.admin.model.dto;

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
public class DdcConfigQueryRequest {

    @Size(max = 128)
    private String bizCode;

    @Size(max = 128)
    private String namespaceCode;

    @Size(max = 32)
    private String env;

    @Size(max = 128)
    private String appCode;

    private boolean includeDeleted;
}
