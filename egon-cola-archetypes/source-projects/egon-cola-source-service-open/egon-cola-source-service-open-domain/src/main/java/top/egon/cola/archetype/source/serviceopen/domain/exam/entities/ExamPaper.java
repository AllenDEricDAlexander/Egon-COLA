package top.egon.cola.archetype.source.serviceopen.domain.exam.entities;

import top.egon.cola.archetype.source.serviceopen.domain.exam.enums.ExamPaperStatus;
import top.egon.cola.archetype.source.serviceopen.domain.exam.vos.ExamId;

public final class ExamPaper {

    private final Long id;
    private final ExamId examId;
    private final String title;
    private final int totalPoints;
    private ExamPaperStatus status;

    public ExamPaper(
            Long id,
            ExamId examId,
            String title,
            int totalPoints,
            ExamPaperStatus status) {
        this.id = id;
        this.examId = examId;
        this.title = title;
        this.totalPoints = totalPoints;
        this.status = status;
    }

    public void publish() { status = ExamPaperStatus.PUBLISHED; }
    public Long getId() { return id; }
    public ExamId getExamId() { return examId; }
    public String getTitle() { return title; }
    public int getTotalPoints() { return totalPoints; }
    public ExamPaperStatus getStatus() { return status; }
}
