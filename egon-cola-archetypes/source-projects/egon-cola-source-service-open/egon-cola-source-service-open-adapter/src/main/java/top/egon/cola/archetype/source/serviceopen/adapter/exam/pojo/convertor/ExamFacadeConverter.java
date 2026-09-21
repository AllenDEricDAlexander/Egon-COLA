package top.egon.cola.archetype.source.serviceopen.adapter.exam.pojo.convertor;

import com.google.protobuf.Timestamp;
import java.time.Instant;
import org.mapstruct.AnnotateWith;
import org.mapstruct.BeanMapping;
import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;
import org.springframework.stereotype.Component;
import top.egon.cola.archetype.source.serviceopen.application.exam.pojo.command.AttachExamPaperCommand;
import top.egon.cola.archetype.source.serviceopen.application.exam.pojo.command.CreateExamCommand;
import top.egon.cola.archetype.source.serviceopen.application.exam.pojo.command.PublishExamCommand;
import top.egon.cola.archetype.source.serviceopen.application.exam.pojo.query.GetExamQuery;
import top.egon.cola.archetype.source.serviceopen.application.exam.pojo.result.ExamDetailResult;
import top.egon.cola.archetype.source.serviceopen.application.exam.pojo.result.ExamPaperResult;
import top.egon.cola.archetype.source.serviceopen.facade.evaluation.v1.AttachExamPaperRequest;
import top.egon.cola.archetype.source.serviceopen.facade.evaluation.v1.CreateExamRequest;
import top.egon.cola.archetype.source.serviceopen.facade.evaluation.v1.Exam;
import top.egon.cola.archetype.source.serviceopen.facade.evaluation.v1.ExamPaper;
import top.egon.cola.archetype.source.serviceopen.facade.evaluation.v1.GetExamRequest;
import top.egon.cola.archetype.source.serviceopen.facade.evaluation.v1.PublishExamRequest;
import top.egon.cola.component.common.core.converter.BaseForwardConverter;

/** Maps the evaluation Exam wire onto the exam Commands, Queries and Results. */
@Mapper(collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED,
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
        componentModel = "spring")
@AnnotateWith(value = Component.class, elements = @AnnotateWith.Element(strings = "examFacadeConverterImpl"))
public interface ExamFacadeConverter extends BaseForwardConverter<ExamDetailResult, Exam> {

    @Override
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "courseId", source = "courseId")
    @Mapping(target = "title", source = "title")
    @Mapping(target = "startsAt", source = "startsAt")
    @Mapping(target = "endsAt", source = "endsAt")
    @Mapping(target = "status", source = "status")
    Exam toTarget(ExamDetailResult source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "examId", source = "examId")
    @Mapping(target = "title", source = "title")
    @Mapping(target = "totalPoints", source = "totalPoints")
    @Mapping(target = "status", source = "status")
    ExamPaper toPaper(ExamPaperResult source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "courseId", source = "courseId")
    @Mapping(target = "title", source = "title")
    @Mapping(target = "startsAt", source = "startsAt")
    @Mapping(target = "endsAt", source = "endsAt")
    CreateExamCommand toCommand(CreateExamRequest source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "examId", source = "examId")
    @Mapping(target = "title", source = "title")
    @Mapping(target = "totalPoints", source = "totalPoints")
    AttachExamPaperCommand toCommand(AttachExamPaperRequest source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "examId", source = "examId")
    PublishExamCommand toCommand(PublishExamRequest source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "examId", source = "examId")
    GetExamQuery toQuery(GetExamRequest source);

    default Timestamp toTimestamp(Instant value) {
        return value == null
                ? Timestamp.getDefaultInstance()
                : Timestamp.newBuilder().setSeconds(value.getEpochSecond()).setNanos(value.getNano()).build();
    }

    default Instant toInstant(Timestamp value) {
        return value == null ? null : Instant.ofEpochSecond(value.getSeconds(), value.getNanos());
    }
}
