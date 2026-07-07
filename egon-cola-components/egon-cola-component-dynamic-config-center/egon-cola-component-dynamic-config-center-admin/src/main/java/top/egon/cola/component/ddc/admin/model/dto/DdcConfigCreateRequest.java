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

    private String appCode;

    private String env;

    private String namespace;

    private String configKey;

    private String configValue;

    private String defaultValue;

    private String valueType;

    private String description;
}
