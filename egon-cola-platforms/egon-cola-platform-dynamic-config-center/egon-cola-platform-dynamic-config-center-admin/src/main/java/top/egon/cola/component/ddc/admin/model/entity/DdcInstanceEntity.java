package top.egon.cola.component.ddc.admin.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import top.egon.cola.component.ddc.admin.converter.DdcStringMapConverter;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
@Entity
@Table(name = "ddc_instance")
public class DdcInstanceEntity {

    @Id
    private String id;

    @Column(name = "instance_id", nullable = false, unique = true, length = 256)
    private String instanceId;

    @Column(name = "biz_code", nullable = false, length = 128)
    private String bizCode;

    @Column(name = "app_code", length = 128)
    private String appCode;

    @Column(length = 32)
    private String env;

    private String host;

    private Integer port;

    private String pid;

    @Column(name = "sdk_version")
    private String sdkVersion;

    @Column(name = "lease_id", length = 64)
    private String leaseId;

    @Column(name = "lease_expire_at")
    private LocalDateTime leaseExpireAt;

    /** IdP Resource Server 稳定标识；stable IdP Resource Server identifier. */
    @Column(name = "resource_server_id", length = 128)
    private String resourceServerId;

    /** 准入时确认的 Resource 当前版本；current Resource version verified at admission. */
    @Column(name = "resource_version")
    private Long resourceVersion;

    /** 签发准入票据所用公钥凭据标识；public credential identifier used to issue admission. */
    @Column(name = "credential_id", length = 128)
    private String credentialId;

    /** 最近一次验票确认的 Admission Ticket 到期时间；last verified Admission Ticket expiry. */
    @Column(name = "admission_expires_at")
    private LocalDateTime admissionExpiresAt;

    private String status;

    @Column(name = "last_heartbeat_at")
    private LocalDateTime lastHeartbeatAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Convert(converter = DdcStringMapConverter.class)
    @Column(
            name = "runtime_metadata",
            nullable = false,
            columnDefinition = "text"
    )
    private Map<String, String> runtimeMetadata = Map.of();
}
