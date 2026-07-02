package top.egon.fable-web.domain.service.user;

import top.egon.fable-web.common.constants.ErrorCodes;
import top.egon.fable-web.common.exceptions.BizException;
import top.egon.fable-web.domain.entities.user.User;

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
