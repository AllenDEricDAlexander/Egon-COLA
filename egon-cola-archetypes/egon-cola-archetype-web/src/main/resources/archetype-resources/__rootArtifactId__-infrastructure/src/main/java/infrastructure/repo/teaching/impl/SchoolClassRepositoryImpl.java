package ${package}.infrastructure.repo.teaching.impl;

import ${package}.common.constants.ErrorCodes;
import ${package}.common.exceptions.BizException;
import ${package}.domain.entities.teaching.SchoolClass;
import ${package}.domain.repos.teaching.SchoolClassRepository;
import ${package}.infrastructure.repo.teaching.converter.SchoolClassPoConverter;
import ${package}.infrastructure.repo.teaching.jpa.GradeJpaRepository;
import ${package}.infrastructure.repo.teaching.jpa.SchoolClassJpaRepository;
import ${package}.infrastructure.repo.teaching.jpa.SchoolClassUserJpaRepository;
import ${package}.infrastructure.repo.teaching.po.GradePO;
import ${package}.infrastructure.repo.teaching.po.SchoolClassPo;
import ${package}.infrastructure.repo.teaching.po.SchoolClassUserPo;
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

    @Qualifier("gradeJpaRepository")
    private final GradeJpaRepository gradeJpaRepository;

    @Qualifier("schoolClassUserJpaRepository")
    private final SchoolClassUserJpaRepository schoolClassUserJpaRepository;

    @Qualifier("schoolClassPoConverter")
    private final SchoolClassPoConverter schoolClassPoConverter;

    @Override
    public SchoolClass save(SchoolClass schoolClass) {
        SchoolClassPo schoolClassPo = schoolClassPoConverter.toPo(schoolClass);
        ensureLegacyGrade(schoolClassPo);
        SchoolClassPo saved = schoolClassJpaRepository.save(schoolClassPo);
        schoolClass.getUserIds().forEach(userId -> {
            if (!schoolClassUserJpaRepository.existsByUserIdAndSchoolClassId(userId, schoolClass.getId())) {
                saveSchoolClassUser(userId, schoolClass.getId());
            }
        });
        return restore(saved);
    }

    private void ensureLegacyGrade(SchoolClassPo schoolClassPo) {
        if (gradeJpaRepository.existsById(schoolClassPo.getGradeId())) {
            return;
        }
        gradeJpaRepository.save(new GradePO(
                schoolClassPo.getGradeId(),
                schoolClassPo.getGradeName(),
                schoolClassPo.getGradeName(),
                "ACTIVE",
                LocalDateTime.now()));
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
