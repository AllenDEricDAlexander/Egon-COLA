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
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "grade_id", nullable = false)
    private Long gradeId;

    @Column(name = "school_class_id", nullable = false)
    private Long schoolClassId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public SchoolClassUserPO(
            Long id,
            Long gradeId,
            Long schoolClassId,
            Long userId,
            LocalDateTime createdAt) {
        this.id = id;
        this.gradeId = gradeId;
        this.userId = userId;
        this.schoolClassId = schoolClassId;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getGradeId() {
        return gradeId;
    }

    public Long getSchoolClassId() {
        return schoolClassId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
