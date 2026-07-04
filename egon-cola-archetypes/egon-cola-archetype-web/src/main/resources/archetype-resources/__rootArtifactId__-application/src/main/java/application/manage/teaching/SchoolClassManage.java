package ${package}.application.manage.teaching;

import ${package}.domain.entities.teaching.SchoolClass;
import jakarta.validation.constraints.NotBlank;

public interface SchoolClassManage {
    SchoolClass create(@NotBlank String name, @NotBlank String gradeName);

    void assignUser(@NotBlank String userId, @NotBlank String schoolClassId);
}
