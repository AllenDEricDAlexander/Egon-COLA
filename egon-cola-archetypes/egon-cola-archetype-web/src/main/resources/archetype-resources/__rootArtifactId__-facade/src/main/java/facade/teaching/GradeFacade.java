package ${package}.facade.teaching;

import ${package}.facade.dto.teaching.CreateGradeDTO;
import ${package}.facade.dto.teaching.GradeDetailDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public interface GradeFacade {
    GradeDetailDTO createGrade(@Valid @NotNull CreateGradeDTO request);
    GradeDetailDTO getGrade(@NotBlank String gradeId);
}
