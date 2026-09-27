package top.egon.cola.archetype.source.webopen.infrastructure.user.dao;

import top.egon.cola.archetype.source.webopen.infrastructure.user.po.RolePO;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.validation.annotation.Validated;
import org.apache.ibatis.annotations.Param;
import top.egon.cola.component.common.mybatis.extension.EgonColaMapper;

@Mapper
@Validated
public interface RoleDAO extends EgonColaMapper<RolePO> {
    RolePO selectByCode(@Param("code") String code);
}
