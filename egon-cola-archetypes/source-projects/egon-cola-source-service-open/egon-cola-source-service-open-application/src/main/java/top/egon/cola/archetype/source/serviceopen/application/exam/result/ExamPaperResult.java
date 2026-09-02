package top.egon.cola.archetype.source.serviceopen.application.exam.result;

public record ExamPaperResult(
        Long id, Long examId, String title, int totalPoints, String status) {
}
