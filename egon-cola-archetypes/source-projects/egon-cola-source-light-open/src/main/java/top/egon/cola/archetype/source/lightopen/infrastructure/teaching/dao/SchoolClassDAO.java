package top.egon.cola.archetype.source.lightopen.infrastructure.teaching.dao;

import top.egon.cola.archetype.source.lightopen.infrastructure.teaching.po.SchoolClassPO;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.mybatis.extension.EgonColaMapper;

/** MyBatis mapper for school classes. */
@Mapper
@Validated
/** Shared active reads and versioned deletion are bound by this DAO XML. */
public interface SchoolClassDAO extends EgonColaMapper<SchoolClassPO> {
}
