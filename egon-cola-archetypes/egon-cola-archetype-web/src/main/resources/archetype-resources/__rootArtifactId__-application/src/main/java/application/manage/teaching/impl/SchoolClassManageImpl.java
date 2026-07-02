package ${package}.application.manage.teaching.impl;

import ${package}.application.manage.teaching.SchoolClassManage;
import ${package}.application.manage.teaching.SchoolClassView;
import ${package}.common.constants.ErrorCodes;
import ${package}.common.exceptions.BizException;
import ${package}.common.exceptions.NotFoundException;
import ${package}.common.utils.IdGenerator;
import ${package}.domain.entities.teaching.SchoolClass;
import ${package}.domain.entities.user.User;
import ${package}.domain.repos.teaching.SchoolClassRepository;
import ${package}.domain.repos.user.UserRepository;
import ${package}.domain.service.teaching.SchoolClassDomainService;
import ${package}.domain.service.user.UserDomainService;
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
