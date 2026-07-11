package ${package}.facade.teaching;

import ${package}.facade.dto.teaching.AssignUserToClassDTO;
import ${package}.facade.dto.teaching.CreateSchoolClassDTO;
import ${package}.facade.dto.teaching.SchoolClassDetailDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public interface SchoolClassFacade {
    SchoolClassDetailDTO createSchoolClass(@Valid @NotNull CreateSchoolClassDTO request);
    SchoolClassDetailDTO getSchoolClass(@NotBlank String schoolClassId);
    void assignUser(@Valid @NotNull AssignUserToClassDTO request);
}
