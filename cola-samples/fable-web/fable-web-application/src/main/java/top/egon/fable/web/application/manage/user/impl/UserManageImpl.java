package top.egon.fable.web.application.manage.user.impl;

import top.egon.fable.web.application.manage.user.UserManage;
import top.egon.fable.web.common.constants.ErrorCodes;
import top.egon.fable.web.common.exceptions.BizException;
import top.egon.fable.web.common.exceptions.NotFoundException;
import top.egon.fable.web.common.utils.IdGenerator;
import top.egon.fable.web.domain.common.Page;
import top.egon.fable.web.domain.entities.user.User;
import top.egon.fable.web.domain.repos.user.UserRepository;
import top.egon.fable.web.domain.service.user.UserDomainService;
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

    @Override
    @Transactional(readOnly = true)
    public Page<User> getPage(int currentPage, int pageSize) {
        return userRepository.findPage(currentPage, pageSize);
    }
}
