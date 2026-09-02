package top.egon.cola.archetype.source.service.adapter.exam.converter;

import top.egon.cola.archetype.source.service.application.exam.command.AttachExamPaperCommand;
import top.egon.cola.archetype.source.service.application.exam.command.CreateExamCommand;
import top.egon.cola.archetype.source.service.application.exam.command.PublishExamCommand;
import top.egon.cola.archetype.source.service.application.exam.result.ExamDetailResult;
import top.egon.cola.archetype.source.service.application.exam.result.ExamPaperResult;
import org.mapstruct.BeforeMapping;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import top.egon.cola.evaluation.facade.exam.dto.AttachExamPaperRequest;
import top.egon.cola.evaluation.facade.exam.dto.CreateExamRequest;
import top.egon.cola.evaluation.facade.exam.dto.ExamPaperResponse;
import top.egon.cola.evaluation.facade.exam.dto.ExamResponse;
import top.egon.cola.evaluation.facade.exam.dto.PublishExamRequest;

import java.util.Objects;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ExamFacadeConverter {

    CreateExamCommand toCommand(CreateExamRequest request);

    AttachExamPaperCommand toCommand(AttachExamPaperRequest request);

    PublishExamCommand toCommand(PublishExamRequest request);

    ExamResponse toResponse(ExamDetailResult result);

    ExamPaperResponse toResponse(ExamPaperResult result);

    @BeforeMapping
    default void requireCreateRequest(CreateExamRequest request) {
        Objects.requireNonNull(request, "request");
    }

    @BeforeMapping
    default void requireAttachRequest(AttachExamPaperRequest request) {
        Objects.requireNonNull(request, "request");
    }

    @BeforeMapping
    default void requirePublishRequest(PublishExamRequest request) {
        Objects.requireNonNull(request, "request");
    }

    @BeforeMapping
    default void requireExamResult(ExamDetailResult result) {
        Objects.requireNonNull(result, "result");
    }

    @BeforeMapping
    default void requirePaperResult(ExamPaperResult result) {
        Objects.requireNonNull(result, "result");
    }
}
