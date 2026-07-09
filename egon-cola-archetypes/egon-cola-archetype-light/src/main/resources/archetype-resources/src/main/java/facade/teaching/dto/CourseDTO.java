package ${package}.facade.teaching.dto;

import java.io.Serializable;

public record CourseDTO(String id, String code, String name, String status) implements Serializable {
}
