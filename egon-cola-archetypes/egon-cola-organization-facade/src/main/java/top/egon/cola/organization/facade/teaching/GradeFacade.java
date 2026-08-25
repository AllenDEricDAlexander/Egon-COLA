package top.egon.cola.organization.facade.teaching;

import top.egon.cola.organization.facade.teaching.dto.CreateGradeDTO;
import top.egon.cola.organization.facade.teaching.dto.GradeDetailDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public interface GradeFacade {
    GradeDetailDTO createGrade(@Valid @NotNull CreateGradeDTO request);
    GradeDetailDTO getGrade(@NotNull @Positive Long gradeId);
}
