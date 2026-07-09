package ${package}.infrastructure.teaching.repo.converter;

import ${package}.domain.teaching.entities.SchoolClass;
import ${package}.domain.teaching.enums.SchoolClassStatus;
import ${package}.domain.teaching.vos.SchoolClassId;
import ${package}.domain.teaching.vos.Semester;
import ${package}.infrastructure.teaching.repo.po.SchoolClassPO;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class SchoolClassPOConverter {
    public SchoolClassPO toPO(SchoolClass schoolClass) {
        return new SchoolClassPO(
                schoolClass.id().value(), schoolClass.name(), schoolClass.semester().value(),
                schoolClass.status().name(), Instant.now());
    }

    public SchoolClass toDomain(SchoolClassPO schoolClass) {
        return new SchoolClass(
                new SchoolClassId(schoolClass.getId()), schoolClass.getName(),
                new Semester(schoolClass.getSemester()),
                SchoolClassStatus.valueOf(schoolClass.getStatus()));
    }
}
