package top.egon.cola.component.tianshu.admin.model.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DdcPublishTaskQueryRequest {

    private String bizCode;

    private String env;

    private String appCode;

    private String status;

    private String changeId;
}
