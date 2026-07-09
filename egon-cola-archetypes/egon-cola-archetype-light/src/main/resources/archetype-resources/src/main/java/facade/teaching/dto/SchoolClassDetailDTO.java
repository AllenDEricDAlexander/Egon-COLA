package ${package}.facade.teaching.dto;

import java.io.Serializable;

public record SchoolClassDetailDTO(
        String id,
        String name,
        String semester,
        String status,
        int scheduleCount) implements Serializable {
}
