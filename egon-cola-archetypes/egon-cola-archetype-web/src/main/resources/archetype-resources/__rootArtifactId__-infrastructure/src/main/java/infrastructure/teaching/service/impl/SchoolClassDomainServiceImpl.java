package ${package}.infrastructure.teaching.service.impl;

import ${package}.domain.exceptions.OrganizationDomainErrorCode;
import ${package}.domain.exceptions.OrganizationDomainException;
import ${package}.domain.teaching.entities.Grade;
import ${package}.domain.teaching.entities.SchoolClass;
import ${package}.domain.teaching.enums.GradeStatus;
import ${package}.domain.teaching.enums.SchoolClassStatus;
import ${package}.domain.teaching.service.SchoolClassDomainService;
import ${package}.domain.teaching.validators.TeachingDomainValidator;
import ${package}.domain.teaching.vos.GradeCode;
import ${package}.domain.teaching.vos.SchoolClassId;
import ${package}.domain.user.vos.UserId;
import ${package}.infrastructure.teaching.repo.converter.SchoolClassPOConverter;
import ${package}.infrastructure.teaching.repo.dao.GradeDAO;
import ${package}.infrastructure.teaching.repo.dao.SchoolClassDAO;
import ${package}.infrastructure.teaching.repo.dao.SchoolClassUserDAO;
import ${package}.infrastructure.teaching.repo.po.SchoolClassPO;
import ${package}.infrastructure.teaching.repo.po.SchoolClassUserPO;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;
import top.egon.cola.component.common.mybatis.business.EgonColaTenantIdProvider;
import top.egon.cola.component.common.mybatis.extension.EgonColaServiceImpl;
import top.egon.cola.component.common.mybatis.model.EgonColaModelValidationUtils;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service("schoolClassDomainService")
@RequiredArgsConstructor
public class SchoolClassDomainServiceImpl
        extends EgonColaServiceImpl<SchoolClassDAO, SchoolClassPO>
        implements SchoolClassDomainService<SchoolClassPO> {

    @Qualifier("schoolClassDAO")
    private final SchoolClassDAO schoolClassDAO;
    @Qualifier("gradeDAO")
    private final GradeDAO gradeDAO;
    @Qualifier("schoolClassUserDAO")
    private final SchoolClassUserDAO schoolClassUserDAO;
    @Qualifier("schoolClassPOConverter")
    private final SchoolClassPOConverter converter;
    @Qualifier("longIdGenerator")
    private final LongIdGenerator idGenerator;
    @Getter(AccessLevel.PROTECTED)
    @Qualifier("egonColaModelValidationUtils")
    private final EgonColaModelValidationUtils modelValidationUtils;
    @Getter(AccessLevel.PROTECTED)
    @Qualifier("egonColaMdcTenantIdProvider")
    private final EgonColaTenantIdProvider tenantIdProvider;
    @Getter(AccessLevel.PROTECTED)
    @Qualifier("egon.cola.component.mybatis-plus-top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties")
    private final EgonColaMybatisPlusProperties properties;

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
        return Optional.ofNullable(gradeDAO.selectByCode(code.value()))
                .map(grade -> new Grade(grade.getId(), GradeCode.create(grade.getCode()),
                        grade.getName(), GradeStatus.valueOf(grade.getStatus())));
    }

    @Override
    public Optional<SchoolClass> findByGradeIdAndId(Long gradeId, SchoolClassId schoolClassId) {
        return Optional.ofNullable(schoolClassDAO.selectByGradeIdAndId(gradeId, schoolClassId.value()))
                .map(this::restore);
    }

    @Override
    public boolean existsByGradeIdAndNameIgnoreCase(Long gradeId, String name) {
        return schoolClassDAO.countByGradeIdAndNameIgnoreCase(gradeId, name) > 0;
    }

    @Override
    public SchoolClass save(SchoolClass schoolClass) {
        SchoolClassPO po = converter.toTarget(schoolClass);
        SchoolClassPO existing = schoolClassDAO.selectById(po.getId());
        boolean saved;
        if (existing == null) {
            saved = schoolClassDAO.insert(po) == 1;
        } else {
            copyMetadata(existing, po);
            saved = schoolClassDAO.updateById(po) == 1;
        }
        if (!saved) {
            throw new IllegalStateException("save school class affected zero rows");
        }
        return restore(po);
    }

    @Override
    public void addUser(Long gradeId, SchoolClassId schoolClassId, UserId userId) {
        SchoolClassUserPO po = SchoolClassUserPO.builder().gradeId(gradeId)
                .schoolClassId(schoolClassId.value()).userId(userId.value()).build();
        po.setId(idGenerator.nextLongId());
        schoolClassUserDAO.insert(po);
    }

    @Override
    public boolean hasUser(Long gradeId, SchoolClassId schoolClassId, UserId userId) {
        return schoolClassUserDAO.countByGradeIdAndSchoolClassIdAndUserId(
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
        GradeCode gradeCode = Optional.ofNullable(gradeDAO.selectById(po.getGradeId()))
                .map(grade -> GradeCode.create(grade.getCode()))
                .orElseThrow(() -> new OrganizationDomainException(
                        OrganizationDomainErrorCode.DEPENDENCY_UNAVAILABLE, "grade row missing"));
        List<UserId> userIds = schoolClassUserDAO.selectByGradeIdAndSchoolClassId(
                        po.getGradeId(), po.getId()).stream()
                .map(SchoolClassUserPO::getUserId).map(UserId::new).toList();
        return converter.toEntity(po, gradeCode, userIds);
    }

    private static void copyMetadata(SchoolClassPO source, SchoolClassPO target) {
        target.setTenantId(source.getTenantId());
        target.setCreateUserId(source.getCreateUserId());
        target.setCreateTime(source.getCreateTime());
        target.setUpdateUserId(source.getUpdateUserId());
        target.setUpdateTime(source.getUpdateTime());
        target.setIsDeleted(source.getIsDeleted());
    }
}
