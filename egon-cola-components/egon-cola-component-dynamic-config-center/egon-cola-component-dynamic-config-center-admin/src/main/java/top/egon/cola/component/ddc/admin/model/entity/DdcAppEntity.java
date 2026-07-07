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
@Table(name = "ddc_app")
public class DdcAppEntity {

    @Id
    private String id;

    @Column(name = "app_code", nullable = false, unique = true, length = 128)
    private String appCode;

    @Column(name = "app_name", nullable = false, length = 128)
    private String appName;

    private String owner;

    private String description;

    private Boolean enabled;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
