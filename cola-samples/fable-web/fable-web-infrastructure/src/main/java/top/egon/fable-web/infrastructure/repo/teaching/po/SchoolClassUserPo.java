package top.egon.fable-web.infrastructure.repo.teaching.po;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "school_class_users",
        uniqueConstraints = @UniqueConstraint(name = "uk_school_class_user", columnNames = {"user_id", "school_class_id"})
)
public class SchoolClassUserPo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Column(name = "school_class_id", nullable = false, length = 64)
    private String schoolClassId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected SchoolClassUserPo() {
    }

    public SchoolClassUserPo(String userId, String schoolClassId, LocalDateTime createdAt) {
        this.userId = userId;
        this.schoolClassId = schoolClassId;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getSchoolClassId() {
        return schoolClassId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
