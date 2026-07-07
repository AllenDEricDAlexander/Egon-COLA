package top.egon.cola.component.ddc.admin.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DdcConfigRollbackRequest {

    private String configId;

    private Long version;

    private String reason;
}
