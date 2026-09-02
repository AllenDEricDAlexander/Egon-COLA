package top.egon.cola.archetype.source.serviceopen.application.exam.command;

public record AttachExamPaperCommand(Long examId, String title, int totalPoints) {
}
