package ${package}.application.manage.user.impl;

import ${package}.application.manage.user.UserManage;
import ${package}.common.constants.ErrorCodes;
import ${package}.common.exceptions.BizException;
import ${package}.common.exceptions.NotFoundException;
import ${package}.common.utils.IdGenerator;
import ${package}.domain.entities.user.User;
import ${package}.domain.repos.user.UserRepository;
import ${package}.domain.service.user.UserDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("userManage")
@RequiredArgsConstructor
public class UserManageImpl implements UserManage {
    @Qualifier("userRepositoryImpl")
    private final UserRepository userRepository;

    @Qualifier("userDomainService")
    private final UserDomainService userDomainService;

    @Override
    @Transactional
    public User create(String name, String email) {
        if (userRepository.existsByEmail(email)) {
            throw new BizException(ErrorCodes.USER_EMAIL_DUPLICATED, "user email already exists");
        }
        User user = userDomainService.create(IdGenerator.nextId(), name, email);
        return userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public User getById(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.USER_NOT_FOUND, "user not found"));
    }
}
