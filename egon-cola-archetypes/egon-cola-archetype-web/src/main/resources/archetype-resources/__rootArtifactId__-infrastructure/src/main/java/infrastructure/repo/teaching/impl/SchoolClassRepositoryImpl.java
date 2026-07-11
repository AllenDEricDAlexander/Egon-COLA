package ${package}.infrastructure.repo.teaching.impl;

import ${package}.domain.entities.teaching.SchoolClass;
import ${package}.domain.exceptions.OrganizationDomainErrorCode;
import ${package}.domain.exceptions.OrganizationPortException;
import ${package}.domain.repos.teaching.SchoolClassRepository;
import ${package}.domain.vos.teaching.GradeCode;
import ${package}.domain.vos.teaching.SchoolClassId;
import ${package}.domain.vos.user.UserId;
import ${package}.infrastructure.repo.teaching.converter.SchoolClassPOConverter;
import ${package}.infrastructure.repo.teaching.jpa.GradeJpaRepository;
import ${package}.infrastructure.repo.teaching.jpa.SchoolClassJpaRepository;
import ${package}.infrastructure.repo.teaching.jpa.SchoolClassUserJpaRepository;
import ${package}.infrastructure.repo.teaching.po.SchoolClassPO;
import ${package}.infrastructure.repo.teaching.po.SchoolClassUserPO;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository("schoolClassRepositoryImpl")
public class SchoolClassRepositoryImpl implements SchoolClassRepository {
    private final SchoolClassJpaRepository schoolClassJpaRepository;
    private final GradeJpaRepository gradeJpaRepository;
    private final SchoolClassUserJpaRepository schoolClassUserJpaRepository;
    private final SchoolClassPOConverter converter;

    public SchoolClassRepositoryImpl(
            SchoolClassJpaRepository schoolClassJpaRepository,
            GradeJpaRepository gradeJpaRepository,
            SchoolClassUserJpaRepository schoolClassUserJpaRepository,
            SchoolClassPOConverter converter) {
        this.schoolClassJpaRepository = schoolClassJpaRepository;
        this.gradeJpaRepository = gradeJpaRepository;
        this.schoolClassUserJpaRepository = schoolClassUserJpaRepository;
        this.converter = converter;
    }

    @Override public SchoolClass save(SchoolClass schoolClass) {
        try {
            return restore(schoolClassJpaRepository.save(converter.toPO(schoolClass)));
        } catch (DataIntegrityViolationException exception) {
            throw conflict("school class persistence conflict", exception);
        }
    }

    @Override public Optional<SchoolClass> findById(SchoolClassId schoolClassId) {
        return schoolClassJpaRepository.findById(schoolClassId.value()).map(this::restore);
    }

    @Override public boolean existsByGradeIdAndNameIgnoreCase(String gradeId, String name) {
        return schoolClassJpaRepository.existsByGradeIdAndNameIgnoreCase(gradeId, name);
    }

    @Override public void addUser(SchoolClassId schoolClassId, UserId userId) {
        try {
            schoolClassUserJpaRepository.saveAndFlush(
                new SchoolClassUserPO(userId.value(), schoolClassId.value(), LocalDateTime.now()));
        } catch (DataIntegrityViolationException exception) {
            throw conflict("school class membership conflict", exception);
        }
    }

    @Override public boolean hasUser(SchoolClassId schoolClassId, UserId userId) {
        return schoolClassUserJpaRepository.existsBySchoolClassIdAndUserId(
            schoolClassId.value(), userId.value());
    }

    private SchoolClass restore(SchoolClassPO schoolClassPO) {
        GradeCode gradeCode = gradeJpaRepository.findById(schoolClassPO.getGradeId())
            .map(grade -> grade.getId().startsWith("legacy:")
                ? GradeCode.restoreLegacy(grade.getCode()) : GradeCode.create(grade.getCode()))
            .orElseThrow(() -> new OrganizationPortException(
                OrganizationDomainErrorCode.DEPENDENCY_UNAVAILABLE, "grade row missing",
                new IllegalStateException("grade row missing")));
        List<UserId> userIds = schoolClassUserJpaRepository.findBySchoolClassId(schoolClassPO.getId()).stream()
            .map(SchoolClassUserPO::getUserId).map(UserId::new).toList();
        return converter.toEntity(schoolClassPO, gradeCode, userIds);
    }

    private static OrganizationPortException conflict(String message, Exception exception) {
        return new OrganizationPortException(OrganizationDomainErrorCode.CONFLICT, message, exception);
    }
}
