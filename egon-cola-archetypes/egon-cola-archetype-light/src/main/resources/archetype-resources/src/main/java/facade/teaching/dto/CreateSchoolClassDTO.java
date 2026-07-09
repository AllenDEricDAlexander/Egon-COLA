package ${package}.facade.teaching.dto;

import java.io.Serializable;

public record CreateSchoolClassDTO(
        String name,
        String semester,
        String operatorId,
        String requestId) implements Serializable {
}
