package ${package}.infrastructure.teaching.repo.dao;

import ${package}.infrastructure.teaching.repo.po.SchoolClassPO;
import org.apache.ibatis.annotations.Mapper;
import top.egon.cola.component.common.mybatis.extension.EgonColaMapper;

/** MyBatis mapper for school classes. */
@Mapper
public interface SchoolClassDAO extends EgonColaMapper<SchoolClassPO> {
}
