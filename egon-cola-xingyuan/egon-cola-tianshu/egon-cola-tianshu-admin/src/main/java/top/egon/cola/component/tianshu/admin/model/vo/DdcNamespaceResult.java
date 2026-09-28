package top.egon.cola.component.tianshu.admin.model.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DdcNamespaceResult {
    @JsonProperty("id") private String publicId;
    private String bizCode;
    private String namespace;
    private String description;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String namespaceCode;
}
