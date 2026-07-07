package top.egon.cola.component.ddc.admin.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "ddc_config_item")
public class DdcConfigItemEntity {

    @Id
    private String id;

    @Column(name = "app_code", nullable = false, length = 128)
    private String appCode;

    @Column(nullable = false, length = 32)
    private String env;

    @Column(nullable = false, length = 128)
    private String namespace;

    @Column(name = "config_key", nullable = false, length = 256)
    private String configKey;

    @Column(name = "config_value", columnDefinition = "text")
    private String configValue;

    @Column(name = "default_value", columnDefinition = "text")
    private String defaultValue;

    @Column(name = "value_type", nullable = false, length = 32)
    private String valueType;

    @Column(name = "current_version", nullable = false)
    private Long currentVersion;

    private String description;

    private Boolean enabled;

    private Boolean deleted;

    @Version
    @Column(name = "lock_version")
    private Long lockVersion;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
