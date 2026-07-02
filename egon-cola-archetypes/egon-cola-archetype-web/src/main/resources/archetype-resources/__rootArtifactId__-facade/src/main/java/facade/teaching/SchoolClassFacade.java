package ${package}.facade.teaching;

import ${package}.facade.dto.teaching.AssignUserToClassRequest;
import ${package}.facade.dto.teaching.CreateSchoolClassRequest;
import ${package}.facade.dto.teaching.SchoolClassDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public interface SchoolClassFacade {
    SchoolClassDTO createSchoolClass(@Valid @NotNull CreateSchoolClassRequest request);

    void assignUser(@Valid @NotNull AssignUserToClassRequest request);
}
