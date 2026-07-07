package top.egon.cola.component.ddc.admin.model.dto;

import lombok.Getter;
import lombok.Setter;
import top.egon.cola.component.ddc.admin.model.enums.PublishMode;

@Getter
@Setter
public class DdcPublishRequest {

    private String appCode;

    private String env;

    private String namespace;

    private String configKey;

    private String configValue;

    private PublishMode publishMode = PublishMode.ASYNC;

    private Long expectedVersion;

    private Long timeoutMs;

    public static DdcPublishRequest invalidForTest(String appCode, String env, String namespace, String configKey) {
        DdcPublishRequest request = new DdcPublishRequest();
        request.setAppCode(appCode);
        request.setEnv(env);
        request.setNamespace(namespace);
        request.setConfigKey(configKey);
        request.setPublishMode(PublishMode.ASYNC);
        return request;
    }
}
