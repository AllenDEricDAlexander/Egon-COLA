package top.egon.cola.component.tianshu.admin.model.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DdcAppResult {
    @JsonProperty("id") private String publicId;
    private String appCode;
    private String bizCode;
    private String appName;
    private String owner;
    private String description;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
