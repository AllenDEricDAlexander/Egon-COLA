package ${package}.infrastructure.repo.teaching.converter;

import ${package}.domain.teaching.entities.SchoolClass;
import ${package}.domain.teaching.enums.SchoolClassStatus;
import ${package}.domain.teaching.vos.GradeCode;
import ${package}.domain.teaching.vos.SchoolClassId;
import ${package}.domain.user.vos.UserId;
import ${package}.infrastructure.repo.teaching.po.SchoolClassPO;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component("schoolClassPOConverter")
public final class SchoolClassPOConverter {
    public SchoolClassPO toPO(SchoolClass schoolClass) {
        return new SchoolClassPO(schoolClass.id().value(), schoolClass.name(), schoolClass.gradeName(),
            schoolClass.gradeId(), schoolClass.status().name(), LocalDateTime.now());
    }

    public SchoolClass toEntity(SchoolClassPO schoolClassPO, GradeCode gradeCode, List<UserId> userIds) {
        return new SchoolClass(new SchoolClassId(schoolClassPO.getId()), schoolClassPO.getName(),
            schoolClassPO.getGradeId(), gradeCode, schoolClassPO.getGradeName(),
            SchoolClassStatus.valueOf(schoolClassPO.getStatus()), userIds);
    }
}
