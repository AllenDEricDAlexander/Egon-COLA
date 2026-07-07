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
@Table(name = "ddc_publish_task")
public class DdcPublishTaskEntity {

    @Id
    private String id;

    @Column(name = "change_id", nullable = false, unique = true, length = 64)
    private String changeId;

    @Column(name = "config_id", length = 64)
    private String configId;

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

    @Column(name = "publish_mode", length = 32)
    private String publishMode;

    @Column(length = 32)
    private String status;

    @Column(name = "target_count")
    private Integer targetCount;

    @Column(name = "ack_count")
    private Integer ackCount;

    @Column(name = "failed_count")
    private Integer failedCount;

    @Column(name = "ignored_count")
    private Integer ignoredCount;

    @Column(name = "timeout_count")
    private Integer timeoutCount;

    @Column(name = "timeout_ms")
    private Long timeoutMs;

    private String operator;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
