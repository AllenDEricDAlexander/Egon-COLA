package ${package}.infrastructure.teaching.repo.converter;

import ${package}.domain.teaching.entities.Grade;
import ${package}.domain.teaching.enums.GradeStatus;
import ${package}.domain.teaching.vos.GradeCode;
import ${package}.infrastructure.teaching.repo.po.GradePO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("gradePOConverter")
@RequiredArgsConstructor
public final class GradePOConverter {

    private final GradePOMapper mapper;

    public GradePO toPO(Grade grade) {
        return mapper.convert(grade);
    }

    public Grade toEntity(GradePO gradePO) {
        return new Grade(gradePO.getId(), GradeCode.create(gradePO.getCode()),
            gradePO.getName(), GradeStatus.valueOf(gradePO.getStatus()));
    }
}
