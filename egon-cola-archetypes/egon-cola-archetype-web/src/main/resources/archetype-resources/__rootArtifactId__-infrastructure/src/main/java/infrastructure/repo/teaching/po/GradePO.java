package ${package}.infrastructure.repo.teaching.po;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "grades")
public class GradePO {

    @Id
    @Column(length = 160)
    private String id;

    @Column(nullable = false, unique = true, length = 160)
    private String code;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected GradePO() {
    }

    public GradePO(String id, String code, String name, String status, LocalDateTime createdAt) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.status = status;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }
}
