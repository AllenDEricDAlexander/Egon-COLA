package top.egon.cola.organization.facade.teaching;

import top.egon.cola.organization.facade.teaching.dto.AssignUserToClassDTO;
import top.egon.cola.organization.facade.teaching.dto.CreateSchoolClassDTO;
import top.egon.cola.organization.facade.teaching.dto.SchoolClassDetailDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public interface SchoolClassFacade {
    SchoolClassDetailDTO createSchoolClass(@Valid @NotNull CreateSchoolClassDTO request);
    SchoolClassDetailDTO getSchoolClass(
            @NotNull @Positive Long gradeId,
            @NotNull @Positive Long schoolClassId);
    void assignUser(@Valid @NotNull AssignUserToClassDTO request);
}
