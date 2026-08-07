package top.egon.cola.component.ddc.admin.model.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DdcConfigQueryRequest {

    private String bizCode;

    private String namespaceCode;

    private String env;

    private String appCode;

    private boolean includeDeleted;
}
