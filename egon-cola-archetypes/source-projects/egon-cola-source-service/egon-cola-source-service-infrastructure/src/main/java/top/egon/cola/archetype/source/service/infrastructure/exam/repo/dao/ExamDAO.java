package top.egon.cola.archetype.source.service.infrastructure.exam.repo.dao;

import top.egon.cola.archetype.source.service.infrastructure.exam.repo.po.ExamPO;
import org.apache.ibatis.annotations.Mapper;
import top.egon.cola.component.common.mybatis.extension.EgonColaMapper;

@Mapper
public interface ExamDAO extends EgonColaMapper<ExamPO> {
}
