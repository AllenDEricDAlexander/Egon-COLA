package top.egon.cola.component.ddc.admin.model.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DdcConfigQueryRequest {

    private String appCode;

    private String env;

    private String namespace;

    private String configKey;

    private boolean includeDeleted;
}
