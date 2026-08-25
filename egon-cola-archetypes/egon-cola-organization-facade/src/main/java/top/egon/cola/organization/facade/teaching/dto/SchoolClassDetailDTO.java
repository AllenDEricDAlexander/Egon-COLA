package top.egon.cola.organization.facade.teaching.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

public record SchoolClassDetailDTO(
        @NotNull @Positive Long id, String name, String gradeCode, String gradeName, String status,
        List<@NotNull @Positive Long> userIds) {
    public SchoolClassDetailDTO { userIds = List.copyOf(userIds); }
}
