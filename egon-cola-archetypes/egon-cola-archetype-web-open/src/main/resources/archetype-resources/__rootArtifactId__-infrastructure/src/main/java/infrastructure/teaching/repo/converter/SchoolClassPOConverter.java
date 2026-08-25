package ${package}.infrastructure.teaching.repo.converter;

import ${package}.domain.teaching.entities.SchoolClass;
import ${package}.domain.teaching.enums.SchoolClassStatus;
import ${package}.domain.teaching.vos.GradeCode;
import ${package}.domain.teaching.vos.SchoolClassId;
import ${package}.domain.user.vos.UserId;
import ${package}.infrastructure.teaching.repo.po.SchoolClassPO;
import org.springframework.stereotype.Component;
import top.egon.cola.component.common.core.converter.BaseConverter;

import java.util.List;

@Component("schoolClassPOConverter")
public final class SchoolClassPOConverter implements BaseConverter<SchoolClass, SchoolClassPO> {
    @Override
    public SchoolClassPO toTarget(SchoolClass schoolClass) {
        SchoolClassPO target = SchoolClassPO.builder().name(schoolClass.name()).gradeName(schoolClass.gradeName())
                .gradeId(schoolClass.gradeId()).status(schoolClass.status().name()).build();
        target.setId(schoolClass.id().value());
        return target;
    }

    @Override
    public SchoolClass toSource(SchoolClassPO target) {
        return toEntity(target, GradeCode.create("UNKNOWN"), List.of());
    }

    public SchoolClass toEntity(SchoolClassPO target, GradeCode gradeCode, List<UserId> userIds) {
        return new SchoolClass(new SchoolClassId(target.getId()), target.getName(), target.getGradeId(),
                gradeCode, target.getGradeName(), SchoolClassStatus.valueOf(target.getStatus()), userIds);
    }

    public SchoolClassPO toPO(SchoolClass schoolClass) {
        return toTarget(schoolClass);
    }
}
