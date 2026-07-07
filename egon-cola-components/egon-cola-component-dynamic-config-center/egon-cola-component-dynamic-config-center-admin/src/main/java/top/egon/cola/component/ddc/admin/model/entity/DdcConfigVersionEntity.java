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
@Table(name = "ddc_config_version")
public class DdcConfigVersionEntity {

    @Id
    private String id;

    @Column(name = "config_id", nullable = false, length = 64)
    private String configId;

    @Column(name = "app_code", nullable = false, length = 128)
    private String appCode;

    @Column(nullable = false, length = 32)
    private String env;

    @Column(nullable = false, length = 128)
    private String namespace;

    @Column(name = "config_key", nullable = false, length = 256)
    private String configKey;

    @Column(nullable = false)
    private Long version;

    @Column(name = "old_value", columnDefinition = "text")
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "text")
    private String newValue;

    @Column(name = "value_type", length = 32)
    private String valueType;

    @Column(name = "change_type", length = 32)
    private String changeType;

    @Column(name = "change_reason")
    private String changeReason;

    private String operator;

    @Column(name = "operator_ip")
    private String operatorIp;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
