package top.egon.cola.archetype.source.serviceopen.infrastructure.exam.dao;

import top.egon.cola.archetype.source.serviceopen.infrastructure.exam.po.ExamPO;
import org.apache.ibatis.annotations.Mapper;
import top.egon.cola.component.common.mybatis.extension.EgonColaMapper;

@Mapper
public interface ExamDAO extends EgonColaMapper<ExamPO> {
}
