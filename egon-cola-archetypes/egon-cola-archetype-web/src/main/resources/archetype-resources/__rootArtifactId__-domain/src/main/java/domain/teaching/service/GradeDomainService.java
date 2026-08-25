package ${package}.domain.teaching.service;

import ${package}.domain.teaching.entities.Grade;
import ${package}.domain.teaching.vos.GradeCode;
import top.egon.cola.component.common.mybatis.extension.EgonColaIService;
import top.egon.cola.component.common.mybatis.model.EgonModel;

import java.util.Optional;

public interface GradeDomainService<P extends EgonModel<P>> extends EgonColaIService<P> {
    Grade create(Long gradeId, String code, String name);

    Optional<Grade> findById(Long gradeId);

    Optional<Grade> findByCode(GradeCode code);

    boolean existsByCode(GradeCode code);

    Grade save(Grade grade);
}
