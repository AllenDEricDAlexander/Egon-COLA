package top.egon.cola.component.ddc.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.List;

public class DdcDefaultReportRequest {

    private String bizCode;

    private String appCode;

    private String env;

    private String namespace;

    private String instanceId;

    private List<DdcConfigValueRequest> configs = new ArrayList<>();

    public String getBizCode() {
        return bizCode;
    }

    public void setBizCode(String bizCode) {
        this.bizCode = bizCode;
    }

    public String getAppCode() {
        return appCode;
    }

    public void setAppCode(String appCode) {
        this.appCode = appCode;
    }

    public String getEnv() {
        return env;
    }

    public void setEnv(String env) {
        this.env = env;
    }

    @JsonIgnore
    @Deprecated(forRemoval = true)
    public String getNamespace() {
        return namespace;
    }

    @JsonIgnore
    @Deprecated(forRemoval = true)
    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public List<DdcConfigValueRequest> getConfigs() {
        return configs;
    }

    public void setConfigs(List<DdcConfigValueRequest> configs) {
        this.configs = configs;
    }

    public static class DdcConfigValueRequest {

        private String configKey;

        private String defaultValue;

        private String valueType;

        public String getConfigKey() {
            return configKey;
        }

        public void setConfigKey(String configKey) {
            this.configKey = configKey;
        }

        public String getDefaultValue() {
            return defaultValue;
        }

        public void setDefaultValue(String defaultValue) {
            this.defaultValue = defaultValue;
        }

        public String getValueType() {
            return valueType;
        }

        public void setValueType(String valueType) {
            this.valueType = valueType;
        }
    }
}
