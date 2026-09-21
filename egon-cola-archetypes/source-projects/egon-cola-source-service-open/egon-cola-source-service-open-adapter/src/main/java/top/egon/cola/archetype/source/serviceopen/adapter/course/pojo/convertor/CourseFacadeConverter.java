package top.egon.cola.archetype.source.serviceopen.adapter.course.pojo.convertor;

import com.google.protobuf.Timestamp;
import java.time.Instant;
import java.util.Objects;
import org.mapstruct.AnnotateWith;
import org.mapstruct.AfterMapping;
import org.mapstruct.BeanMapping;
import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValueCheckStrategy;
import org.springframework.stereotype.Component;
import top.egon.cola.archetype.source.serviceopen.application.course.pojo.command.CreateCourseCommand;
import top.egon.cola.archetype.source.serviceopen.application.course.pojo.command.ScheduleCourseCommand;
import top.egon.cola.archetype.source.serviceopen.application.course.pojo.query.GetCourseQuery;
import top.egon.cola.archetype.source.serviceopen.application.course.pojo.query.PageCourseQuery;
import top.egon.cola.archetype.source.serviceopen.application.course.pojo.result.CourseResult;
import top.egon.cola.archetype.source.serviceopen.application.course.pojo.result.CourseScheduleResult;
import top.egon.cola.archetype.source.serviceopen.application.pojo.result.PageResult;
import top.egon.cola.archetype.source.serviceopen.facade.evaluation.v1.Course;
import top.egon.cola.archetype.source.serviceopen.facade.evaluation.v1.CourseSchedule;
import top.egon.cola.archetype.source.serviceopen.facade.evaluation.v1.CreateCourseRequest;
import top.egon.cola.archetype.source.serviceopen.facade.evaluation.v1.GetCourseRequest;
import top.egon.cola.archetype.source.serviceopen.facade.evaluation.v1.PageCourseResponse;
import top.egon.cola.archetype.source.serviceopen.facade.evaluation.v1.PageCoursesRequest;
import top.egon.cola.archetype.source.serviceopen.facade.evaluation.v1.ScheduleCourseRequest;
import top.egon.cola.component.common.core.converter.BaseForwardConverter;

/**
 * Maps the evaluation Course wire onto the course Commands, Queries and Results.
 *
 * <p>Protobuf messages carry no constraints, so the request direction always lands on an annotated
 * carrier and the adapter validator runs the native rules on that carrier instead.</p>
 */
@Mapper(collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED,
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
        componentModel = "spring")
@AnnotateWith(value = Component.class, elements = @AnnotateWith.Element(strings = "courseFacadeConverterImpl"))
public interface CourseFacadeConverter extends BaseForwardConverter<CourseResult, Course> {

    @Override
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "code", source = "code")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "credit", source = "credit")
    @Mapping(target = "status", source = "status")
    Course toTarget(CourseResult source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "courseId", source = "courseId")
    @Mapping(target = "classId", source = "classId")
    @Mapping(target = "startsAt", source = "startsAt")
    @Mapping(target = "endsAt", source = "endsAt")
    @Mapping(target = "status", source = "status")
    CourseSchedule toSchedule(CourseScheduleResult source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "code", source = "code")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "credit", source = "credit")
    CreateCourseCommand toCommand(CreateCourseRequest source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "courseId", source = "courseId")
    @Mapping(target = "classId", source = "classId")
    @Mapping(target = "startsAt", source = "startsAt")
    @Mapping(target = "endsAt", source = "endsAt")
    ScheduleCourseCommand toCommand(ScheduleCourseRequest source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "courseId", source = "courseId")
    GetCourseQuery toQuery(GetCourseRequest source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "currentPage", source = "currentPage")
    @Mapping(target = "pageSize", source = "pageSize")
    PageCourseQuery toQuery(PageCoursesRequest source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "currentPage", source = "currentPage")
    @Mapping(target = "totalPages", source = "totalPages")
    @Mapping(target = "pageSize", source = "pageSize")
    @Mapping(target = "totalCount", source = "totalCount")
    PageCourseResponse toPage(PageResult<CourseResult> source);

    /** Protobuf exposes immutable list getters; append through its builder API. */
    @AfterMapping
    default void appendRecords(
            PageResult<CourseResult> source, @MappingTarget PageCourseResponse.Builder target) {
        target.addAllRecords(Objects.requireNonNull(source.records(), "page records must not be null")
                .stream().map(this::toTarget).toList());
    }

    default Timestamp toTimestamp(Instant value) {
        return value == null
                ? Timestamp.getDefaultInstance()
                : Timestamp.newBuilder().setSeconds(value.getEpochSecond()).setNanos(value.getNano()).build();
    }

    default Instant toInstant(Timestamp value) {
        return value == null ? null : Instant.ofEpochSecond(value.getSeconds(), value.getNanos());
    }
}
