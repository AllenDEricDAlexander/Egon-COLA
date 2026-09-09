package top.egon.cola.component.tianshu.admin.model.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DdcPublishRequest {

    private String changeId;

    private String bizCode;

    private String env;

    private String appCode;

    private String resourceName;

    private String content;

    private String format;

    private Long expectedVersion;

    private Long timeoutMs;
}
