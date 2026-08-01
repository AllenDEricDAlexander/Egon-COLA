package top.egon.cola.component.ddc.admin.model.vo;

import lombok.Getter;
import lombok.Setter;
import top.egon.cola.component.ddc.admin.model.entity.DdcConfigItemEntity;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class DdcConfigVO {

    private String id;

    private String bizCode;

    private String env;

    private String appCode;

    private List<String> visibleNamespaces = List.of();

    private String configKey;

    private String configValue;

    private String defaultValue;

    private String valueType;

    private Long currentVersion;

    private String description;

    private Boolean enabled;

    private Boolean deleted;

    private LocalDateTime updatedAt;

    public static DdcConfigVO from(DdcConfigItemEntity entity) {
        DdcConfigVO vo = new DdcConfigVO();
        vo.setId(entity.getId());
        vo.setBizCode(entity.getBizCode());
        vo.setAppCode(entity.getAppCode());
        vo.setEnv(entity.getEnv());
        vo.setConfigKey(entity.getConfigKey());
        vo.setConfigValue(entity.getConfigValue());
        vo.setDefaultValue(entity.getDefaultValue());
        vo.setValueType(entity.getValueType());
        vo.setCurrentVersion(entity.getCurrentVersion());
        vo.setDescription(entity.getDescription());
        vo.setEnabled(entity.getEnabled());
        vo.setDeleted(entity.getDeleted());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }
}
