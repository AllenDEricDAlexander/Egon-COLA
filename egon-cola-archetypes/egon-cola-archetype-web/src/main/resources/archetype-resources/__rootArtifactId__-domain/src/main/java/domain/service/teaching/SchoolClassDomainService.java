package ${package}.domain.service.teaching;

import ${package}.common.constants.ErrorCodes;
import ${package}.common.exceptions.BizException;
import ${package}.domain.entities.teaching.SchoolClass;

public class SchoolClassDomainService {
    public SchoolClass create(String schoolClassId, String name, String gradeName) {
        return SchoolClass.create(schoolClassId, name, gradeName);
    }

    public SchoolClass assignUser(SchoolClass schoolClass, String userId) {
        if (schoolClass.hasUser(userId)) {
            throw new BizException(ErrorCodes.SCHOOL_CLASS_USER_DUPLICATED, "user already assigned to school class");
        }
        schoolClass.assignUser(userId);
        return schoolClass;
    }
}
