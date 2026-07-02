package ${package}.application.manage.user.impl;

import ${package}.application.manage.user.UserManage;
import ${package}.application.manage.user.UserView;
import ${package}.common.constants.ErrorCodes;
import ${package}.common.exceptions.BizException;
import ${package}.common.exceptions.NotFoundException;
import ${package}.common.utils.IdGenerator;
import ${package}.domain.entities.user.User;
import ${package}.domain.repos.user.UserRepository;
import ${package}.domain.service.user.UserDomainService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserManageImpl implements UserManage {
    private final UserRepository userRepository;
    private final UserDomainService userDomainService = new UserDomainService();

    public UserManageImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public UserView create(String name, String email) {
        if (userRepository.existsByEmail(email)) {
            throw new BizException(ErrorCodes.USER_EMAIL_DUPLICATED, "user email already exists");
        }
        User user = userDomainService.create(IdGenerator.nextId(), name, email);
        return toView(userRepository.save(user));
    }

    @Override
    @Transactional(readOnly = true)
    public UserView getById(String userId) {
        return userRepository.findById(userId)
                .map(this::toView)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.USER_NOT_FOUND, "user not found"));
    }

    private UserView toView(User user) {
        return new UserView(user.getId(), user.getName(), user.getEmail(), user.getStatus().name(), user.getSchoolClassIds());
    }
}
