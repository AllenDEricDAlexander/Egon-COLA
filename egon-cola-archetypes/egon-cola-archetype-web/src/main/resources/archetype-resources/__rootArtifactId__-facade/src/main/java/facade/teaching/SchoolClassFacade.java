package ${package}.facade.teaching;

import ${package}.facade.teaching.dto.AssignUserToClassDTO;
import ${package}.facade.teaching.dto.CreateSchoolClassDTO;
import ${package}.facade.teaching.dto.SchoolClassDetailDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public interface SchoolClassFacade {
    SchoolClassDetailDTO createSchoolClass(@Valid @NotNull CreateSchoolClassDTO request);
    SchoolClassDetailDTO getSchoolClass(@NotBlank String schoolClassId);
    void assignUser(@Valid @NotNull AssignUserToClassDTO request);
}
