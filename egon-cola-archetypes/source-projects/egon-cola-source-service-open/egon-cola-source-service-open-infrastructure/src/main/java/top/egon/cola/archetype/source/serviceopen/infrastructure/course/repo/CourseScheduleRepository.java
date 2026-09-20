package top.egon.cola.archetype.source.serviceopen.infrastructure.course.repo;

import top.egon.cola.archetype.source.serviceopen.infrastructure.course.repo.dao.CourseScheduleDAO;
import top.egon.cola.archetype.source.serviceopen.infrastructure.course.repo.po.CourseSchedulePO;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;
import top.egon.cola.component.common.mybatis.extension.EgonColaRepository;
import java.util.Collection;
import java.util.List;
import java.time.Instant;
import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Max;

/** Logical persistence boundary; query implementations remain explicit Mapper SQL. */
@Slf4j
@Validated
@Repository("courseScheduleRepository")
@RequiredArgsConstructor
public class CourseScheduleRepository extends EgonColaRepository<CourseScheduleDAO, CourseSchedulePO> {
    @Getter
    @Qualifier("courseScheduleDAO")
    private final CourseScheduleDAO baseMapper;
    @Getter(AccessLevel.PROTECTED)
    @Qualifier("egon.cola.component.mybatis-plus-top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties")
    private final EgonColaMybatisPlusProperties properties;

    public List<CourseSchedulePO> selectOverlapping(@Positive Long courseId, @Positive Long classId, @NotNull Instant startsAt, @NotNull Instant endsAt) {
        return getBaseMapper().selectOverlapping(courseId, classId, startsAt, endsAt);
    }
}
