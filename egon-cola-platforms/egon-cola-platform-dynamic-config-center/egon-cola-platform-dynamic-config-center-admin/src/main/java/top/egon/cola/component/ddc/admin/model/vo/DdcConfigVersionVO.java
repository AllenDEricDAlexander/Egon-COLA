package top.egon.cola.component.ddc.admin.model.vo;

import lombok.Getter;
import lombok.Setter;
import top.egon.cola.component.ddc.admin.model.entity.DdcConfigVersionEntity;

import java.time.LocalDateTime;

@Getter
@Setter
public class DdcConfigVersionVO {

    private String id;

    private String configId;

    private Long version;

    private String resourceName;

    private String oldContent;

    private String newContent;

    private String format;

    private String changeType;

    private String changeReason;

    private String operator;

    private LocalDateTime createdAt;

    public static DdcConfigVersionVO from(DdcConfigVersionEntity entity) {
        DdcConfigVersionVO vo = new DdcConfigVersionVO();
        vo.setId(entity.getId());
        vo.setConfigId(entity.getConfigId());
        vo.setVersion(entity.getVersion());
        vo.setResourceName(entity.getResourceName());
        vo.setOldContent(entity.getOldContent());
        vo.setNewContent(entity.getNewContent());
        vo.setFormat(entity.getFormat());
        vo.setChangeType(entity.getChangeType());
        vo.setChangeReason(entity.getChangeReason());
        vo.setOperator(entity.getOperator());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
