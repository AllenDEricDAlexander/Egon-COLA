package [=daoPackage];

import [=poFqn];
import org.apache.ibatis.annotations.Mapper;
[#if includeQuery!false]
import [=domainQueryFqn];
import org.apache.ibatis.annotations.Param;
import java.util.List;
[/#if]
import top.egon.cola.component.common.mybatis.extension.EgonColaMapper;

@Mapper
public interface [=daoType] extends EgonColaMapper<[=poType]> {
[#if includeQuery!false]
    List<[=poType]> selectByQuery(@Param("query") [=domainQueryType] query, @Param("limit") int limit, @Param("offset") long offset);
[/#if]
}
