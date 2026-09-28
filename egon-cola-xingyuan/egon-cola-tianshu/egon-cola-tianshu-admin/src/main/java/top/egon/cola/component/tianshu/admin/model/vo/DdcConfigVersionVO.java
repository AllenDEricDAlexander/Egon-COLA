package top.egon.cola.component.tianshu.admin.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
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
}
