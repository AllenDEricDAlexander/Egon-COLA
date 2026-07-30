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
@Table(name = "ddc_operation_log")
public class DdcOperationLogEntity {

    @Id
    private String id;

    @Column(name = "app_code", length = 128)
    private String appCode;

    @Column(length = 32)
    private String env;

    @Column(length = 128)
    private String namespace;

    @Column(name = "config_key", length = 256)
    private String configKey;

    @Column(name = "operation_type", length = 64)
    private String operationType;

    private String operator;

    @Column(name = "operator_ip")
    private String operatorIp;

    @Column(name = "operation_content", columnDefinition = "text")
    private String operationContent;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
