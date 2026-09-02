package top.egon.cola.archetype.source.service.application.exam.command;

public record AttachExamPaperCommand(Long examId, String title, int totalPoints) {
}
