package ${package}.infrastructure.repo.teaching.impl;

import ${package}.common.constants.ErrorCodes;
import ${package}.common.exceptions.BizException;
import ${package}.domain.entities.teaching.SchoolClass;
import ${package}.domain.repos.teaching.SchoolClassRepository;
import ${package}.infrastructure.repo.teaching.converter.SchoolClassPoConverter;
import ${package}.infrastructure.repo.teaching.jpa.SchoolClassJpaRepository;
import ${package}.infrastructure.repo.teaching.jpa.SchoolClassUserJpaRepository;
import ${package}.infrastructure.repo.teaching.po.SchoolClassPo;
import ${package}.infrastructure.repo.teaching.po.SchoolClassUserPo;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Repository
public class SchoolClassRepositoryImpl implements SchoolClassRepository {
    private final SchoolClassJpaRepository schoolClassJpaRepository;
    private final SchoolClassUserJpaRepository schoolClassUserJpaRepository;

    public SchoolClassRepositoryImpl(
            SchoolClassJpaRepository schoolClassJpaRepository,
            SchoolClassUserJpaRepository schoolClassUserJpaRepository) {
        this.schoolClassJpaRepository = schoolClassJpaRepository;
        this.schoolClassUserJpaRepository = schoolClassUserJpaRepository;
    }

    @Override
    public SchoolClass save(SchoolClass schoolClass) {
        SchoolClassPo saved = schoolClassJpaRepository.save(SchoolClassPoConverter.toPo(schoolClass));
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
        return SchoolClassPoConverter.toEntity(schoolClassPo, userIds);
    }
}
