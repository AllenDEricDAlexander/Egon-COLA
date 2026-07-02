package top.egon.light.infrastructure.repo.student.po;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "students")
public class StudentPo {
    @Id
    private String id;
    private String name;
    private String email;
    private String status;
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    protected StudentPo() {
    }

    public StudentPo(String id, String name, String email, String status, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.status = status;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
