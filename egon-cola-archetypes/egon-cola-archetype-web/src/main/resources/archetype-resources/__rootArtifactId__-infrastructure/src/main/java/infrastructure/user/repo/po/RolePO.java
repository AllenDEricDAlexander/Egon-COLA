package ${package}.infrastructure.user.repo.po;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "roles")
public class RolePO {
    @Id @Column(length = 64) private String id;
    @Column(nullable = false, unique = true, length = 64) private String code;
    @Column(nullable = false, length = 120) private String name;
    @Column(nullable = false, length = 32) private String status;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;

    protected RolePO() {}

    public RolePO(String id, String code, String name, String status, LocalDateTime createdAt) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.status = status;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
