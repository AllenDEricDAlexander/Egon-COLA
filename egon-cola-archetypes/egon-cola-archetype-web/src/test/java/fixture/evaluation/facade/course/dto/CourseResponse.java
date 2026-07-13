package fixture.evaluation.facade.course.dto;

import java.io.Serializable;

public record CourseResponse(
        String id,
        String code,
        String name,
        int credit,
        String status) implements Serializable {
}
