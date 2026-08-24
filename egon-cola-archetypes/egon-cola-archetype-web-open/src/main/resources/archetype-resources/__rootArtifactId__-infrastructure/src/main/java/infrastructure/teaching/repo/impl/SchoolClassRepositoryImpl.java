package ${package}.infrastructure.teaching.repo.impl;

import ${package}.domain.teaching.entities.SchoolClass;
import ${package}.domain.exceptions.OrganizationDomainErrorCode;
import ${package}.domain.exceptions.OrganizationPortException;
import ${package}.domain.teaching.repos.SchoolClassRepository;
import ${package}.domain.teaching.vos.GradeCode;
import ${package}.domain.teaching.vos.SchoolClassId;
import ${package}.domain.user.vos.UserId;
import ${package}.infrastructure.teaching.repo.converter.SchoolClassPOConverter;
import ${package}.infrastructure.teaching.repo.mapper.GradeMapper;
import ${package}.infrastructure.teaching.repo.mapper.SchoolClassMapper;
import ${package}.infrastructure.teaching.repo.mapper.SchoolClassUserMapper;
import ${package}.infrastructure.teaching.repo.po.SchoolClassPO;
import ${package}.infrastructure.teaching.repo.po.SchoolClassUserPO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.id.generator.LongIdGenerator;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository("schoolClassRepositoryImpl")
@RequiredArgsConstructor
public class SchoolClassRepositoryImpl implements SchoolClassRepository {
    private final SchoolClassMapper schoolClassMapper;
    private final GradeMapper gradeMapper;
    private final SchoolClassUserMapper schoolClassUserMapper;
    private final SchoolClassPOConverter converter;
    private final LongIdGenerator idGenerator;

    @Override
    @Transactional
    public SchoolClass save(SchoolClass schoolClass) {
        try {
            SchoolClassPO schoolClassPO = converter.toPO(schoolClass);
            SchoolClassPO existing = schoolClassMapper.selectByGradeIdAndId(
                    schoolClassPO.getGradeId(), schoolClassPO.getId());
            int affected = existing == null
                    ? schoolClassMapper.insert(schoolClassPO)
                    : schoolClassMapper.updateById(schoolClassPO);
            requireAffected(affected, "save school class");
            return restore(schoolClassPO);
        } catch (DataIntegrityViolationException exception) {
            throw conflict("school class persistence conflict", exception);
        }
    }

    @Override public Optional<SchoolClass> findByGradeIdAndId(
            Long gradeId,
            SchoolClassId schoolClassId) {
        return Optional.ofNullable(schoolClassMapper
                .selectByGradeIdAndId(gradeId, schoolClassId.value()))
                .map(this::restore);
    }

    @Override public boolean existsByGradeIdAndNameIgnoreCase(Long gradeId, String name) {
        return schoolClassMapper.countByGradeIdAndNameIgnoreCase(gradeId, name) > 0;
    }

    @Override
    @Transactional
    public void addUser(
            Long gradeId,
            SchoolClassId schoolClassId,
            UserId userId) {
        try {
            int affected = schoolClassUserMapper.insert(new SchoolClassUserPO(
                    idGenerator.nextLongId(), gradeId, schoolClassId.value(), userId.value(),
                    LocalDateTime.now()));
            requireAffected(affected, "insert school class user");
        } catch (DataIntegrityViolationException exception) {
            throw conflict("school class membership conflict", exception);
        }
    }

    @Override public boolean hasUser(
            Long gradeId,
            SchoolClassId schoolClassId,
            UserId userId) {
        return schoolClassUserMapper.countByGradeIdAndSchoolClassIdAndUserId(
                gradeId, schoolClassId.value(), userId.value()) > 0;
    }

    private SchoolClass restore(SchoolClassPO schoolClassPO) {
        GradeCode gradeCode = Optional.ofNullable(gradeMapper.selectById(schoolClassPO.getGradeId()))
            .map(grade -> GradeCode.create(grade.getCode()))
            .orElseThrow(() -> new OrganizationPortException(
                OrganizationDomainErrorCode.DEPENDENCY_UNAVAILABLE, "grade row missing",
                new IllegalStateException("grade row missing")));
        List<UserId> userIds = schoolClassUserMapper
            .selectByGradeIdAndSchoolClassId(schoolClassPO.getGradeId(), schoolClassPO.getId()).stream()
            .map(SchoolClassUserPO::getUserId).map(UserId::new).toList();
        return converter.toEntity(schoolClassPO, gradeCode, userIds);
    }

    private static OrganizationPortException conflict(String message, Exception exception) {
        return new OrganizationPortException(OrganizationDomainErrorCode.CONFLICT, message, exception);
    }

    private static void requireAffected(int affected, String operation) {
        if (affected != 1) {
            throw new IllegalStateException(operation + " affected " + affected + " rows");
        }
    }
}
