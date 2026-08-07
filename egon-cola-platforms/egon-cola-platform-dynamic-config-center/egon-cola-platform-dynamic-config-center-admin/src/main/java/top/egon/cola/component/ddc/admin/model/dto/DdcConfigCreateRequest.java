package top.egon.cola.component.ddc.admin.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DdcConfigCreateRequest {

    private String bizCode;

    private String env;

    private String appCode;

    private String namespaceCode;

    private String configValue;

    private String description;
}
