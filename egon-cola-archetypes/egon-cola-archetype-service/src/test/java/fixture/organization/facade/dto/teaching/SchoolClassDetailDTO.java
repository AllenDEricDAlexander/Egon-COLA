package fixture.organization.facade.dto.teaching;

import java.util.List;

public record SchoolClassDetailDTO(
        String id,
        String name,
        String gradeCode,
        String gradeName,
        String status,
        List<String> userIds) {

    public SchoolClassDetailDTO {
        userIds = List.copyOf(userIds);
    }
}
