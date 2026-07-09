package ${package}.infrastructure.teaching.repo.po;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "school_classes")
public class SchoolClassPO {
    @Id
    private String id;
    @Column(nullable = false)
    private String name;
    @Column(nullable = false)
    private String semester;
    @Column(nullable = false)
    private String status;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected SchoolClassPO() {
    }

    public SchoolClassPO(String id, String name, String semester, String status, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.semester = semester;
        this.status = status;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getSemester() { return semester; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}
