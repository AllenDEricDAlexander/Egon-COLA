package top.egon.cola.archetype.source.lightopen.infrastructure.user.service.impl;

import top.egon.cola.archetype.source.lightopen.domain.user.aggregates.UserAggregate;
import top.egon.cola.archetype.source.lightopen.domain.user.entities.User;
import top.egon.cola.archetype.source.lightopen.domain.user.enums.UserStatus;
import top.egon.cola.archetype.source.lightopen.domain.user.service.UserDomainService;
import top.egon.cola.archetype.source.lightopen.domain.user.vos.UserId;
import top.egon.cola.archetype.source.lightopen.infrastructure.user.repo.converter.UserPOConverter;
import top.egon.cola.archetype.source.lightopen.infrastructure.user.repo.UserRepository;
import top.egon.cola.archetype.source.lightopen.infrastructure.user.repo.UserRoleRepository;
import top.egon.cola.archetype.source.lightopen.infrastructure.user.repo.po.UserPO;
import top.egon.cola.archetype.source.lightopen.infrastructure.user.repo.po.UserRolePO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.id.generator.LongIdGenerator;

import java.util.Optional;

/** Business rules and orchestration for the user domain service. */
@Slf4j
@Validated
@Service("userDomainService")
@RequiredArgsConstructor
public class UserDomainServiceImpl
        implements UserDomainService {

    @Qualifier("userRepository")
    private final UserRepository userRepository;
    @Qualifier("userRoleRepository")
    private final UserRoleRepository userRoleRepository;
    @Qualifier("userPOConverterImpl")
    private final UserPOConverter converter;
    @Qualifier("snowflakeIdGenerator")
    private final LongIdGenerator idGenerator;

    @Override
    public User createUser(String externalId, String name, String email) {
        return new User(new UserId(idGenerator.nextLongId()), externalId, name, email,
                UserStatus.ACTIVE);
    }

    @Override
    @Transactional
    public User save(User user) {
        UserPO po = converter.toTarget(user);
        UserPO current = po.getId() == null ? null : userRepository.getById(po.getId());
        if (current != null) { converter.updateMetadata(po, current); }
        boolean written = current == null ? userRepository.save(po) : userRepository.updateById(po);
        if (!written) { throw new org.springframework.dao.OptimisticLockingFailureException("VERSIONED_WRITE_CONFLICT"); }

        return converter.toSource(po);
    }

    @Override
    public Optional<User> findById(UserId userId) {
        return Optional.ofNullable(userRepository.getById(userId.value())).map(converter::toSource);
    }

    @Override
    @Transactional
    public void saveRoles(UserAggregate aggregate) {
        aggregate.roles().forEach(roleCode -> {
            if (!userRoleRepository.save(UserRolePO.builder()
                .userId(aggregate.user().id().value())
                .roleCode(roleCode.value())
                .build())) { throw new IllegalStateException("INSERT_AFFECTED_ZERO_ROWS"); }
        });
    }

}
