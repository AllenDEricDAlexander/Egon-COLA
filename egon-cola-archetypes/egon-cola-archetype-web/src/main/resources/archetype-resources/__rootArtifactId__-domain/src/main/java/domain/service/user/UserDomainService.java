package ${package}.domain.service.user;

import ${package}.common.constants.ErrorCodes;
import ${package}.common.exceptions.BizException;
import ${package}.domain.entities.user.User;

public class UserDomainService {
    public User create(String userId, String name, String email) {
        return User.create(userId, name, email);
    }

    public User assignClass(User user, String schoolClassId) {
        if (user.hasSchoolClass(schoolClassId)) {
            throw new BizException(ErrorCodes.SCHOOL_CLASS_USER_DUPLICATED, "user already assigned to school class");
        }
        user.assignToClass(schoolClassId);
        return user;
    }
}
