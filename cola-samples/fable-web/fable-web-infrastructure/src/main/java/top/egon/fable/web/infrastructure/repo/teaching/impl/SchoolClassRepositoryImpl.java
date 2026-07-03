package top.egon.fable.web.infrastructure.repo.teaching.impl;

import top.egon.fable.web.common.constants.ErrorCodes;
import top.egon.fable.web.common.exceptions.BizException;
import top.egon.fable.web.domain.entities.teaching.SchoolClass;
import top.egon.fable.web.domain.repos.teaching.SchoolClassRepository;
import top.egon.fable.web.infrastructure.repo.teaching.converter.SchoolClassPoConverter;
import top.egon.fable.web.infrastructure.repo.teaching.jpa.SchoolClassJpaRepository;
import top.egon.fable.web.infrastructure.repo.teaching.jpa.SchoolClassUserJpaRepository;
import top.egon.fable.web.infrastructure.repo.teaching.po.SchoolClassPo;
import top.egon.fable.web.infrastructure.repo.teaching.po.SchoolClassUserPo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Repository("schoolClassRepositoryImpl")
@RequiredArgsConstructor
public class SchoolClassRepositoryImpl implements SchoolClassRepository {
    @Qualifier("schoolClassJpaRepository")
    private final SchoolClassJpaRepository schoolClassJpaRepository;

    @Qualifier("schoolClassUserJpaRepository")
    private final SchoolClassUserJpaRepository schoolClassUserJpaRepository;

    @Qualifier("schoolClassPoConverter")
    private final SchoolClassPoConverter schoolClassPoConverter;

    @Override
    public SchoolClass save(SchoolClass schoolClass) {
        SchoolClassPo saved = schoolClassJpaRepository.save(schoolClassPoConverter.toPo(schoolClass));
        schoolClass.getUserIds().forEach(userId -> {
            if (!schoolClassUserJpaRepository.existsByUserIdAndSchoolClassId(userId, schoolClass.getId())) {
                saveSchoolClassUser(userId, schoolClass.getId());
            }
        });
        return restore(saved);
    }

    @Override
    public Optional<SchoolClass> findById(String schoolClassId) {
        return schoolClassJpaRepository.findById(schoolClassId).map(this::restore);
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

    private SchoolClass restore(SchoolClassPo schoolClassPo) {
        List<String> userIds = schoolClassUserJpaRepository.findBySchoolClassId(schoolClassPo.getId())
                .stream()
                .map(SchoolClassUserPo::getUserId)
                .toList();
        return schoolClassPoConverter.toEntity(schoolClassPo, userIds);
    }
}
