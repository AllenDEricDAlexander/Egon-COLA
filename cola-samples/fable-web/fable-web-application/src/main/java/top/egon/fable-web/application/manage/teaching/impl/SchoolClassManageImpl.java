package top.egon.fable-web.application.manage.teaching.impl;

import top.egon.fable-web.application.manage.teaching.SchoolClassManage;
import top.egon.fable-web.application.manage.teaching.SchoolClassView;
import top.egon.fable-web.common.constants.ErrorCodes;
import top.egon.fable-web.common.exceptions.BizException;
import top.egon.fable-web.common.exceptions.NotFoundException;
import top.egon.fable-web.common.utils.IdGenerator;
import top.egon.fable-web.domain.entities.teaching.SchoolClass;
import top.egon.fable-web.domain.entities.user.User;
import top.egon.fable-web.domain.repos.teaching.SchoolClassRepository;
import top.egon.fable-web.domain.repos.user.UserRepository;
import top.egon.fable-web.domain.service.teaching.SchoolClassDomainService;
import top.egon.fable-web.domain.service.user.UserDomainService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SchoolClassManageImpl implements SchoolClassManage {
    private final SchoolClassRepository schoolClassRepository;
    private final UserRepository userRepository;
    private final SchoolClassDomainService schoolClassDomainService = new SchoolClassDomainService();
    private final UserDomainService userDomainService = new UserDomainService();

    public SchoolClassManageImpl(SchoolClassRepository schoolClassRepository, UserRepository userRepository) {
        this.schoolClassRepository = schoolClassRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public SchoolClassView create(String name, String gradeName) {
        SchoolClass schoolClass = schoolClassDomainService.create(IdGenerator.nextId(), name, gradeName);
        return toView(schoolClassRepository.save(schoolClass));
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

    private SchoolClassView toView(SchoolClass schoolClass) {
        return new SchoolClassView(schoolClass.getId(), schoolClass.getName(), schoolClass.getGradeName(), schoolClass.getUserIds());
    }
}
