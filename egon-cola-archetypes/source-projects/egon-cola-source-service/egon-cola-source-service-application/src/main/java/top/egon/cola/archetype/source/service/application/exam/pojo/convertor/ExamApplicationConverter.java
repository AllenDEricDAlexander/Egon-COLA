package top.egon.cola.archetype.source.service.application.exam.pojo.convertor;

import org.mapstruct.AnnotateWith;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.springframework.stereotype.Component;
import top.egon.cola.archetype.source.service.application.exam.pojo.result.ExamDetailResult;
import top.egon.cola.archetype.source.service.application.exam.pojo.result.ExamPaperResult;
import top.egon.cola.archetype.source.service.application.exam.pojo.result.ScoreResult;
import top.egon.cola.archetype.source.service.domain.exam.entities.Exam;
import top.egon.cola.archetype.source.service.domain.exam.entities.ExamPaper;
import top.egon.cola.archetype.source.service.domain.exam.entities.Score;
import top.egon.cola.component.common.core.converter.BaseForwardConverter;

/** Exam, paper and score entity-to-result projection. */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
@AnnotateWith(value = Component.class, elements = @AnnotateWith.Element(strings = "examApplicationConverterImpl"))
public interface ExamApplicationConverter extends BaseForwardConverter<Exam, ExamDetailResult> {

    @Override
    @Mapping(target = "id", source = "id.value")
    @Mapping(target = "courseId", source = "courseId.value")
    @Mapping(target = "title", source = "title")
    @Mapping(target = "startsAt", source = "startsAt")
    @Mapping(target = "endsAt", source = "endsAt")
    @Mapping(target = "status", source = "status")
    ExamDetailResult toTarget(Exam source);

    /** Callable contract kept from the previous hand-written projection. */
    default ExamDetailResult toResult(Exam source) {
        return toTarget(source);
    }

    @Mapping(target = "id", source = "id")
    @Mapping(target = "examId", source = "examId.value")
    @Mapping(target = "title", source = "title")
    @Mapping(target = "totalPoints", source = "totalPoints")
    @Mapping(target = "status", source = "status")
    ExamPaperResult toResult(ExamPaper source);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "examId", source = "examId.value")
    @Mapping(target = "courseId", source = "courseId.value")
    @Mapping(target = "studentId", source = "studentId")
    @Mapping(target = "points", source = "points.value")
    @Mapping(target = "status", source = "status")
    ScoreResult toResult(Score source);
}
