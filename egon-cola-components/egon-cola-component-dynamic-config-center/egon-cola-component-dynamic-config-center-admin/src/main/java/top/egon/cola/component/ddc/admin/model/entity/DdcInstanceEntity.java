package top.egon.cola.component.ddc.admin.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "ddc_instance")
public class DdcInstanceEntity {

    @Id
    private String id;

    @Column(name = "instance_id", nullable = false, unique = true, length = 256)
    private String instanceId;

    @Column(name = "app_code", length = 128)
    private String appCode;

    @Column(length = 32)
    private String env;

    @Column(length = 128)
    private String namespace;

    private String host;

    private Integer port;

    private String pid;

    @Column(name = "sdk_version")
    private String sdkVersion;

    private String status;

    @Column(name = "last_heartbeat_at")
    private LocalDateTime lastHeartbeatAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
