package top.egon.cola.archetype.source.lightopen.infrastructure.teaching.repo.dao;

import top.egon.cola.archetype.source.lightopen.infrastructure.teaching.repo.po.ClassCourseSchedulePO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import top.egon.cola.component.common.mybatis.extension.EgonColaMapper;

import java.util.List;

/** MyBatis mapper for class-course schedules. */
@Mapper
public interface ClassCourseScheduleDAO extends EgonColaMapper<ClassCourseSchedulePO> {
    List<ClassCourseSchedulePO> selectBySchoolClassIdOrderByStartsAt(
            @Param("schoolClassId") Long schoolClassId);
}
