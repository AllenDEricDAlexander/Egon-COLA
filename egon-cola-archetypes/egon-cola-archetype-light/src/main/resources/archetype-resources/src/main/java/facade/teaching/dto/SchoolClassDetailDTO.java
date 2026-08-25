package ${package}.facade.teaching.dto;

import java.io.Serializable;

public record SchoolClassDetailDTO(
        Long id,
        String name,
        String semester,
        String status,
        int scheduleCount) implements Serializable {
}
