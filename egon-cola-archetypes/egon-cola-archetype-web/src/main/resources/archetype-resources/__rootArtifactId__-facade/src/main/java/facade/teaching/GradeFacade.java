package ${package}.facade.teaching;

import ${package}.facade.teaching.dto.CreateGradeDTO;
import ${package}.facade.teaching.dto.GradeDetailDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public interface GradeFacade {
    GradeDetailDTO createGrade(@Valid @NotNull CreateGradeDTO request);
    GradeDetailDTO getGrade(@NotBlank String gradeId);
}
