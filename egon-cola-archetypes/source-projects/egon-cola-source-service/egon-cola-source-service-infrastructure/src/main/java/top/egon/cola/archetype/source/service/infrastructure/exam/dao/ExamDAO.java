package top.egon.cola.archetype.source.service.infrastructure.exam.dao;

import top.egon.cola.archetype.source.service.infrastructure.exam.po.ExamPO;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.mybatis.extension.EgonColaMapper;

@Mapper
@Validated
public interface ExamDAO extends EgonColaMapper<ExamPO> {
}
