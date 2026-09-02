package top.egon.cola.archetype.source.web.infrastructure.teaching.repo.converter;

import top.egon.cola.archetype.source.web.domain.teaching.entities.Grade;
import top.egon.cola.archetype.source.web.domain.teaching.enums.GradeStatus;
import top.egon.cola.archetype.source.web.domain.teaching.vos.GradeCode;
import top.egon.cola.archetype.source.web.infrastructure.teaching.repo.po.GradePO;
import org.springframework.stereotype.Component;
import top.egon.cola.component.common.core.converter.BaseConverter;

@Component("gradePOConverter")
public final class GradePOConverter implements BaseConverter<Grade, GradePO> {
    @Override
    public GradePO toTarget(Grade grade) {
        GradePO target = GradePO.builder().code(grade.code().value()).name(grade.name())
                .status(grade.status().name()).build();
        target.setId(grade.id());
        return target;
    }

    @Override
    public Grade toSource(GradePO target) {
        return new Grade(target.getId(), GradeCode.create(target.getCode()), target.getName(),
                GradeStatus.valueOf(target.getStatus()));
    }

    public GradePO toPO(Grade grade) {
        return toTarget(grade);
    }
}
