package top.egon.fable.web.infrastructure.repo.user.impl;

import top.egon.fable.web.common.constants.ErrorCodes;
import top.egon.fable.web.common.exceptions.BizException;
import top.egon.fable.web.domain.common.Page;
import top.egon.fable.web.domain.entities.user.User;
import top.egon.fable.web.domain.repos.user.UserRepository;
import top.egon.fable.web.infrastructure.repo.teaching.jpa.SchoolClassUserJpaRepository;
import top.egon.fable.web.infrastructure.repo.teaching.po.SchoolClassUserPo;
import top.egon.fable.web.infrastructure.repo.user.converter.UserPoConverter;
import top.egon.fable.web.infrastructure.repo.user.jpa.UserJpaRepository;
import top.egon.fable.web.infrastructure.repo.user.po.UserPo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Repository("userRepositoryImpl")
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {
    @Qualifier("userJpaRepository")
    private final UserJpaRepository userJpaRepository;

    @Qualifier("schoolClassUserJpaRepository")
    private final SchoolClassUserJpaRepository schoolClassUserJpaRepository;

    @Qualifier("userPoConverter")
    private final UserPoConverter userPoConverter;

    @Override
    public User save(User user) {
        UserPo saved = userJpaRepository.save(userPoConverter.toPo(user));
        user.getSchoolClassIds().forEach(schoolClassId -> {
            if (!schoolClassUserJpaRepository.existsByUserIdAndSchoolClassId(user.getId(), schoolClassId)) {
                saveSchoolClassUser(user.getId(), schoolClassId);
            }
        });
        return restore(saved);
    }

    @Override
    public Optional<User> findById(String userId) {
        return userJpaRepository.findById(userId).map(this::restore);
    }

    @Override
    public Page<User> findPage(int currentPage, int pageSize) {
        Pageable pageable = PageRequest.of(Math.max(currentPage, 1) - 1, pageSize);
        org.springframework.data.domain.Page<UserPo> page = userJpaRepository.findAll(pageable);
        return Page.of(
                page.getContent().stream()
                        .map(this::restore)
                        .toList(),
                currentPage,
                page.getTotalPages(),
                pageSize,
                page.getTotalElements());
    }

    @Override
    public boolean existsByEmail(String email) {
        return userJpaRepository.existsByEmail(email);
    }

    private void saveSchoolClassUser(String userId, String schoolClassId) {
        try {
            schoolClassUserJpaRepository.saveAndFlush(new SchoolClassUserPo(userId, schoolClassId, LocalDateTime.now()));
        } catch (DataIntegrityViolationException exception) {
            if (isDuplicateAssignment(exception)) {
                throw new BizException(ErrorCodes.SCHOOL_CLASS_USER_DUPLICATED, "user already assigned to school class");
            }
            throw exception;
        }
    }

    private boolean isDuplicateAssignment(DataIntegrityViolationException exception) {
        String message = exception.getMessage();
        return message != null && message.toLowerCase(Locale.ROOT).contains("uk_school_class_user");
    }

    private User restore(UserPo userPo) {
        List<String> schoolClassIds = schoolClassUserJpaRepository.findByUserId(userPo.getId())
                .stream()
                .map(SchoolClassUserPo::getSchoolClassId)
                .toList();
        return userPoConverter.toEntity(userPo, schoolClassIds);
    }
}
