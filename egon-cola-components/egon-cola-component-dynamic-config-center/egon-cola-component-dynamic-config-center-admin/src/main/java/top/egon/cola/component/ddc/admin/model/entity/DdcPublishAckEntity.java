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
@Table(name = "ddc_publish_ack")
public class DdcPublishAckEntity {

    @Id
    private String id;

    @Column(name = "change_id", nullable = false, length = 64)
    private String changeId;

    @Column(name = "instance_id", nullable = false, length = 256)
    private String instanceId;

    @Column(name = "app_code", length = 128)
    private String appCode;

    @Column(length = 32)
    private String env;

    @Column(length = 128)
    private String namespace;

    @Column(name = "config_key", length = 256)
    private String configKey;

    @Column(name = "target_version")
    private Long targetVersion;

    @Column(name = "current_version")
    private Long currentVersion;

    @Column(name = "ack_status", length = 32)
    private String ackStatus;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "ack_at")
    private LocalDateTime ackAt;
}
