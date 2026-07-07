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
@Table(name = "ddc_namespace")
public class DdcNamespaceEntity {

    @Id
    private String id;

    @Column(name = "app_code", nullable = false, length = 128)
    private String appCode;

    @Column(nullable = false, length = 32)
    private String env;

    @Column(nullable = false, length = 128)
    private String namespace;

    private String description;

    private Boolean enabled;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
