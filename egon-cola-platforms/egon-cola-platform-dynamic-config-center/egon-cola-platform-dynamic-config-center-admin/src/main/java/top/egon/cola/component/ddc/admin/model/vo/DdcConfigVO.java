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

    private String resourceName;

    private String content;

    private String format;

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
        vo.setResourceName(entity.getResourceName());
        vo.setContent(entity.getContent());
        vo.setFormat(entity.getFormat());
        vo.setCurrentVersion(entity.getCurrentVersion());
        vo.setDescription(entity.getDescription());
        vo.setEnabled(entity.getEnabled());
        vo.setDeleted(entity.getDeleted());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }
}
