package top.egon.cola.archetype.source.lightopen.infrastructure.teaching.repo.dao;

import top.egon.cola.archetype.source.lightopen.infrastructure.teaching.repo.po.SchoolClassPO;
import org.apache.ibatis.annotations.Mapper;
import top.egon.cola.component.common.mybatis.extension.EgonColaMapper;

/** MyBatis mapper for school classes. */
@Mapper
/** Shared active reads and versioned deletion are bound by this DAO XML. */
public interface SchoolClassDAO extends EgonColaMapper<SchoolClassPO> {
}
