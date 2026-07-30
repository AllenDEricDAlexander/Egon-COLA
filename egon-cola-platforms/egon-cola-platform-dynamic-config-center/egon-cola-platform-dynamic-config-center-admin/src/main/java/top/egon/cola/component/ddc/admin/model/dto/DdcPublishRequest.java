package top.egon.cola.component.ddc.admin.model.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DdcPublishRequest {

    private String changeId;

    private String appCode;

    private String env;

    private String namespace;

    private String configKey;

    private String configValue;

    private Long expectedVersion;

    private Long timeoutMs;
}
