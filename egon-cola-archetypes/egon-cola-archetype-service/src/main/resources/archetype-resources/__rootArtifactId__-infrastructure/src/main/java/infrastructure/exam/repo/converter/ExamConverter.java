#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.exam.repo.converter;

import ${package}.domain.course.vos.CourseId;
import ${package}.domain.exam.entities.Exam;
import ${package}.domain.exam.enums.ExamStatus;
import ${package}.domain.exam.vos.ExamId;
import ${package}.infrastructure.exam.repo.po.ExamPO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import top.egon.cola.component.common.core.converter.BaseConverter;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ExamConverter extends BaseConverter<Exam, ExamPO> {

    @Override
    @Mapping(target = "courseId", expression = "java(source.getCourseId().value())")
    @Mapping(target = "status", expression = "java(source.getStatus().name())")
    ExamPO toTarget(Exam source);

    @Override
    default Exam toSource(ExamPO target) {
        return new Exam(
                new ExamId(target.getId()), new CourseId(target.getCourseId()), target.getTitle(),
                target.getStartsAt(), target.getEndsAt(), ExamStatus.valueOf(target.getStatus()));
    }
}
