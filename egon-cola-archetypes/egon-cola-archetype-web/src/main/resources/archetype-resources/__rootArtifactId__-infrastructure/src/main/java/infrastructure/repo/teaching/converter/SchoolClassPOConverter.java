package ${package}.infrastructure.repo.teaching.converter;

import ${package}.domain.entities.teaching.SchoolClass;
import ${package}.domain.enums.teaching.SchoolClassStatus;
import ${package}.domain.vos.teaching.GradeCode;
import ${package}.domain.vos.teaching.SchoolClassId;
import ${package}.domain.vos.user.UserId;
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
