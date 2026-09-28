package top.egon.cola.component.tianshu.admin.model.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
public class DdcPublishTaskResult {

    @JsonProperty("id")
    @NotNull
    private String publicId;
    private String changeId;
    private String configId;
    private String bizCode;
    private String appCode;
    private String env;
    private String namespace;
    private String resourceName;
    private Long targetVersion;
    private String publishMode;
    private String resourceChecksum;
    private Integer attemptCount;
    private LocalDateTime dispatchedAt;
    private LocalDateTime completedAt;
    private String failureStage;
    private String status;
    private Integer targetCount;
    private Integer ackCount;
    private Integer failedCount;
    private Integer ignoredCount;
    private Integer timeoutCount;
    private Long timeoutMs;
    private String operator;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
