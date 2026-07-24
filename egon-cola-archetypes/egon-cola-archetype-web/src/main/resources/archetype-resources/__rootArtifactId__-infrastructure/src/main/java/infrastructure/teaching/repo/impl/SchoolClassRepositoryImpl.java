package ${package}.infrastructure.teaching.repo.impl;

import ${package}.domain.teaching.entities.SchoolClass;
import ${package}.domain.exceptions.OrganizationDomainErrorCode;
import ${package}.domain.exceptions.OrganizationPortException;
import ${package}.domain.teaching.repos.SchoolClassRepository;
import ${package}.domain.teaching.vos.GradeCode;
import ${package}.domain.teaching.vos.SchoolClassId;
import ${package}.domain.user.vos.UserId;
import ${package}.infrastructure.teaching.repo.converter.SchoolClassPOConverter;
import ${package}.infrastructure.teaching.repo.jpa.GradeJpaRepository;
import ${package}.infrastructure.teaching.repo.jpa.SchoolClassJpaRepository;
import ${package}.infrastructure.teaching.repo.jpa.SchoolClassUserJpaRepository;
import ${package}.infrastructure.teaching.repo.po.SchoolClassPO;
import ${package}.infrastructure.teaching.repo.po.SchoolClassUserPO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository("schoolClassRepositoryImpl")
@RequiredArgsConstructor
public class SchoolClassRepositoryImpl implements SchoolClassRepository {
    private final SchoolClassJpaRepository schoolClassJpaRepository;
    private final GradeJpaRepository gradeJpaRepository;
    private final SchoolClassUserJpaRepository schoolClassUserJpaRepository;
    private final SchoolClassPOConverter converter;

    @Override public SchoolClass save(SchoolClass schoolClass) {
        try {
            return restore(schoolClassJpaRepository.save(converter.toPO(schoolClass)));
        } catch (DataIntegrityViolationException exception) {
            throw conflict("school class persistence conflict", exception);
        }
    }

    @Override public Optional<SchoolClass> findByGradeIdAndId(
            String gradeId,
            SchoolClassId schoolClassId) {
        return schoolClassJpaRepository
                .findByGradeIdAndId(gradeId, schoolClassId.value())
                .map(this::restore);
    }

    @Override public boolean existsByGradeIdAndNameIgnoreCase(String gradeId, String name) {
        return schoolClassJpaRepository.existsByGradeIdAndNameIgnoreCase(gradeId, name);
    }

    @Override public void addUser(
            String gradeId,
            SchoolClassId schoolClassId,
            UserId userId) {
        try {
            schoolClassUserJpaRepository.saveAndFlush(
                new SchoolClassUserPO(
                        gradeId,
                        schoolClassId.value(),
                        userId.value(),
                        LocalDateTime.now()));
        } catch (DataIntegrityViolationException exception) {
            throw conflict("school class membership conflict", exception);
        }
    }

    @Override public boolean hasUser(
            String gradeId,
            SchoolClassId schoolClassId,
            UserId userId) {
        return schoolClassUserJpaRepository.existsByGradeIdAndSchoolClassIdAndUserId(
            gradeId, schoolClassId.value(), userId.value());
    }

    private SchoolClass restore(SchoolClassPO schoolClassPO) {
        GradeCode gradeCode = gradeJpaRepository.findById(schoolClassPO.getGradeId())
            .map(grade -> grade.getId().startsWith("legacy:")
                ? GradeCode.restoreLegacy(grade.getCode()) : GradeCode.create(grade.getCode()))
            .orElseThrow(() -> new OrganizationPortException(
                OrganizationDomainErrorCode.DEPENDENCY_UNAVAILABLE, "grade row missing",
                new IllegalStateException("grade row missing")));
        List<UserId> userIds = schoolClassUserJpaRepository
            .findByGradeIdAndSchoolClassId(schoolClassPO.getGradeId(), schoolClassPO.getId()).stream()
            .map(SchoolClassUserPO::getUserId).map(UserId::new).toList();
        return converter.toEntity(schoolClassPO, gradeCode, userIds);
    }

    private static OrganizationPortException conflict(String message, Exception exception) {
        return new OrganizationPortException(OrganizationDomainErrorCode.CONFLICT, message, exception);
    }
}
