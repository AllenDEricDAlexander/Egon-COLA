package top.egon.cola.component.tianshu.admin.model.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DdcEnvResult {
    @JsonProperty("id") private String publicId;
    private String envCode;
    private String description;
    private Integer sortOrder;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
