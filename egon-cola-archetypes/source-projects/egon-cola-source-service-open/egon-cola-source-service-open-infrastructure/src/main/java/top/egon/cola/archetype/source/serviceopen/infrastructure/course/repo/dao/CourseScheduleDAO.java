package top.egon.cola.archetype.source.serviceopen.infrastructure.course.repo.dao;

import top.egon.cola.archetype.source.serviceopen.infrastructure.course.repo.po.CourseSchedulePO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import top.egon.cola.component.common.mybatis.extension.EgonColaMapper;

import java.time.Instant;
import java.util.List;

@Mapper
public interface CourseScheduleDAO extends EgonColaMapper<CourseSchedulePO> {
    List<CourseSchedulePO> selectOverlapping(
            @Param("courseId") Long courseId,
            @Param("classId") Long classId,
            @Param("startsAt") Instant startsAt,
            @Param("endsAt") Instant endsAt);
}
