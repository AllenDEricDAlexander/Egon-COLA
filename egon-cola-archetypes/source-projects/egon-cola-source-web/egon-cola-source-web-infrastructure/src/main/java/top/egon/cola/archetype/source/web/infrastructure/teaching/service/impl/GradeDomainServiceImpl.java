package top.egon.cola.archetype.source.web.infrastructure.teaching.service.impl;

import top.egon.cola.archetype.source.web.domain.teaching.entities.Grade;
import top.egon.cola.archetype.source.web.domain.teaching.enums.GradeStatus;
import top.egon.cola.archetype.source.web.domain.teaching.service.GradeDomainService;
import top.egon.cola.archetype.source.web.domain.teaching.vos.GradeCode;
import top.egon.cola.archetype.source.web.infrastructure.teaching.repo.converter.GradePOConverter;
import top.egon.cola.archetype.source.web.infrastructure.teaching.repo.dao.GradeDAO;
import top.egon.cola.archetype.source.web.infrastructure.teaching.repo.po.GradePO;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;
import top.egon.cola.component.common.mybatis.business.EgonColaTenantIdProvider;
import top.egon.cola.component.common.mybatis.extension.EgonColaServiceImpl;
import top.egon.cola.component.common.mybatis.model.EgonColaModelValidationUtils;

import java.util.Optional;

@Slf4j
@Service("gradeDomainService")
@RequiredArgsConstructor
public class GradeDomainServiceImpl
        extends EgonColaServiceImpl<GradeDAO, GradePO>
        implements GradeDomainService<GradePO> {

    @Qualifier("gradeDAO")
    private final GradeDAO gradeDAO;
    @Qualifier("gradePOConverter")
    private final GradePOConverter converter;
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
    public Grade create(Long gradeId, String code, String name) {
        return new Grade(gradeId, GradeCode.create(code), name, GradeStatus.ACTIVE);
    }

    @Override
    public Optional<Grade> findById(Long gradeId) {
        return Optional.ofNullable(getById(gradeId)).map(converter::toSource);
    }

    @Override
    public Optional<Grade> findByCode(GradeCode code) {
        return Optional.ofNullable(gradeDAO.selectByCode(code.value())).map(converter::toSource);
    }

    @Override
    public boolean existsByCode(GradeCode code) {
        return gradeDAO.countByCode(code.value()) > 0;
    }

    @Override
    public Grade save(Grade grade) {
        GradePO po = converter.toTarget(grade);
        GradePO existing = gradeDAO.selectById(po.getId());
        boolean saved;
        if (existing == null) {
            saved = gradeDAO.insert(po) == 1;
        } else {
            copyMetadata(existing, po);
            saved = gradeDAO.updateById(po) == 1;
        }
        if (!saved) {
            throw new IllegalStateException("save grade affected zero rows");
        }
        return converter.toSource(po);
    }

    private static void copyMetadata(GradePO source, GradePO target) {
        target.setTenantId(source.getTenantId());
        target.setCreateUserId(source.getCreateUserId());
        target.setCreateTime(source.getCreateTime());
        target.setUpdateUserId(source.getUpdateUserId());
        target.setUpdateTime(source.getUpdateTime());
        target.setIsDeleted(source.getIsDeleted());
    }
}
