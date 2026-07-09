package ${package}.infrastructure.repo.teaching.po;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "courses")
public class CoursePo {
    @Id
    private String id;
    @Column(name = "course_code")
    private String courseCode;
    private String name;
    private String description;
    private String status;
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    protected CoursePo() {
    }

    public CoursePo(String id, String name, String description, LocalDateTime createdAt) {
        this.id = id;
        this.courseCode = id;
        this.name = name;
        this.description = description;
        this.status = "ACTIVE";
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public String getCourseCode() { return courseCode; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
