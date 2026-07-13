package fixture.evaluation.facade.exam.dto;

import java.io.Serializable;
import java.time.Instant;

public record ExamResponse(
        String id,
        String courseId,
        String title,
        Instant startsAt,
        Instant endsAt,
        String status) implements Serializable {
}
