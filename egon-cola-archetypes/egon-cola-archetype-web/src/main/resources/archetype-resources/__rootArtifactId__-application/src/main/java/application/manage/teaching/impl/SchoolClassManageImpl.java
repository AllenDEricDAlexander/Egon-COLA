package ${package}.application.manage.teaching.impl;

import ${package}.application.manage.teaching.SchoolClassManage;
import ${package}.common.constants.ErrorCodes;
import ${package}.common.exceptions.BizException;
import ${package}.common.exceptions.NotFoundException;
import ${package}.common.utils.IdGenerator;
import ${package}.domain.client.teaching.SchoolClassClient;
import ${package}.domain.client.user.UserClient;
import ${package}.domain.entities.teaching.SchoolClass;
import ${package}.domain.entities.user.User;
import ${package}.domain.service.teaching.SchoolClassDomainService;
import ${package}.domain.service.user.UserDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service("schoolClassManage")
@Validated
@RequiredArgsConstructor
public class SchoolClassManageImpl implements SchoolClassManage {
    @Qualifier("schoolClassClientImpl")
    private final SchoolClassClient schoolClassClient;

    @Qualifier("userClientImpl")
    private final UserClient userClient;

    @Qualifier("schoolClassDomainService")
    private final SchoolClassDomainService schoolClassDomainService;

    @Qualifier("userDomainService")
    private final UserDomainService userDomainService;

    @Override
    @Transactional
    public SchoolClass create(String name, String gradeName) {
        SchoolClass schoolClass = schoolClassDomainService.create(IdGenerator.nextId(), name, gradeName);
        return schoolClassClient.save(schoolClass);
    }

    @Override
    @Transactional
    public void assignUser(String userId, String schoolClassId) {
        User user = userClient.findById(userId)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.USER_NOT_FOUND, "user not found"));
        SchoolClass schoolClass = schoolClassClient.findById(schoolClassId)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.SCHOOL_CLASS_NOT_FOUND, "school class not found"));
        if (schoolClass.hasUser(userId) || user.hasSchoolClass(schoolClassId)) {
            throw new BizException(ErrorCodes.SCHOOL_CLASS_USER_DUPLICATED, "user already assigned to school class");
        }
        schoolClassClient.save(schoolClassDomainService.assignUser(schoolClass, userId));
        userClient.save(userDomainService.assignClass(user, schoolClassId));
    }
}
