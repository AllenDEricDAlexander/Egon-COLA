package ${package}.infrastructure.repo.user.impl;

import ${package}.domain.common.Page;
import ${package}.domain.entities.user.User;
import ${package}.domain.exceptions.OrganizationDomainErrorCode;
import ${package}.domain.exceptions.OrganizationPortException;
import ${package}.domain.repos.user.UserRepository;
import ${package}.domain.vos.user.UserId;
import ${package}.infrastructure.repo.teaching.jpa.SchoolClassUserJpaRepository;
import ${package}.infrastructure.repo.teaching.po.SchoolClassUserPo;
import ${package}.infrastructure.repo.user.converter.UserPOConverter;
import ${package}.infrastructure.repo.user.jpa.UserJpaRepository;
import ${package}.infrastructure.repo.user.po.UserPO;
import org.springframework.data.domain.PageRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository("userRepositoryImpl")
public class UserRepositoryImpl implements UserRepository {

    private final UserJpaRepository userJpaRepository;
    private final SchoolClassUserJpaRepository schoolClassUserJpaRepository;
    private final UserPOConverter converter;

    public UserRepositoryImpl(
            UserJpaRepository userJpaRepository,
            SchoolClassUserJpaRepository schoolClassUserJpaRepository,
            UserPOConverter converter) {
        this.userJpaRepository = userJpaRepository;
        this.schoolClassUserJpaRepository = schoolClassUserJpaRepository;
        this.converter = converter;
    }

    @Override
    public User save(User user) {
        try {
            UserPO saved = userJpaRepository.save(converter.toPO(user));
            user.getSchoolClassIds().forEach(schoolClassId -> saveMembershipIfMissing(user.getId(), schoolClassId));
            return restore(saved);
        } catch (DataIntegrityViolationException exception) {
            throw new OrganizationPortException(
                OrganizationDomainErrorCode.CONFLICT, "user persistence conflict", exception);
        }
    }

    @Override
    public Optional<User> findById(UserId userId) {
        return userJpaRepository.findById(userId.value()).map(this::restore);
    }

    @Override
    public Page<User> findPage(int currentPage, int pageSize) {
        org.springframework.data.domain.Page<UserPO> page =
            userJpaRepository.findAll(PageRequest.of(Math.max(currentPage, 1) - 1, pageSize));
        return Page.of(page.getContent().stream().map(this::restore).toList(),
            currentPage, page.getTotalPages(), pageSize, page.getTotalElements());
    }

    @Override
    public boolean existsByEmail(String normalizedEmail) {
        return userJpaRepository.existsByEmail(normalizedEmail);
    }

    private void saveMembershipIfMissing(String userId, String schoolClassId) {
        if (!schoolClassUserJpaRepository.existsByUserIdAndSchoolClassId(userId, schoolClassId)) {
            schoolClassUserJpaRepository.save(new SchoolClassUserPo(userId, schoolClassId, LocalDateTime.now()));
        }
    }

    private User restore(UserPO userPO) {
        List<String> schoolClassIds = schoolClassUserJpaRepository.findByUserId(userPO.getId()).stream()
            .map(SchoolClassUserPo::getSchoolClassId)
            .toList();
        return converter.toEntity(userPO, schoolClassIds);
    }
}
