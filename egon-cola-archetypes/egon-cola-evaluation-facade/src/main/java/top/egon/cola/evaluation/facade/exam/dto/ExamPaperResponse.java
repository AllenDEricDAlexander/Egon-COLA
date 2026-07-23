package top.egon.cola.evaluation.facade.exam.dto;

import java.io.Serializable;

public record ExamPaperResponse(
        String id,
        String examId,
        String title,
        int totalPoints,
        String status) implements Serializable {
}
