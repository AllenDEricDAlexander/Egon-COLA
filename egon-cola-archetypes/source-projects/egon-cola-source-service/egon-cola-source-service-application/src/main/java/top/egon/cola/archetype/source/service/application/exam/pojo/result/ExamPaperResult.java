package top.egon.cola.archetype.source.service.application.exam.pojo.result;

public record ExamPaperResult(
        Long id, Long examId, String title, int totalPoints, String status) {
}
