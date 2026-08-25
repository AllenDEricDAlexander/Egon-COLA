package ${package}.infrastructure.user.service.impl;

import ${package}.domain.user.aggregates.UserAggregate;
import ${package}.domain.user.entities.User;
import ${package}.domain.user.enums.UserStatus;
import ${package}.domain.user.service.UserDomainService;
import ${package}.domain.user.vos.UserId;
import ${package}.infrastructure.user.repo.converter.UserPOConverter;
import ${package}.infrastructure.user.repo.dao.UserDAO;
import ${package}.infrastructure.user.repo.dao.UserRoleDAO;
import ${package}.infrastructure.user.repo.po.UserPO;
import ${package}.infrastructure.user.repo.po.UserRolePO;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;
import top.egon.cola.component.common.mybatis.business.EgonColaTenantIdProvider;
import top.egon.cola.component.common.mybatis.extension.EgonColaServiceImpl;
import top.egon.cola.component.common.mybatis.model.EgonColaModelValidationUtils;

import java.util.Optional;

/** MyBatis-Plus implementation of the user domain service. */
@Slf4j
@Service("userDomainService")
@RequiredArgsConstructor
public class UserDomainServiceImpl
        extends EgonColaServiceImpl<UserDAO, UserPO>
        implements UserDomainService<UserPO> {

    @Qualifier("userDAO")
    private final UserDAO userDAO;
    @Qualifier("userRoleDAO")
    private final UserRoleDAO userRoleDAO;
    @Qualifier("userPOConverterImpl")
    private final UserPOConverter converter;
    @Qualifier("snowflakeIdGenerator")
    private final LongIdGenerator idGenerator;
    @Getter(AccessLevel.PROTECTED)
    @Qualifier("egonColaModelValidationUtils")
    private final EgonColaModelValidationUtils modelValidationUtils;
    @Getter(AccessLevel.PROTECTED)
    @Qualifier("egonColaMdcTenantIdProvider")
    private final EgonColaTenantIdProvider tenantIdProvider;
    @Getter(AccessLevel.PROTECTED)
    @Qualifier("egon.cola.component.mybatis-plus-top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties")
    private final EgonColaMybatisPlusProperties properties;

    @Override
    public User createUser(String externalId, String name, String email) {
        return new User(new UserId(idGenerator.nextLongId()), externalId, name, email,
                UserStatus.ACTIVE);
    }

    @Override
    @Transactional
    public User save(User user) {
        UserPO po = converter.toTarget(user);
        po.setId(user.id().value());
        userDAO.insert(po);
        return converter.toSource(po);
    }

    @Override
    public Optional<User> findById(UserId userId) {
        return Optional.ofNullable(userDAO.selectById(userId.value())).map(converter::toSource);
    }

    @Override
    @Transactional
    public void saveRoles(UserAggregate aggregate) {
        aggregate.roles().forEach(roleCode -> userRoleDAO.insert(UserRolePO.builder()
                .userId(aggregate.user().id().value())
                .roleCode(roleCode.value())
                .build()));
    }
}
