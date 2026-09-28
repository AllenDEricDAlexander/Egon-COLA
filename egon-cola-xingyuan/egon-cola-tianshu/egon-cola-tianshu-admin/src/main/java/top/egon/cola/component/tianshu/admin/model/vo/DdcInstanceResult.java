package top.egon.cola.component.tianshu.admin.model.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
public class DdcInstanceResult {

    @JsonProperty("id")
    @NotNull
    private String publicId;
    private String instanceId;
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
