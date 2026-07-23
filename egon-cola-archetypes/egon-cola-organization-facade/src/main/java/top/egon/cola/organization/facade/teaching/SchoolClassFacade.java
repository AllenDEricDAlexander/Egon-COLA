package top.egon.cola.organization.facade.teaching;

import top.egon.cola.organization.facade.teaching.dto.AssignUserToClassDTO;
import top.egon.cola.organization.facade.teaching.dto.CreateSchoolClassDTO;
import top.egon.cola.organization.facade.teaching.dto.SchoolClassDetailDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public interface SchoolClassFacade {
    SchoolClassDetailDTO createSchoolClass(@Valid @NotNull CreateSchoolClassDTO request);
    SchoolClassDetailDTO getSchoolClass(@NotBlank String schoolClassId);
    void assignUser(@Valid @NotNull AssignUserToClassDTO request);
}
