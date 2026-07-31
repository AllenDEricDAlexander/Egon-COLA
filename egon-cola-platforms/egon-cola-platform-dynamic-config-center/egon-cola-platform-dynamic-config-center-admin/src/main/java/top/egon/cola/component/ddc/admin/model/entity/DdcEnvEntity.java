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
@Table(name = "ddc_env")
public class DdcEnvEntity {

    @Id
    private String id;

    @Column(name = "env_code", nullable = false, unique = true, length = 32)
    private String envCode;

    private String description;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    private Boolean enabled;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
