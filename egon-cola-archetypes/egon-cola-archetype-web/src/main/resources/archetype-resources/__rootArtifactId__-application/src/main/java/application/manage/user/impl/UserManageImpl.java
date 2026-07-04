package ${package}.application.manage.user.impl;

import ${package}.application.manage.user.UserManage;
import ${package}.common.constants.ErrorCodes;
import ${package}.common.exceptions.BizException;
import ${package}.common.exceptions.NotFoundException;
import ${package}.common.utils.IdGenerator;
import ${package}.domain.client.user.UserClient;
import ${package}.domain.common.Page;
import ${package}.domain.entities.user.User;
import ${package}.domain.service.user.UserDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service("userManage")
@Validated
@RequiredArgsConstructor
public class UserManageImpl implements UserManage {
    @Qualifier("userClientImpl")
    private final UserClient userClient;

    @Qualifier("userDomainService")
    private final UserDomainService userDomainService;

    @Override
    @Transactional
    public User create(String name, String email) {
        if (userClient.existsByEmail(email)) {
            throw new BizException(ErrorCodes.USER_EMAIL_DUPLICATED, "user email already exists");
        }
        User user = userDomainService.create(IdGenerator.nextId(), name, email);
        return userClient.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public User getById(String userId) {
        return userClient.findById(userId)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.USER_NOT_FOUND, "user not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<User> getPage(int currentPage, int pageSize) {
        return userClient.findPage(currentPage, pageSize);
    }
}
