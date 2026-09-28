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
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
public class DdcInstanceBO {

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
    private String publicId;
    @NotNull(groups = {EgonColaModelValidationGroups.Insert.class, EgonColaModelValidationGroups.Persisted.class})
    @Size(max = 256, groups = {EgonColaModelValidationGroups.Insert.class,
            EgonColaModelValidationGroups.Update.class, EgonColaModelValidationGroups.Persisted.class})
    private String instanceId;
    @NotNull(groups = {EgonColaModelValidationGroups.Insert.class, EgonColaModelValidationGroups.Update.class,
            EgonColaModelValidationGroups.Persisted.class})
    @Size(max = 128, groups = {EgonColaModelValidationGroups.Insert.class,
            EgonColaModelValidationGroups.Update.class, EgonColaModelValidationGroups.Persisted.class})
    private String bizCode;
    private String appCode;
    private String env;
    private String namespace;
    private String host;
    private Integer port;
    private String pid;
    private String sdkVersion;
    private String leaseId;
    private LocalDateTime leaseExpireAt;
    private String resourceServerId;
    private Long resourceVersion;
    private String credentialId;
    private LocalDateTime admissionExpiresAt;
    private String status;
    private LocalDateTime lastHeartbeatAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @Builder.Default private Map<String, String> runtimeMetadata = Map.of();
}
