package ${package}.infrastructure.teaching.repo.po;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "school_classes")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class SchoolClassPO {
    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "grade_name", nullable = false, length = 120)
    private String gradeName;

    @Column(name = "grade_id", nullable = false, length = 36)
    private String gradeId;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

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
