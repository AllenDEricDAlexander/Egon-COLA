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
@Table(name = "ddc_biz")
public class DdcBizEntity {

    @Id
    private String id;

    @Column(name = "biz_code", nullable = false, unique = true, length = 128)
    private String bizCode;

    @Column(name = "biz_name", nullable = false, length = 128)
    private String bizName;

    private String description;

    private Boolean enabled;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
