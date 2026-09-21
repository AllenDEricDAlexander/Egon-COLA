package top.egon.cola.archetype.source.web.infrastructure.teaching.service.impl;

import top.egon.cola.archetype.source.web.domain.teaching.entities.Grade;
import top.egon.cola.archetype.source.web.domain.teaching.enums.GradeStatus;
import top.egon.cola.archetype.source.web.domain.teaching.service.GradeDomainService;
import top.egon.cola.archetype.source.web.domain.teaching.vos.GradeCode;
import top.egon.cola.archetype.source.web.infrastructure.teaching.converter.GradePOConverter;
import top.egon.cola.archetype.source.web.infrastructure.teaching.repo.GradeRepository;
import top.egon.cola.archetype.source.web.infrastructure.teaching.po.GradePO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Validated
@Service("gradeDomainService")
@RequiredArgsConstructor
public class GradeDomainServiceImpl
        implements GradeDomainService {

    @Qualifier("gradeRepository")
    private final GradeRepository gradeRepository;
    @Qualifier("gradePOConverterImpl")
    private final GradePOConverter converter;

    @Override
    public Grade create(Long gradeId, String code, String name) {
        return new Grade(gradeId, GradeCode.create(code), name, GradeStatus.ACTIVE);
    }

    @Override
    public Optional<Grade> findById(Long gradeId) {
        return Optional.ofNullable(gradeRepository.findCachedById(gradeId)).map(converter::toSource);
    }

    @Override
    public Optional<Grade> findByCode(GradeCode code) {
        return Optional.ofNullable(gradeRepository.selectByCode(code.value())).map(converter::toSource);
    }

    @Override
    public boolean existsByCode(GradeCode code) {
        return gradeRepository.countByCode(code.value()) > 0;
    }

    @Override
    @Transactional
    public Grade save(Grade grade) {
        GradePO po = converter.toTarget(grade);
        GradePO existing = gradeRepository.getById(po.getId());
        boolean saved;
        if (existing == null) {
            saved = gradeRepository.save(po);
        } else {
            converter.updateMetadata(po, existing);
            saved = gradeRepository.updateCachedById(po);
        }
        if (!saved) {
            throw new IllegalStateException("save grade affected zero rows");
        }
        return converter.toSource(po);
    }

}
