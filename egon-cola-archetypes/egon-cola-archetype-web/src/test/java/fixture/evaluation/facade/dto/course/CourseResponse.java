package fixture.evaluation.facade.dto.course;

import java.io.Serializable;

public record CourseResponse(
        String id,
        String code,
        String name,
        int credit,
        String status) implements Serializable {
}
