package [=daoPackage];

import [=poFqn];
import org.apache.ibatis.annotations.Mapper;
import top.egon.cola.component.common.mybatis.extension.EgonColaMapper;

@Mapper
public interface [=daoType] extends EgonColaMapper<[=poType]> {
}
