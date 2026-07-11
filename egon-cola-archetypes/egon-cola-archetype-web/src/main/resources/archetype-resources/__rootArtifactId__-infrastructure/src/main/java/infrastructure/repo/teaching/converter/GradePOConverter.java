package ${package}.infrastructure.repo.teaching.converter;

import ${package}.domain.entities.teaching.Grade;
import ${package}.domain.enums.teaching.GradeStatus;
import ${package}.domain.vos.teaching.GradeCode;
import ${package}.infrastructure.repo.teaching.po.GradePO;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component("gradePOConverter")
public final class GradePOConverter {
    public GradePO toPO(Grade grade) {
        return new GradePO(grade.id(), grade.code().value(), grade.name(), grade.status().name(), LocalDateTime.now());
    }

    public Grade toEntity(GradePO gradePO) {
        GradeCode code = gradePO.getId().startsWith("legacy:")
            ? GradeCode.restoreLegacy(gradePO.getCode())
            : GradeCode.create(gradePO.getCode());
        return new Grade(gradePO.getId(), code, gradePO.getName(), GradeStatus.valueOf(gradePO.getStatus()));
    }
}
