package top.egon.cola.archetype.source.web.infrastructure.teaching.service.impl;

import top.egon.cola.archetype.source.web.domain.exceptions.OrganizationDomainErrorCode;
import top.egon.cola.archetype.source.web.domain.exceptions.OrganizationDomainException;
import top.egon.cola.archetype.source.web.domain.teaching.entities.Grade;
import top.egon.cola.archetype.source.web.domain.teaching.entities.SchoolClass;
import top.egon.cola.archetype.source.web.domain.teaching.enums.GradeStatus;
import top.egon.cola.archetype.source.web.domain.teaching.enums.SchoolClassStatus;
import top.egon.cola.archetype.source.web.domain.teaching.service.SchoolClassDomainService;
import top.egon.cola.archetype.source.web.domain.teaching.validators.TeachingDomainValidator;
import top.egon.cola.archetype.source.web.domain.teaching.vos.GradeCode;
import top.egon.cola.archetype.source.web.domain.teaching.vos.SchoolClassId;
import top.egon.cola.archetype.source.web.domain.user.vos.UserId;
import top.egon.cola.archetype.source.web.infrastructure.teaching.repo.converter.SchoolClassPOConverter;
import top.egon.cola.archetype.source.web.infrastructure.teaching.repo.GradeRepository;
import top.egon.cola.archetype.source.web.infrastructure.teaching.repo.SchoolClassRepository;
import top.egon.cola.archetype.source.web.infrastructure.teaching.repo.SchoolClassUserRepository;
import top.egon.cola.archetype.source.web.infrastructure.teaching.repo.po.SchoolClassPO;
import top.egon.cola.archetype.source.web.infrastructure.teaching.repo.po.SchoolClassUserPO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.id.snowflake.SnowflakeIdGenerator;

import java.util.List;
import java.util.Optional;

@Slf4j
@Validated
@Service("schoolClassDomainService")
@RequiredArgsConstructor
public class SchoolClassDomainServiceImpl
        implements SchoolClassDomainService {

    @Qualifier("schoolClassRepository")
    private final SchoolClassRepository schoolClassRepository;
    @Qualifier("gradeRepository")
    private final GradeRepository gradeRepository;
    @Qualifier("schoolClassUserRepository")
    private final SchoolClassUserRepository schoolClassUserRepository;
    @Qualifier("schoolClassPOConverterImpl")
    private final SchoolClassPOConverter converter;

    @Override
    public SchoolClass create(SchoolClassId schoolClassId, String name, Grade grade) {
        if (grade.status() == GradeStatus.ARCHIVED) {
            throw new OrganizationDomainException(OrganizationDomainErrorCode.DOMAIN_REJECTED,
                    "archived grade cannot receive school classes");
        }
        return new SchoolClass(schoolClassId, TeachingDomainValidator.normalizeName(name, "school class name"),
                grade.id(), grade.code(), grade.name(), SchoolClassStatus.ACTIVE, List.of());
    }

    @Override
    public Optional<Grade> findGradeByCode(GradeCode code) {
        return Optional.ofNullable(gradeRepository.selectByCode(code.value()))
                .map(grade -> new Grade(grade.getId(), GradeCode.create(grade.getCode()),
                        grade.getName(), GradeStatus.valueOf(grade.getStatus())));
    }

    @Override
    public Optional<SchoolClass> findByGradeIdAndId(Long gradeId, SchoolClassId schoolClassId) {
        return Optional.ofNullable(schoolClassRepository.selectByGradeIdAndId(gradeId, schoolClassId.value()))
                .map(this::restore);
    }

    @Override
    public boolean existsByGradeIdAndNameIgnoreCase(Long gradeId, String name) {
        return schoolClassRepository.countByGradeIdAndNameIgnoreCase(gradeId, name) > 0;
    }

    @Override
    @Transactional
    public SchoolClass save(SchoolClass schoolClass) {
        SchoolClassPO po = converter.toTarget(schoolClass);
        SchoolClassPO existing = schoolClassRepository.getById(po.getId());
        boolean saved;
        if (existing == null) {
            saved = schoolClassRepository.save(po);
        } else {
            converter.updateMetadata(po, existing);
            saved = schoolClassRepository.updateById(po);
        }
        if (!saved) {
            throw new IllegalStateException("save school class affected zero rows");
        }
        return restore(po);
    }

    @Override
    @Transactional
    public void addUser(Long gradeId, SchoolClassId schoolClassId, UserId userId) {
        SchoolClassUserPO po = SchoolClassUserPO.builder().gradeId(gradeId)
                .schoolClassId(schoolClassId.value()).userId(userId.value()).build();
        po.setId(SnowflakeIdGenerator.nextLongId());
        if (!schoolClassUserRepository.save(po)) { throw new IllegalStateException("INSERT_AFFECTED_ZERO_ROWS"); }
    }

    @Override
    public boolean hasUser(Long gradeId, SchoolClassId schoolClassId, UserId userId) {
        return schoolClassUserRepository.countByGradeIdAndSchoolClassIdAndUserId(
                gradeId, schoolClassId.value(), userId.value()) > 0;
    }

    @Override
    public SchoolClass assignUser(SchoolClass schoolClass, UserId userId) {
        if (schoolClass.hasUser(userId)) {
            throw new OrganizationDomainException(OrganizationDomainErrorCode.CONFLICT,
                    "user already assigned to school class");
        }
        schoolClass.assignUser(userId);
        return schoolClass;
    }

    private SchoolClass restore(SchoolClassPO po) {
        GradeCode gradeCode = Optional.ofNullable(gradeRepository.getById(po.getGradeId()))
                .map(grade -> GradeCode.create(grade.getCode()))
                .orElseThrow(() -> new OrganizationDomainException(
                        OrganizationDomainErrorCode.DEPENDENCY_UNAVAILABLE, "grade row missing"));
        List<UserId> userIds = schoolClassUserRepository.selectByGradeIdAndSchoolClassId(
                        po.getGradeId(), po.getId()).stream()
                .map(SchoolClassUserPO::getUserId).map(UserId::new).toList();
        return converter.toEntity(po, gradeCode, userIds);
    }

}
