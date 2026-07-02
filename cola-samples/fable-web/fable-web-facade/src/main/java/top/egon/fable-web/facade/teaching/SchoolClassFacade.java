package top.egon.fable-web.facade.teaching;

import top.egon.fable-web.facade.dto.teaching.AssignUserToClassRequest;
import top.egon.fable-web.facade.dto.teaching.CreateSchoolClassRequest;
import top.egon.fable-web.facade.dto.teaching.SchoolClassDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public interface SchoolClassFacade {
    SchoolClassDTO createSchoolClass(@Valid @NotNull CreateSchoolClassRequest request);

    void assignUser(@Valid @NotNull AssignUserToClassRequest request);
}
