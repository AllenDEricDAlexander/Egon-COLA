#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.exam.repo.po;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "exam_paper")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ExamPaperPo {
    @Id private String id;
    @Column(name = "exam_id", nullable = false) private String examId;
    @Column(nullable = false) private String title;
    @Column(name = "total_points", nullable = false) private int totalPoints;
    @Column(nullable = false) private String status;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    public String getId() { return id; } public String getExamId() { return examId; }
    public String getTitle() { return title; } public int getTotalPoints() { return totalPoints; }
    public String getStatus() { return status; } public Instant getCreatedAt() { return createdAt; }
    public void update(String title, int totalPoints, String status, Instant updatedAt) {
        this.title = title;
        this.totalPoints = totalPoints;
        this.status = status;
        this.updatedAt = updatedAt;
    }
}
