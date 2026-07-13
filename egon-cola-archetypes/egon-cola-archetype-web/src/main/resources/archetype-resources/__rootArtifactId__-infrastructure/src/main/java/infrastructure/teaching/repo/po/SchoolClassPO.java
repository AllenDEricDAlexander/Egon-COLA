package ${package}.infrastructure.teaching.repo.po;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "school_classes")
public class SchoolClassPO {
    @Id
    @Column(length = 64)
    private String id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "grade_name", nullable = false, length = 120)
    private String gradeName;

    @Column(name = "grade_id", nullable = false, length = 160)
    private String gradeId;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected SchoolClassPO() {
    }

    public SchoolClassPO(
            String id,
            String name,
            String gradeName,
            String gradeId,
            String status,
            LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.gradeName = gradeName;
        this.gradeId = gradeId;
        this.status = status;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getGradeName() {
        return gradeName;
    }

    public String getGradeId() {
        return gradeId;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
