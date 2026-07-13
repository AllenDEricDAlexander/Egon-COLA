package fixture.evaluation.facade.dto.exam;

import java.io.Serializable;

public record ScoreResponse(
        String id,
        String examId,
        String courseId,
        String studentId,
        int points,
        String status) implements Serializable {
}
