package top.egon.fable.web.infrastructure.repo.teaching.po;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "school_classes")
public class SchoolClassPo {
    @Id
    @Column(length = 64)
    private String id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "grade_name", nullable = false, length = 120)
    private String gradeName;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected SchoolClassPo() {
    }

    public SchoolClassPo(String id, String name, String gradeName, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.gradeName = gradeName;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
