package ${package}.facade.dto.teaching;

import java.util.List;

public record SchoolClassDTO(String id, String name, String gradeName, List<String> userIds) {
}
