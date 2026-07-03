package top.egon.fable.web.application.manage.teaching.impl;

import top.egon.fable.web.application.manage.teaching.SchoolClassManage;
import top.egon.fable.web.common.constants.ErrorCodes;
import top.egon.fable.web.common.exceptions.BizException;
import top.egon.fable.web.common.exceptions.NotFoundException;
import top.egon.fable.web.common.utils.IdGenerator;
import top.egon.fable.web.domain.entities.teaching.SchoolClass;
import top.egon.fable.web.domain.entities.user.User;
import top.egon.fable.web.domain.repos.teaching.SchoolClassRepository;
import top.egon.fable.web.domain.repos.user.UserRepository;
import top.egon.fable.web.domain.service.teaching.SchoolClassDomainService;
import top.egon.fable.web.domain.service.user.UserDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("schoolClassManage")
@RequiredArgsConstructor
public class SchoolClassManageImpl implements SchoolClassManage {
    @Qualifier("schoolClassRepositoryImpl")
    private final SchoolClassRepository schoolClassRepository;

    @Qualifier("userRepositoryImpl")
    private final UserRepository userRepository;

    @Qualifier("schoolClassDomainService")
    private final SchoolClassDomainService schoolClassDomainService;

    @Qualifier("userDomainService")
    private final UserDomainService userDomainService;

    @Override
    @Transactional
    public SchoolClass create(String name, String gradeName) {
        SchoolClass schoolClass = schoolClassDomainService.create(IdGenerator.nextId(), name, gradeName);
        return schoolClassRepository.save(schoolClass);
    }

    @Override
    @Transactional
    public void assignUser(String userId, String schoolClassId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.USER_NOT_FOUND, "user not found"));
        SchoolClass schoolClass = schoolClassRepository.findById(schoolClassId)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.SCHOOL_CLASS_NOT_FOUND, "school class not found"));
        if (schoolClass.hasUser(userId) || user.hasSchoolClass(schoolClassId)) {
            throw new BizException(ErrorCodes.SCHOOL_CLASS_USER_DUPLICATED, "user already assigned to school class");
        }
        schoolClassRepository.save(schoolClassDomainService.assignUser(schoolClass, userId));
        userRepository.save(userDomainService.assignClass(user, schoolClassId));
    }
}
