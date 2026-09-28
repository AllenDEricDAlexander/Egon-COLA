package top.egon.cola.component.tianshu.admin.model.bo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import top.egon.cola.component.common.mybatis.model.EgonColaModelValidationGroups;

import java.time.Instant;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
public class DdcPublishTaskBO {

    @JsonIgnore private Long id;
    @JsonIgnore private Long tenantId;
    @JsonIgnore private String createUserId;
    @JsonIgnore private Instant createTime;
    @JsonIgnore private String updateUserId;
    @JsonIgnore private Instant updateTime;
    @JsonIgnore private LocalDateTime deletedAt;
    @JsonIgnore private Long version;

    @JsonProperty("id")
    @NotNull(groups = EgonColaModelValidationGroups.Persisted.class)
    @Size(max = 64, groups = {EgonColaModelValidationGroups.Insert.class,
            EgonColaModelValidationGroups.Update.class, EgonColaModelValidationGroups.Persisted.class})
    private String publicId;

    @NotNull(groups = {EgonColaModelValidationGroups.Insert.class, EgonColaModelValidationGroups.Update.class,
            EgonColaModelValidationGroups.Persisted.class})
    @Size(max = 64, groups = {EgonColaModelValidationGroups.Insert.class,
            EgonColaModelValidationGroups.Update.class, EgonColaModelValidationGroups.Persisted.class})
    private String changeId;

    @JsonIgnore private Long configItemId;
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
