package top.egon.cola.component.ddc.admin.model.vo;

import lombok.Getter;
import lombok.Setter;
import top.egon.cola.component.ddc.admin.model.entity.DdcConfigItemEntity;

@Getter
@Setter
public class DdcConfigVO {

    private String id;

    private String appCode;

    private String env;

    private String namespace;

    private String configKey;

    private String configValue;

    private String defaultValue;

    private String valueType;

    private Long currentVersion;

    private String description;

    private Boolean enabled;

    private Boolean deleted;

    public static DdcConfigVO from(DdcConfigItemEntity entity) {
        DdcConfigVO vo = new DdcConfigVO();
        vo.setId(entity.getId());
        vo.setAppCode(entity.getAppCode());
        vo.setEnv(entity.getEnv());
        vo.setNamespace(entity.getNamespace());
        vo.setConfigKey(entity.getConfigKey());
        vo.setConfigValue(entity.getConfigValue());
        vo.setDefaultValue(entity.getDefaultValue());
        vo.setValueType(entity.getValueType());
        vo.setCurrentVersion(entity.getCurrentVersion());
        vo.setDescription(entity.getDescription());
        vo.setEnabled(entity.getEnabled());
        vo.setDeleted(entity.getDeleted());
        return vo;
    }
}
