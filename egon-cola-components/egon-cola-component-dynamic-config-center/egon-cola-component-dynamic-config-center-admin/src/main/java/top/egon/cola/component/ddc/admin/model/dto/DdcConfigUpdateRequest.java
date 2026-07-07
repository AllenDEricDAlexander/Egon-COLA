package top.egon.cola.component.ddc.admin.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DdcConfigUpdateRequest {

    private String id;

    private String configValue;

    private String changeReason;

    private Long currentVersion;
}
