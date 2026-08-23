package ${package}.infrastructure.teaching.repo.po;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "school_class_users",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_school_class_user",
                columnNames = {"grade_id", "school_class_id", "user_id"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SchoolClassUserPO {
    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "grade_id", nullable = false, length = 36)
    private String gradeId;

    @Column(name = "school_class_id", nullable = false, length = 36)
    private String schoolClassId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public SchoolClassUserPO(
            String id,
            String gradeId,
            String schoolClassId,
            String userId,
            LocalDateTime createdAt) {
        this.id = id;
        this.gradeId = gradeId;
        this.userId = userId;
        this.schoolClassId = schoolClassId;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getGradeId() {
        return gradeId;
    }

    public String getSchoolClassId() {
        return schoolClassId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
