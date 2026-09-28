package top.egon.cola.component.tianshu.admin.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
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
}
