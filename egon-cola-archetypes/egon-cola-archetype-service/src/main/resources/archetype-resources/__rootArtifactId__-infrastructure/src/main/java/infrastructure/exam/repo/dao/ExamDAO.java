#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.exam.repo.dao;

import ${package}.infrastructure.exam.repo.po.ExamPO;
import org.apache.ibatis.annotations.Mapper;
import top.egon.cola.component.common.mybatis.extension.EgonColaMapper;

@Mapper
public interface ExamDAO extends EgonColaMapper<ExamPO> {
}
