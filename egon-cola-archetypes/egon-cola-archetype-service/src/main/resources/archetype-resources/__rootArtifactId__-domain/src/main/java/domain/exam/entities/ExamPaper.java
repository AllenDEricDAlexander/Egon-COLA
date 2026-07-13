#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.domain.exam.entities;

import ${package}.domain.exam.enums.ExamPaperStatus;
import ${package}.domain.exam.vos.ExamId;

public final class ExamPaper {

    private final String id;
    private final ExamId examId;
    private final String title;
    private final int totalPoints;
    private ExamPaperStatus status;

    public ExamPaper(
            String id,
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
    public String getId() { return id; }
    public ExamId getExamId() { return examId; }
    public String getTitle() { return title; }
    public int getTotalPoints() { return totalPoints; }
    public ExamPaperStatus getStatus() { return status; }
}
